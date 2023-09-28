package io.nuvalence.services;

import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.valueitems.datatypes.DataType;
import io.nuvalence.valueitems.datatypes.JSONSchemaDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BigQueryDataWarehouseTypeTranslatorTests {
    @Test
    void intIsInt64(){
        final var typeFactory = new DataTypeFactory();
        final var underTest = new BigQueryDataWarehouseTypeTranslator();
        final var res = underTest.getWarehouseDataType(typeFactory.fromString(DataTypeFactory.INTEGER));

        assertEquals("INT64", res);
    }

    @Test
    void boolIsBool(){
        final var typeFactory = new DataTypeFactory();
        final var underTest = new BigQueryDataWarehouseTypeTranslator();
        final var res = underTest.getWarehouseDataType(typeFactory.fromString(DataTypeFactory.BOOLEAN));

        assertEquals("BOOL", res);
    }

    @Test
    void uniqueIdentifierIsString(){
        final var typeFactory = new DataTypeFactory();
        final var underTest = new BigQueryDataWarehouseTypeTranslator();
        final var res = underTest.getWarehouseDataType(typeFactory.getUniqueIdentifier());

        assertEquals("STRING", res);
    }

    @Test
    void relationIsString(){
        final var typeFactory = new DataTypeFactory();
        final var underTest = new BigQueryDataWarehouseTypeTranslator();
        final var res = underTest.getWarehouseDataType(typeFactory.fromString(DataTypeFactory.RELATION, null, "SOMETHING"));

        assertEquals("STRING", res);
    }

    @Test
    void listOfStringIsArrayOfString(){
        final var typeFactory = new DataTypeFactory();
        final var underTest = new BigQueryDataWarehouseTypeTranslator();
        final var res = underTest.getWarehouseDataType(typeFactory.fromString(DataTypeFactory.LIST, DataTypeFactory.TEXT, null));

        assertEquals("ARRAY<STRING>", res);
    }

    @Test
    void unknownTypeThrows(){
        final var underTest = new BigQueryDataWarehouseTypeTranslator();
        final var weirdType = new DataType("FUNKY", null, null, new JSONSchemaDataType("object", null));

        assertThrows(IllegalArgumentException.class, () -> underTest.getWarehouseDataType(weirdType));
    }
}
