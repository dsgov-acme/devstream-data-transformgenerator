package io.nuvalence.services;

import io.nuvalence.entities.Pipeline;
import io.nuvalence.entities.TransactionSchemaTransformation;
import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.valueitems.DataColumn;
import io.nuvalence.valueitems.DataTable;
import io.nuvalence.valueitems.Stages;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class DBTTransformToolGeneratorServiceTests {
    @Test
    void pipelineWithoutSubTablesMakesFiles() throws InvalidSchemaException, IOException {
        final var fileWriter = mock(FileWriter.class);
        final var typeTranslator = new BigQueryDataWarehouseTypeTranslator();
        final var logger = mock(Logger.class);

        final var underTest = new DBTTransformToolGeneratorService(Path.of("output"), typeTranslator, logger, fileWriter);
        final var pipeline = makeSimplePipeline();

        // act
        final var res = underTest.generateForSchemaTransformation(pipeline);

        assertNotNull(res);
        verify(fileWriter, times(4)).writeSQLFile(any(), any(), any());
        verify(fileWriter, times(1)).writeSchemaFile(any());
    }

    private static Pipeline makeSimplePipeline() throws InvalidSchemaException {
        final var typeFactory = new DataTypeFactory();

        final var tableMap = new HashMap<Stages, List<DataTable>>();

        final var rawTable = new DataTable("test_raw", Collections.singletonList(new DataColumn("val", typeFactory.getText())));
        tableMap.put(Stages.RAW, Collections.singletonList(rawTable));

        final var pkTable = new DataTable(
                "test_pk",
                List.of(
                        new DataColumn("val", typeFactory.getText()),
                        new DataColumn("id", typeFactory.getUniqueIdentifier(), true, true, false, false)
                )
        );
        tableMap.put(Stages.PRIMARY_KEY, Collections.singletonList(pkTable));

        final var normalizedTable = new DataTable("test_normalized", pkTable.getColumns());
        tableMap.put(Stages.NORMALIZED, Collections.singletonList(normalizedTable));

        final var typedTable = new DataTable("test_typed", normalizedTable.getColumns());
        tableMap.put(Stages.TYPED, Collections.singletonList(typedTable));

        final var publicTable = new DataTable("test_public", typedTable.getColumns());
        tableMap.put(Stages.PUBLIC, Collections.singletonList(publicTable));

        final var schema = new TransactionSchemaTransformation("test", tableMap);
        return new Pipeline(Collections.singletonList(schema), "testPipeline");
    }
}
