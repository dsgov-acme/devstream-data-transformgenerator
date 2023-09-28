package io.nuvalence.factories;

import io.nuvalence.valueitems.datatypes.DataType;
import io.nuvalence.valueitems.datatypes.JSONSchemaDataType;
import org.apache.commons.lang3.StringUtils;


public class DataTypeFactory {
    public DataType fromString(String string) {
        return fromString(string, null, null);
    }

    public DataType getText(){
        return fromString(TEXT);
    }

    public DataType getUniqueIdentifier(){
        return fromString(UNIQUEIDENTIFIER);
    }

    public static final String TEXT = "TEXT";
    public static final String INTEGER = "INTEGER";
    public static final String BOOLEAN = "BOOLEAN";
    public static final String NUMBER = "NUMBER";
    public static final String UNIQUEIDENTIFIER = "UNIQUEIDENTIFIER";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String LOCALDATE = "LOCALDATE";
    public static final String LOCALTIME = "LOCALTIME";
    public static final String DOCUMENT = "DOCUMENT";
    public static final String RELATION = "RELATION";
    public static final String LIST = "LIST";

    private static final String JSON_STRING = "string";
    public DataType fromString(String string, String contentType, String entitySchema) {
        switch(string.toUpperCase()){
            case TEXT:
            case "STRING":
            case "VARCHAR":
                return new DataType(TEXT, null,
                        null,
                        new JSONSchemaDataType(JSON_STRING, null));
            case INTEGER:
            case "INT":
            case "INT64":
                return new DataType(INTEGER, null, null,
                        new JSONSchemaDataType("integer", null));
            case BOOLEAN:
            case "BOOL":
                return new DataType(BOOLEAN, null, null,
                        new JSONSchemaDataType("boolean", null));
            case NUMBER:
            case "FLOAT":
            case "DECIMAL":
            case "BIGDECIMAL":
            case "NUMERIC":
                return new DataType(NUMBER, null, null,
                        new JSONSchemaDataType("number", null));
            case "GUID":
            case "UUID":
            case UNIQUEIDENTIFIER:
                return new DataType(UNIQUEIDENTIFIER, null, null,
                        new JSONSchemaDataType(JSON_STRING, "uuid"));
            case TIMESTAMP:
            case "DATE-TIME":
            case "DATETIME":
                return new DataType(TIMESTAMP, null, null,
                        new JSONSchemaDataType(JSON_STRING, "date-time"));
            case "DATE":
            case LOCALDATE:
                return new DataType(LOCALDATE, null, null,
                        new JSONSchemaDataType(JSON_STRING, "date"));
            case "TIME":
            case LOCALTIME:
                return new DataType(LOCALTIME, null, null,
                        new JSONSchemaDataType(JSON_STRING, "time"));
            case DOCUMENT:
                return new DataType(DOCUMENT, null, null,
                        new JSONSchemaDataType(JSON_STRING, "uri"));
            case RELATION:
            case "DYNAMICENTITY":
                if(StringUtils.isBlank(entitySchema)){
                    throw new IllegalArgumentException();
                }
                return new DataType(RELATION, entitySchema, null,
                        new JSONSchemaDataType("object", null));
            case "ARRAY":
            case LIST:
                if(contentType.equalsIgnoreCase("ENTITY") && (StringUtils.isBlank(entitySchema))){
                        throw new IllegalArgumentException();
                }
                return new DataType(LIST, entitySchema, contentType,
                        new JSONSchemaDataType("array", null));
            default:
                throw new IllegalArgumentException();
        }
    }
}
