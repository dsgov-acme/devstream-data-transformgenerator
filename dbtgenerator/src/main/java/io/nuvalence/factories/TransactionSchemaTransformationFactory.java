package io.nuvalence.factories;

import io.nuvalence.valueitems.*;
import io.nuvalence.entities.TransactionSchemaTransformation;
import io.nuvalence.repositories.TransactionSchemaRepository;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class TransactionSchemaTransformationFactory {
    public static final String ID_COLUMN_NAME = "id";
    public static final String RAW_POSTFIX = "_raw";
    public static final String PK_POSTFIX = "_pk";
    public static final String UNNEST_POSTFIX = "_unnested";
    public static final String NORMALIZED_POSTFIX = "_normalized";
    public static final String TYPED_POSTFIX = "_typed";
    public static final String PUBLIC_POSTFIX = "_public";

    private TransactionSchemaRepository transactionSchemaRepository;
    private DataTypeFactory dataTypeFactory;

    public TransactionSchemaTransformation createForTransaction(Schema transactionSchema, Map<String, Schema> allSchemas) throws InvalidSchemaException {
        return createForTransaction(transactionSchema, "data", allSchemas);
    }

    public TransactionSchemaTransformation createForTransaction(Schema transactionSchema, String jsonColumnName, Map<String, Schema> allSchemas) throws InvalidSchemaException {
        final var tables = new EnumMap<Stages, List<DataTable>>(Stages.class);

        final var transactionName = transactionSchema.getName().contains(RAW_POSTFIX) ? transactionSchema.getName().replace(RAW_POSTFIX, "") : transactionSchema.getName();
        final var rawName = transactionName + RAW_POSTFIX;

        // raw table should just be the JSON column
        final var rawTable = new DataTable(
                rawName,
                List.of(
                    new DataColumn(
                            jsonColumnName,
                            dataTypeFactory.getText(),
                            false,
                            false,
                            true,
                            false
                    )
                )
        );
        tables.put(Stages.RAW, List.of(rawTable));

        final var idCols = new ArrayList<>(rawTable.getColumns());
        final var pkCol = new DataColumn(ID_COLUMN_NAME, dataTypeFactory.getUniqueIdentifier(), true, true, false, false);
        idCols.add(pkCol);
        final var idTable = new DataTable(transactionName + PK_POSTFIX, idCols);
        tables.put(Stages.PRIMARY_KEY, List.of(idTable));

        final ArrayList<DataTable> normalizedTables = makeNormalizedTables(transactionSchema, transactionName, pkCol, allSchemas);
        tables.put(Stages.NORMALIZED, normalizedTables);

        // for now, typed is just a copy of normalized!
        final var typedTables = normalizedTables.stream()
                .map(t -> new DataTable(t.getName().replace(NORMALIZED_POSTFIX, TYPED_POSTFIX), t.getColumns()))
                .collect(Collectors.toList());
        tables.put(Stages.TYPED, typedTables);

        // public is a copy of typed
        final var publicTables = typedTables.stream()
                .map(t -> new DataTable(t.getName().replace( TYPED_POSTFIX, PUBLIC_POSTFIX), t.getColumns()))
                .collect(Collectors.toList());
        tables.put(Stages.PUBLIC, publicTables);

        return new TransactionSchemaTransformation(transactionName, tables, transactionSchema);
    }

    @NotNull
    private ArrayList<DataTable> makeNormalizedTables(Schema transactionSchema, String transactionName, DataColumn pkCol, Map<String, Schema> otherSchemas) {
        final var normalizedTables = new ArrayList<DataTable>();
        final var normalizedColumns = new ArrayList<>(transactionSchema.getColumns());
        normalizedColumns.add(pkCol);
        final var mainTableColumns = normalizedColumns.stream()
                .filter(dc -> dc.getType().isSimpleField())
                .collect(Collectors.toList());
        final var mainTable = new DataTable(transactionName + NORMALIZED_POSTFIX, mainTableColumns);
        normalizedTables.add(mainTable);

        // find any relation tables that need to be normalized
        // TODO we need to get subs of subs here
        final var relationSchemas = normalizedColumns.stream()
                .filter(dc -> dc.getType().getEntitySchema() != null)
                .map(dc -> new ColumnAndSchema(dc, otherSchemas.get(dc.getType().getEntitySchema())))
                .collect(Collectors.toList());

        final var subTableFKId = new DataColumn(transactionName + "_" + ID_COLUMN_NAME, dataTypeFactory.getUniqueIdentifier(), false, false, true, true);
        for(final var dcAndSchema: relationSchemas){
            final var allSubTableColumns = new ArrayList<>(dcAndSchema.getDataTable().getColumns());
            allSubTableColumns.add(subTableFKId);
            allSubTableColumns.add(pkCol);
            final var name = makeSubTableName(transactionName, dcAndSchema.getDataColumn());
            final var subTable = new NormalizedSubDataTable(name, allSubTableColumns, dcAndSchema.dataColumn.getName());
            normalizedTables.add(subTable);
        }

        // what to do with lists of simple types?
        final var simpleSubColumns = normalizedColumns.stream()
                .filter(dc -> dc.getType().isList() && dc.getType().getEntitySchema() == null)
                .collect(Collectors.toList());
        for(final var simpleSubColumn : simpleSubColumns){
            final var subDataType = dataTypeFactory.fromString(simpleSubColumn.getType().getContentType());
            final var subCols = List.of(
                    pkCol,
                    subTableFKId,
                    new DataColumn(simpleSubColumn.getName(), subDataType)
            );
            final var subTableName = makeSubTableName(transactionName, simpleSubColumn);
            final var simpleSubTable = new NormalizedSubDataTable(subTableName, subCols, simpleSubColumn.getName());
            normalizedTables.add(simpleSubTable);
        }
        return normalizedTables;
    }

    private String makeSubTableName(String parentName, DataColumn col){
        return parentName + "_" + col.getName().toLowerCase() + "_" + Stages.NORMALIZED.name().toLowerCase();
    }

    @Getter
    @AllArgsConstructor
    private static class ColumnAndSchema{
        private DataColumn dataColumn;
        private DataTable dataTable;
    }
}
