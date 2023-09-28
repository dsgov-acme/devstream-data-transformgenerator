package io.nuvalence.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Pipeline {
    private List<BaseSchemaTransformation> schemaTransformations;
}
