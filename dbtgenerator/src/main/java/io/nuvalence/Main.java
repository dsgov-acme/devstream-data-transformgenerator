package io.nuvalence;

import io.nuvalence.entities.BaseSchemaTransformation;
import io.nuvalence.entities.Pipeline;
import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.factories.TransactionSchemaTransformationFactory;
import io.nuvalence.repositories.BigQueryTransactionSchemaRepository;
import io.nuvalence.services.BigQueryDataWarehouseTypeTranslator;
import io.nuvalence.services.DBTTransformToolGeneratorService;
import io.nuvalence.services.FileWriter;
import io.nuvalence.valueitems.RunConfiguration;
import io.nuvalence.valueitems.Schema;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        final var config = getConfiguration(args);

        final var dataTypeFactory = new DataTypeFactory();
        final var repository = new BigQueryTransactionSchemaRepository(config.getProjectId(), config.getDatasetName(), config.getSchemaTableName(), logger, dataTypeFactory);
        final var schemaFactory = new TransactionSchemaTransformationFactory(repository, dataTypeFactory);

        final var rawSchemas = repository.getSchemas();
        final var createdSchemas = rawSchemas.values().stream()
                .filter(s -> !s.getName().toLowerCase().startsWith("common"))
                .map(s -> makeSchemaTransformation(schemaFactory, s, rawSchemas))
                .collect(Collectors.toList());

        final var pipeline = new Pipeline(createdSchemas);
        generateModels(pipeline, config.getDatasetName(), config.getTransactionTableName());
    }

    private static BaseSchemaTransformation makeSchemaTransformation(TransactionSchemaTransformationFactory schemaFactory, Schema schema, Map<String, Schema> allSchemas){
        try{
            return schemaFactory.createForTransaction(schema, allSchemas);
        } catch(InvalidSchemaException e){
            logger.error("Invalid schema given ", e);
        }
        return null;
    }

    private static RunConfiguration getConfiguration(String[] args) {
        if(args.length > 3){
            return new RunConfiguration(
                    args[0],
                    args[1],
                    args[2],
                    args[3]
            );
        } else if (args.length > 1) {
            return new RunConfiguration(
                    args[0],
                    args[1],
                    "public_dynamic_schema",
                    "public_transaction"
            );
        }
        logger.info("Loading properties from environment");

        return new RunConfiguration(
                System.getenv("PROJECT_ID"),
                System.getenv("DATASET_NAME"),
                getEnvOr("SCHEMA_TABLE_NAME", "public_dynamic_schema"),
                getEnvOr("TRANSACTION_TABLE_NAME", "public_transaction")
        );
    }

    private static String getEnvOr(String key, String defaultVal){
        final var values = System.getenv();
        if(values.containsKey(key)){
            return values.get(key);
        }
        return defaultVal;
    }

    private static void generateModels(Pipeline pipeline, String datasetName, String transactionTableName) throws IOException {
        final var outputDirectory = Path.of("./output");
        final var fileWriter = new FileWriter(logger, outputDirectory);
        fileWriter.delete(outputDirectory);

        final var translator = new BigQueryDataWarehouseTypeTranslator();
        final var generator = new DBTTransformToolGeneratorService(outputDirectory, translator, logger, fileWriter, transactionTableName);

        try {
            generator.generateForSchemaTransformation(pipeline, datasetName);

            logger.info("Model files generated!");
        } catch (IOException | InvalidSchemaException e) {
            logger.error("Error while generating model files", e);
        }
    }
}