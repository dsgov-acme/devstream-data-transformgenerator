package io.nuvalence.factories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class DataTypeFactoryTests {
    private final DataTypeFactory dataTypeFactory = new DataTypeFactory();
    
    @ParameterizedTest
    @ValueSource(strings = {"TEXT", "STRING", "VARCHAR"})
    void textHasNoFormatInJsonSchema(String input) {
        final var res = dataTypeFactory.fromString(input);

        assertEquals(res.getType(), "TEXT");
        assertNotNull(res.getJsonSchemaDataType());
        assertNull(res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INTEGER", "INT", "INT64"})
    void integerAliasesShouldAllReturnInteger(String input) {
        final var res = dataTypeFactory.fromString(input);

        assertNotNull(res);
        assertEquals("INTEGER", res.getType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("integer", res.getJsonSchemaDataType().getBasicType());
        assertNull(res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BOOL", "BOOLEAN"})
    void booleanHasNullFormat(String input){
        final var res = dataTypeFactory.fromString(input);

        assertEquals("BOOLEAN", res.getType());
        assertNull(res.getContentType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("boolean", res.getJsonSchemaDataType().getBasicType());
        assertNull(res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NUMBER", "FLOAT", "DECIMAL", "BIGDECIMAL"})
    void numberHasNullFormat(String input){
        final var res = dataTypeFactory.fromString(input);

        assertEquals("NUMBER", res.getType());
        assertNull(res.getContentType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("number", res.getJsonSchemaDataType().getBasicType());
        assertNull(res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UUID", "UNIQUEIDENTIFIER", "GUID"})
    void uniqueIdentifierHasFormat(String input){
        final var res = dataTypeFactory.fromString(input);

        assertEquals("UNIQUEIDENTIFIER", res.getType());
        assertNull(res.getContentType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("string", res.getJsonSchemaDataType().getBasicType());
        assertEquals("uuid", res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TIMESTAMP", "DATE-TIME", "DATETIME"})
    void timestampHasFormat(String input){
        final var res = dataTypeFactory.fromString(input);

        assertEquals("TIMESTAMP", res.getType());
        assertNull(res.getContentType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("string", res.getJsonSchemaDataType().getBasicType());
        assertEquals("date-time", res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DATE", "LOCALDATE"})
    void localdateHasFormat(String input){
        final var res = dataTypeFactory.fromString(input);

        assertEquals("LOCALDATE", res.getType());
        assertNull(res.getContentType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("string", res.getJsonSchemaDataType().getBasicType());
        assertEquals("date", res.getJsonSchemaDataType().getFormat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TIME", "LOCALTIME"})
    void localtimeHasFormat(String input){
        final var res = dataTypeFactory.fromString(input);

        assertEquals("LOCALTIME", res.getType());
        assertNull(res.getContentType());
        assertNotNull(res.getJsonSchemaDataType());
        assertEquals("string", res.getJsonSchemaDataType().getBasicType());
        assertEquals("time", res.getJsonSchemaDataType().getFormat());
    }

    @Test
    void listWithoutEntitySchemaThrows() {
        assertThrows(IllegalArgumentException.class, () -> dataTypeFactory.fromString("LIST", "ENTITY", ""));
    }

    @Test
    void listWithEntitySchemaIsArray(){
        final var res = dataTypeFactory.fromString("LIST", "ENTITY", "STUFF");

        assertEquals("array", res.getJsonSchemaDataType().getBasicType());
    }

    @Test
    void unknownTypeThrows(){
        assertThrows(IllegalArgumentException.class, () -> dataTypeFactory.fromString("NOTTATHING"));
    }
}
