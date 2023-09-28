package io.nuvalence.valueitems;

import lombok.Getter;

import java.util.List;

@Getter
public class NormalizedSubDataTable extends DataTable {
    private final String sourceColumnName;

    public NormalizedSubDataTable(String name, List<DataColumn> columns, String sourceColumnName) {
        super(name, columns);
        this.sourceColumnName = sourceColumnName;
    }
}
