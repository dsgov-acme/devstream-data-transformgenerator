package io.nuvalence.valueitems.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class JSONSchemaDataType {
    @NonNull
    private String basicType;
    private String format;
}
