package io.nuvalence.valueitems.datatypes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class DataType {
    @NonNull
    private String type;
    /**
     * Optional related type of schema - only applies to LIST types
     */
    private String entitySchema;
    private String contentType;
    @NonNull
    private JSONSchemaDataType jsonSchemaDataType;

    public boolean isSimpleField(){
        return entitySchema == null && contentType == null;
    }

    public boolean isList(){
        return type.equalsIgnoreCase("LIST");
    }
}
