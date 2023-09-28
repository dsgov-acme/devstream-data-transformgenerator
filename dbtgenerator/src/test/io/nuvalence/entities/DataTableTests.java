package io.nuvalence.entities;

import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.valueitems.DataColumn;
import io.nuvalence.valueitems.DataTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataTableTests {
    @Test
    void toJSONSchemaHandlesBasicTypes() throws Exception {
        final var dataTypeFactory = new DataTypeFactory();
        final var intColumn = new DataColumn("howMany", dataTypeFactory.fromString("INTEGER"));
        final var boolColumn = new DataColumn("havingFun", dataTypeFactory.fromString("BOOLEAN"));
        final var underTest = new DataTable("test", List.of(intColumn, boolColumn));

        final var res = underTest.toJSONSchema();

        assertNotNull(res);
        assertEquals("{\"type\":\"object\",\"properties\":[{\"howMany\":{\"type\":\"integer\"}},{\"havingFun\":{\"type\":\"boolean\"}}]}", res);
    }

    @Test
    void toJSONSchemaHandlesFormatTypes() throws Exception {
        final var dataTypeFactory = new DataTypeFactory();
        final var intColumn = new DataColumn("howMany", dataTypeFactory.fromString("INTEGER"));
        final var dateColumn = new DataColumn("when", dataTypeFactory.fromString("LOCALDATE"));
        final var underTest = new DataTable("test", List.of(intColumn, dateColumn));

        final var res = underTest.toJSONSchema();

        assertNotNull(res);
        assertTrue(res.contains("{\"when\":{\"format\":\"date\",\"type\":\"string\"}}"));
    }
}
