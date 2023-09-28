package io.nuvalence.services;

import io.nuvalence.factories.DataTypeFactory;
import io.nuvalence.valueitems.datatypes.DataType;
import org.jetbrains.annotations.NotNull;

public class BigQueryDataWarehouseTypeTranslator implements DataWarehouseDataTypeTranslator {
    @Override
    public String getWarehouseDataType(DataType dataType) {
        final var typeString = dataType.getType();
        if(typeString.equals(DataTypeFactory.LIST)){
            final var subType = getBQTypeFromString(dataType.getContentType());
            return "ARRAY<" + subType + ">";
        } else {
            return getBQTypeFromString(typeString);
        }
    }

    @NotNull
    private static String getBQTypeFromString(String typeString) {
        switch(typeString){
            case DataTypeFactory.INTEGER:
                return "INT64";
            case DataTypeFactory.BOOLEAN:
                return "BOOL";
            case DataTypeFactory.TEXT:
            case DataTypeFactory.RELATION:
            case DataTypeFactory.UNIQUEIDENTIFIER:
            case DataTypeFactory.DOCUMENT:
                return "STRING";
            case DataTypeFactory.NUMBER:
                return "NUMERIC";
            case DataTypeFactory.LOCALDATE:
                return "DATE";
            case DataTypeFactory.TIMESTAMP:
                return "TIMESTAMP";
            case DataTypeFactory.LOCALTIME:
                return "TIME";
            default:
                throw new IllegalArgumentException("Invalid type of " + typeString);
        }
    }
}
