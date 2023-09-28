package io.nuvalence.valueitems;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RunConfiguration {
    private String projectId;
    private String datasetName;
    private String schemaTableName;
    private String transactionTableName;
}
