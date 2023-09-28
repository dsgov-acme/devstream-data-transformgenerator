package io.nuvalence.valueitems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.factories.DataTypeFactory;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Schema extends DataTable {
    private String schemaId;

    public Schema(String schemaId, String name, List<DataColumn> columns) {
        super(name, columns);
        this.schemaId = schemaId;
    }

    public static Schema fromCustomSchema(String schemaId, DataTypeFactory dataTypeFactory, String json) throws JsonProcessingException {
        final var val = new ObjectMapper().readValue(json, Map.class);
        final var attributes = (List<Map<String, Object>>)val.get("attributes");

        final var columns = attributes.stream()
                .map(att -> DataTable.makeColumnFromAttribute(dataTypeFactory, att))
                .collect(Collectors.toList());

        // note with how we store schemas this needs to be key, not name!
        return new Schema(schemaId, val.get("key").toString(), columns);
    }
}
