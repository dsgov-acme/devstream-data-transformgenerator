package io.nuvalence.services;

import io.nuvalence.entities.Pipeline;
import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.valueitems.*;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Getter
@AllArgsConstructor
public class DBTTransformToolGeneratorService implements TransformToolGeneratorService {
    private Path outputDirectory;
    private DataWarehouseDataTypeTranslator dataWarehouseDataTypeTranslator;
    private Logger logger;
    private FileWriter fileWriter;
    private String transactionTableName;

    @Override
    public List<Path> generateForSchemaTransformation(Pipeline pipeline, String datasetName) throws IOException, InvalidSchemaException {
        fileWriter.resetOutputDirectory();

        final var res = new ArrayList<Path>();

        final var modelEntries = new ArrayList<Map<String, Object>>();

        for(final var schemaTransformation: pipeline.getSchemaTransformations()){
            // add the pk table as a model and make model file
            final var pkTable = schemaTransformation.getTablesPerStage().get(Stages.PRIMARY_KEY).get(0);
            final var pkModelFile = makePKModelFile(pkTable, schemaTransformation.getSchema().getSchemaId(), datasetName);
            res.add(pkModelFile);
            modelEntries.add(makeSchemaEntryForModel(pkTable));

            // normalized tables are where we can have more than one, handle each branch in a loop
            final var normalizedTables = schemaTransformation.getTablesPerStage().get(Stages.NORMALIZED);
            for(final var normalizedTable: normalizedTables){
                if(normalizedTable instanceof NormalizedSubDataTable) {
                    res.add(makeNormalizedSubModelFile(normalizedTable, pkTable, ((NormalizedSubDataTable) normalizedTable).getSourceColumnName()));
                } else {
                    res.add(makeNormalizedMainModelFile(normalizedTable, pkTable));
                }
                modelEntries.add(makeSchemaEntryForModel(normalizedTable));

                // get the matching typed table
                final var typedTable = schemaTransformation.getTableAtLaterStage(normalizedTable, Stages.TYPED);
                res.add(makeTypedModelFile(typedTable, normalizedTable));
                modelEntries.add(makeSchemaEntryForModel(typedTable));

                // get the public table
                final var publicTable = schemaTransformation.getTableAtLaterStage(normalizedTable, Stages.PUBLIC);
                res.add(makePublicModelFile(publicTable, typedTable));
                modelEntries.add(makeSchemaEntryForModel(publicTable));
            }
        }

        final var schemaFileDictionary = new HashMap<String, Object>();
        schemaFileDictionary.put("version", 2);

        final var source = new HashMap<String, Object>();
        final var sourceTable = new HashMap<String, String>();
        sourceTable.put("name", this.transactionTableName);

        final var freshTest = new HashMap<String, Object>();
        sourceTable.put("loaded_at_field", "last_updated_timestamp");

        final var warnAfter = new HashMap<String, Object>();
        warnAfter.put("count", 1);
        warnAfter.put("period", "day");
        freshTest.put("warn_after", warnAfter);

        final var errorAfter = new HashMap<String, Object>();
        errorAfter.put("count", 3);
        errorAfter.put("period", "day");
        freshTest.put("error_after", errorAfter);

        source.put("tables", List.of(sourceTable));
        source.put("name", datasetName);
        source.put("freshness", freshTest);
        schemaFileDictionary.put("sources", List.of(source));

        schemaFileDictionary.put("models", modelEntries);

        // make the schema file
        final var schemaFile = fileWriter.writeSchemaFile(schemaFileDictionary);
        res.add(schemaFile);

        // TODO replace the dataset with the one we were given!
        final var newProjectFile = fileWriter.copyDbtFile("dbt_project.yml");

        final var newProfilesFile = fileWriter.copyDbtFile("profiles.yml");

        logger.info(String.format("%d dbt files generated!", res.size()));
        return res;
    }

    private Map<String, Object> makeSchemaEntryForModel(DataTable table){
        final var model = new HashMap<String, Object>();
        model.put("name", table.getBQName());

        final var columns = new ArrayList<HashMap<String, Object>>();
        for(final var dataCol: table.getColumns()){
            final var column = new HashMap<String, Object>();
            column.put("name", dataCol.getName());
            if(dataCol.isUnique() || !dataCol.isNullable()){
                final var tests = new ArrayList<String>();
                if(dataCol.isUnique()){
                    tests.add("unique");
                }
                if(!dataCol.isNullable()){
                    tests.add("not_null");
                }
                column.put("tests", tests);
            }
            columns.add(column);
        }
        model.put("columns", columns);
        return model;
    }

    private Path makePKModelFile(DataTable pkTable, String schemaId, String datasetName) throws IOException {
        //PK will always pull from transactions, but will split up the results based upon the dynamic schema!
        final var sql = new SQLLines();
        for(final var col: pkTable.getColumns()){
            if(col.isAutoGenerate() && Objects.equals(col.getType().getType(), DataTypeFactory.UNIQUEIDENTIFIER)){
                sql.addColumn("GENERATE_UUID() AS " + col.getName());
            } else {
                sql.addColumn(col.getName());
            }
        }

        sql.setFrom("FROM {{source('" + datasetName + "', '"+this.transactionTableName+"')}}");
        sql.addWhere("WHERE dynamic_schema_id = '" + schemaId + "'");
        return fileWriter.writeSQLFile(pkTable, sql, "pk");
    }

    private Path makeNormalizedMainModelFile(DataTable table, DataTable prevTable) throws IOException, InvalidSchemaException {
        final var sql = new SQLLines();
        sql.setSelect("SELECT");

        final var sourceColumn = prevTable.getColumns().stream()
                .filter(dc -> !dc.isAutoGenerate())
                .findFirst()
                .orElseThrow(() -> new InvalidSchemaException("Cannot locate value column in raw table!"));

        final var sourceColumnName = sourceColumn.getName();

        for(final var col: table.getColumns()){
            if (col.isAutoGenerate() && Objects.equals(col.getType().getType(), DataTypeFactory.UNIQUEIDENTIFIER)) {
                sql.addColumn(col.getBQName());
            } else {
                final var jsonStatement = "JSON_VALUE("+ sourceColumnName + "." + col.getName() + ") AS " + col.getName();
                sql.addColumn(jsonStatement);
            }
        }
        sql.setFrom("FROM {{ref('" + prevTable.getBQName() + "')}}");

        return fileWriter.writeSQLFile(table, sql, "normalized");
    }

    private Path makeNormalizedSubModelFile(DataTable table, DataTable prevTable, String fieldName) throws IOException, InvalidSchemaException {
        final var sql = new SQLLines();
        final var baseSql = new SQLLines();

        final var jsonColumn = prevTable.getColumns().stream()
                .filter(dc -> !dc.isAutoGenerate())
                .findFirst()
                .orElseThrow(() -> new InvalidSchemaException("Cannot locate data column in raw table!"));

        final var sourceColumnName = jsonColumn.getBQName();

        DataColumn idColumn = null;
        DataColumn childIdColumn = null;
        List<DataColumn> outputCols = new ArrayList<>();

        for(final var col: table.getColumns()){
            if(col.isForeignKey() && col.getType().getType().equals(DataTypeFactory.UNIQUEIDENTIFIER)){
                // this is a generated foreign key, we should pull the ID from the table as this
                idColumn = col;
            } else {
                if (col.isAutoGenerate() && Objects.equals(col.getType().getType(), DataTypeFactory.UNIQUEIDENTIFIER)) {
                    childIdColumn = col;
                } else {
                    outputCols.add(col);
                }
            }
        }

        baseSql.setSelect("WITH base AS (SELECT");
        baseSql.addColumn("JSON_QUERY("+jsonColumn.getBQName()+", '$."+fieldName+"') AS "+fieldName);
        baseSql.addColumn("id");
        baseSql.setFrom("FROM {{ref('" + prevTable.getBQName() + "')}}");

        final var withStatement = String.join("\n", baseSql.getOutput());
        sql.setSelect(withStatement + ") SELECT");
        sql.addColumn("id as "+ idColumn.getBQName());
        sql.addColumn("GENERATE_UUID() AS " + childIdColumn.getBQName());
        for(final var outputCol : outputCols){
            sql.addColumn("TO_JSON_STRING(JSON_QUERY("+fieldName+", '$."+outputCol.getBQName()+"')) as " + outputCol.getBQName());
        }
        sql.setFrom("FROM base WHERE " +fieldName+ " IS NOT NULL");

        return fileWriter.writeSQLFile(table, sql, "normalized");
    }

    private Path makeTypedModelFile(DataTable table, DataTable prevTable) throws IOException {
        final var sql = new SQLLines();
        sql.setSelect("SELECT");

        for(final var col: table.getColumns()){
            sql.addColumn("SAFE_CAST (" + col.getBQName() + " AS " + dataWarehouseDataTypeTranslator.getWarehouseDataType(col.getType()) + ") as " + col.getBQName());
        }
        sql.setFrom("FROM {{ref('" + prevTable.getBQName() + "')}}");

        return fileWriter.writeSQLFile(table, sql, "typed");
    }

    private Path makePublicModelFile(DataTable table, DataTable prevTable) throws IOException {
        final var sql = new SQLLines();
        sql.setSelect("SELECT");

        for(final var col: table.getColumns()){
            sql.addColumn(col.getBQName());
        }
        sql.setFrom("FROM {{ref('" + prevTable.getBQName() + "')}}");

        return fileWriter.writeSQLFile(table, sql, "public");
    }
}
