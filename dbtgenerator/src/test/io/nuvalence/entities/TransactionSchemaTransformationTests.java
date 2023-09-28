package io.nuvalence.entities;

import io.nuvalence.valueitems.DataTable;
import io.nuvalence.valueitems.Stages;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionSchemaTransformationTests {
    @Test
    void noPublicTablesThrows(){
        final var map = new HashMap<Stages, List<DataTable>>();
        final var startTable = mock(DataTable.class);
        map.put(Stages.RAW, Collections.singletonList(startTable));
        map.put(Stages.PUBLIC, Collections.emptyList());
        assertThrows(InvalidSchemaException.class, () -> new TransactionSchemaTransformation("test", map));
    }

    @Test
    void noRawTableThrows(){
        final var map = new HashMap<Stages, List<DataTable>>();
        map.put(Stages.RAW, Collections.emptyList());
        map.put(Stages.PUBLIC, Collections.singletonList(mock(DataTable.class)));
        assertThrows(InvalidSchemaException.class, () -> new TransactionSchemaTransformation("test", map));
    }

    @Test
    void multipleRawTablesThrows(){
        final var map = new HashMap<Stages, List<DataTable>>();
        map.put(Stages.RAW,
                List.of(
                        mock(DataTable.class),
                        mock(DataTable.class)
                ));
        map.put(Stages.PUBLIC, Collections.singletonList(mock(DataTable.class)));
        assertThrows(InvalidSchemaException.class, () -> new TransactionSchemaTransformation("test", map));
    }

    @Test
    void getInputTableReturnsRawTable() throws InvalidSchemaException {
        final var map = new HashMap<Stages, List<DataTable>>();
        final var rawTable = mock(DataTable.class);
        map.put(Stages.RAW,
                Collections.singletonList(rawTable));
        map.put(Stages.PUBLIC, Collections.singletonList(mock(DataTable.class)));

        final var underTest = new TransactionSchemaTransformation("test", map);

        // act
        final var res = underTest.getInputTable();

        assertEquals(rawTable, res);
    }

    @Test
    void getTableAtLaterStageCanReturnTyped() throws InvalidSchemaException {
        final var map = new HashMap<Stages, List<DataTable>>();
        final var rawTable = mock(DataTable.class);
        map.put(Stages.RAW,
                Collections.singletonList(rawTable));

        final var normalizedTable = mock(DataTable.class);
        when(normalizedTable.getName()).thenReturn("test_normalized");
        map.put(Stages.NORMALIZED, Collections.singletonList(normalizedTable));

        final var typedTable = mock(DataTable.class);
        when(typedTable.getName()).thenReturn("test_typed");
        map.put(Stages.TYPED, Collections.singletonList(typedTable));

        map.put(Stages.PUBLIC, Collections.singletonList(mock(DataTable.class)));

        final var underTest = new TransactionSchemaTransformation("test", map);

        // act
        final var res = underTest.getTableAtLaterStage(normalizedTable, Stages.TYPED);

        assertNotNull(res);
        assertEquals(typedTable, res);
    }
}
