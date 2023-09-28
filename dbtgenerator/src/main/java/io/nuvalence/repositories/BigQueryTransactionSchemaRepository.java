package io.nuvalence.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.bigquery.*;
import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.valueitems.Schema;
import io.nuvalence.valueitems.exceptions.SchemaRetrievalException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
/**
 * Retrieves stored transaction schemas that we've put into BigQuery
 */
public class BigQueryTransactionSchemaRepository implements TransactionSchemaRepository {
   private String projectId;
   private String datasetName;
   private String tableName;
   private Logger logger;

   private DataTypeFactory dataTypeFactory;

    @Override
    public Map<String, Schema> getSchemas() {
        final var query =
                "SELECT schema_json, id, key \n"
                        + " FROM `"
                        + projectId
                        + "."
                        + datasetName
                        + "."
                        + tableName
                        + "`";
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

            final var results = bigquery.query(queryConfig);

            final var tables = new ArrayList<io.nuvalence.valueitems.Schema>();
            for(final var row : results.iterateAll()){
                final var jsonValue = row.get(0).getValue().toString();
                final var schemaId = row.get(1).getValue().toString();
                final var table = io.nuvalence.valueitems.Schema.fromCustomSchema(schemaId, dataTypeFactory, jsonValue);
                tables.add(table);
            }
            logger.debug("Query performed successfully.");
            return tables.stream().collect(Collectors.toMap(Schema::getName, item->item));
        } catch (BigQueryException | InterruptedException | JsonProcessingException e) {
            logger.error("Query not performed", e);
            throw new SchemaRetrievalException("Unable to retrieve schema", e);
        }
    }
}
