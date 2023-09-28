package io.nuvalence.services;

import io.nuvalence.entities.Pipeline;
import io.nuvalence.valueitems.exceptions.InvalidSchemaException;

import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

/**
 * For any given tool, create the file transformations needed
 */
public interface TransformToolGeneratorService {
    List<Path> generateForSchemaTransformation(Pipeline pipeline, String datasetName) throws IOException, InvalidSchemaException;
}
