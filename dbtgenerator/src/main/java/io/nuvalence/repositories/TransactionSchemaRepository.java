package io.nuvalence.repositories;

import io.nuvalence.valueitems.Schema;
import io.nuvalence.valueitems.exceptions.SchemaRetrievalException;

import java.util.Map;

public interface TransactionSchemaRepository {
    Map<String, Schema> getSchemas() throws SchemaRetrievalException;
}
