package io.nuvalence.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.nuvalence.valueitems.DataTable;
import io.nuvalence.valueitems.SQLLines;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;

@Getter
@AllArgsConstructor
public class FileWriter {
    private Logger logger;

    private Path outputDirectory;

    private Path modelsDirectory;

    public FileWriter(Logger logger, Path outputDirectory){
        this(logger, outputDirectory, Paths.get(outputDirectory.toString(), "models"));
    }

    public void resetOutputDirectory() throws IOException {
        if(Files.exists(outputDirectory)){
            resetDirectory(outputDirectory);
        } else {
            Files.createDirectory(outputDirectory);
        }
        Files.createDirectory(modelsDirectory);
    }

    public Path writeSchemaFile(Map<String, Object> schemaFileDictionary) throws IOException {
        final var schemaFile = Paths.get(modelsDirectory.toString(), "schema.yml");
        final var om = new ObjectMapper(new YAMLFactory());
        om.writeValue(schemaFile.toFile(), schemaFileDictionary);

        return schemaFile;
    }

    @NotNull
    public Path writeSQLFile(DataTable table, SQLLines lines, String subfolder) throws IOException {
        final var directory = subfolder == null ? modelsDirectory: Path.of(modelsDirectory.toString(), subfolder);
        if(!Files.exists(directory)){
            Files.createDirectory(directory);
        }
        final var outputFile = Paths.get(directory.toString(), table.getBQName() + ".sql");
        logger.debug("Writing " + outputFile);
        Files.write(outputFile, lines.getOutput());

        return outputFile;
    }
    public void resetDirectory(Path dir) throws IOException {
        // Traverse the file tree in depth-first fashion and delete each file/directory.
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        logger.debug("Deleting: " + path);
                        if(Files.exists(path)) {
                            Files.delete(path);
                        }
                        Files.createDirectory(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        if(!Files.exists(dir)){
            Files.createDirectory(dir);
        }
    }

    public void delete(Path p) throws IOException{
        if(Files.exists(p)) {
            final var file = new File(p.toString());
            delete(file);
        }
    }

    private void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    public Path copyDbtFile(String fileName) throws IOException {
        final var destination = Path.of(outputDirectory.toString(), fileName);
        Files.deleteIfExists(destination);

        final var source = "./src/main/resources/dbt/" + fileName;
        Files.copy(Path.of(source), destination);

        return destination;
    }
}
