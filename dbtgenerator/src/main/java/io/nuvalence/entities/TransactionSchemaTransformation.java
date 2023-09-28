package io.nuvalence.entities;

import io.nuvalence.valueitems.DataTable;
import io.nuvalence.valueitems.Schema;
import io.nuvalence.valueitems.Stages;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class TransactionSchemaTransformation extends BaseSchemaTransformation {
    private final String transactionName;

    public TransactionSchemaTransformation(String transactionName, Map<Stages, List<DataTable>> tablesPerStage, Schema schema) throws InvalidSchemaException {
        super(tablesPerStage, schema);
        this.transactionName = transactionName;
    }
}
