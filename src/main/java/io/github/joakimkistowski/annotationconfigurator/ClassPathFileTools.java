package io.github.joakimkistowski.annotationconfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tools for reading files and directory information from the class path.
 * Supports both classpath files that are located inside regular directories and files that are located inside nested jars.
 */
public final class ClassPathFileTools {

    private static final Logger log = LoggerFactory.getLogger(ClassPathFileTools.class);
    private static final ClassLoader CLASS_LOADER = ClassPathFileTools.class.getClassLoader();

    private ClassPathFileTools() {

    }

    /**
     * Gets the input stream for a given file inside the classpath.
     *
     * @param relativePathInResourcePath The file's path relative to the classpath.
     * @return The file as an input stream. An empty optional in case the file was not found.
     */
    public static Optional<InputStream> getClassPathInputStream(String relativePathInResourcePath) {
        var inputStream = CLASS_LOADER.getResourceAsStream(relativePathInResourcePath);
        if (inputStream == null) {
            log.warn("Did not find file in classpath resouce Path: {}", relativePathInResourcePath);
            return Optional.empty();
        }
        return Optional.of(inputStream);
    }

    /**
     * Returns a list of all non-directory files in the given path. Works both for regular directories and for directories within jars.
     *
     * @param relativeDirectoryPathInResourcePath The directory's path relative to the classpath.
     * @return List of all non-directory file names.
     */
    public static List<String> getFileNamesInDirectory(String relativeDirectoryPathInResourcePath) {
        try {
            URI uri = Optional.ofNullable(CLASS_LOADER.getResource(relativeDirectoryPathInResourcePath)).orElseThrow(
                    () -> new IllegalStateException("path does not exist: " + relativeDirectoryPathInResourcePath)
                    ).toURI();
            Path directoryPath;
            if (uri.getScheme().equals("jar")) {
                FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of());
                directoryPath = fileSystem.getPath(relativeDirectoryPathInResourcePath);
            } else {
                directoryPath = Path.of(uri);
            }
            return Files.walk(directoryPath, 1).filter(path -> {
                try {
                    return !Files.isDirectory(path) && !Files.isHidden(path);
                } catch (IOException e) {
                    log.error("IOException checking file in directory scan of: {}", relativeDirectoryPathInResourcePath, e);
                    return false;
                }
            }).map(path -> path.getFileName().toString()).collect(Collectors.toList());
        } catch (URISyntaxException e) {
            log.error("Error reading URI of classpath resource", e);
            throw new IllegalStateException("Error reading URI of classpath resource", e);
        } catch (IOException e) {
            log.error("Error scanning class path resources", e);
            throw new UncheckedIOException("Error reading URI of classpath resource", e);
        }
    }
}
