package io.nuvalence.factories;

import io.nuvalence.valueitems.DataTable;
import io.nuvalence.repositories.TransactionSchemaRepository;
import io.nuvalence.valueitems.DataColumn;
import io.nuvalence.valueitems.Stages;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionSchemaTransformationFactoryTests {
    @Mock
    private TransactionSchemaRepository transactionSchemaRepository;

    private final DataTypeFactory dataTypeFactory = new DataTypeFactory();

    @Test
    void testCreateForTransactionWithJustSimpleFieldsGivesOneTableOutput() throws InvalidSchemaException {
        transactionSchemaRepository = mock();
        final var rawSchema = new DataTable(
                "test",
                List.of(
                        new DataColumn("name", dataTypeFactory.fromString("TEXT")),
                        new DataColumn("numFriends", dataTypeFactory.fromString("INTEGER"))
                )
        );

        final var underTest = new TransactionSchemaTransformationFactory(transactionSchemaRepository, dataTypeFactory);

        final var res = underTest.createForTransaction(rawSchema);

        assertNotNull(res);
        for(final var stage: Stages.values()){
            assertEquals(1, res.getTablesPerStage().get(stage).size());
        }
        verify(transactionSchemaRepository, never()).getSchemaByName(anyString());
    }

    @Test
    void testCreateForTransactionWithEntityListMakesSubTables() throws InvalidSchemaException {
        transactionSchemaRepository = mock();
        final var rawSchema = new DataTable(
                "test",
                List.of(
                        new DataColumn("name", dataTypeFactory.getText()),
                        new DataColumn("bestFriends", dataTypeFactory.fromString("LIST", "ENTITY", "person")),
                        new DataColumn("favoriteFruits", dataTypeFactory.fromString("LIST","STRING", null))
                )
        );

        final var personSchema = new DataTable(
                "person",
                List.of(
                        new DataColumn("name", dataTypeFactory.fromString("TEXT"))
                )
        );
        when(transactionSchemaRepository.getSchemaByName("person")).thenReturn(personSchema);

        final var underTest = new TransactionSchemaTransformationFactory(transactionSchemaRepository, dataTypeFactory);

        final var res = underTest.createForTransaction(rawSchema);

        assertNotNull(res);
        for(final var stage: Stages.values()){
            assertFalse(res.getTablesPerStage().get(stage).isEmpty());
        }

        final var publicTables = res.getTablesPerStage().get(Stages.PUBLIC);
        assertEquals(3, publicTables.size());
        final var names = publicTables.stream().map(DataTable::getName).collect(Collectors.toList());
        assertTrue(names.contains("test_public"));
        assertTrue(names.contains("test_bestfriends_public"));
        assertTrue(names.contains("test_favoritefruits_public"));
    }
}
