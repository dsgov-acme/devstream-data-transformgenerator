package io.nuvalence.entities;

import io.nuvalence.factories.TransactionSchemaTransformationFactory;
import io.nuvalence.valueitems.DataTable;
import io.nuvalence.valueitems.Schema;
import io.nuvalence.valueitems.Stages;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Represents an input table and all its output tables
 */
@Getter
public abstract class BaseSchemaTransformation {
    private final Schema schema;

    protected BaseSchemaTransformation(Map<Stages, List<DataTable>> tablesPerStage, Schema schema) throws InvalidSchemaException {
        if(tablesPerStage.get(Stages.RAW).size() != 1){
            throw new InvalidSchemaException("Must be only one raw table!");
        }

        if(tablesPerStage.get(Stages.PUBLIC).isEmpty()){
            throw new InvalidSchemaException("No public tables were specified!");
        }

        this.tablesPerStage = tablesPerStage;
        this.schema = schema;
    }

    private final Map<Stages, List<DataTable>> tablesPerStage;

    public DataTable getInputTable(){
        return tablesPerStage.get(Stages.RAW).get(0);
    }

    public DataTable getTableAtLaterStage(DataTable normalizedTable, Stages stage){
        final var postfix = stage == Stages.TYPED? TransactionSchemaTransformationFactory.TYPED_POSTFIX : TransactionSchemaTransformationFactory.PUBLIC_POSTFIX;
        final var table = getTablesPerStage().get(stage).stream()
                .filter(t -> t.getName().replace(postfix, "").equals(normalizedTable.getName().replace("_normalized", "")))
                .findFirst();
        return table.orElseThrow(() -> new IllegalArgumentException("Table has no match at stage " + stage.name()));
    }
}
