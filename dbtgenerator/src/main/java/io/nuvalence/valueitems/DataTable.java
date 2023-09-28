package io.nuvalence.valueitems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.factories.DataTypeFactory;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class DataTable {
    private String name;

    private List<DataColumn> columns;

    public String getBQName(){
        return this.name.toLowerCase().replace(" ", "_");
    }

    public String toJSONSchema() throws JsonProcessingException {
        final var attributes = columns.stream().map(DataTable::columnToJSON).collect(Collectors.toList());

        final var schema = new HashMap<String, Object>();
        schema.put("properties", attributes);
        schema.put("type", "object");

        return new ObjectMapper().writeValueAsString(schema);
    }

    protected static DataColumn makeColumnFromAttribute(DataTypeFactory dataTypeFactory, Map<String, Object> attribute) {
        final var entitySchema = attribute.containsKey("entitySchema") ? attribute.get("entitySchema").toString() : null;
        final var contentType = attribute.containsKey("contentType")? attribute.get("contentType").toString() : null;
        return new DataColumn(
                attribute.get("name").toString(),
                dataTypeFactory.fromString(attribute.get("type").toString(), contentType, entitySchema)
                );
    }

    private static Map<String, Map<String, String>> columnToJSON(DataColumn column){
        final var res = new HashMap<String, Map<String, String>>();

        final var typeStuff = new HashMap<String, String>();
        final var jsonVal = column.getType().getJsonSchemaDataType();
        typeStuff.put("type", jsonVal.getBasicType());
        if(jsonVal.getFormat() != null){
            typeStuff.put("format", jsonVal.getFormat());
        }
        res.put(column.getName(), typeStuff);
        return res;
    }
}

