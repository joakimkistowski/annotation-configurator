package io.github.joakimkistowski.annotationconfigurator;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClassPathFileToolsTest {

    private static final String TEST_DIRECTORY_PATH = "statictest";

    @Test
    public void getsResourceOnClassPath() {
        // given
        String resourceName = AnnotationPropertyConfigContextTest.TEST_PROPERTIES_FILE_PATH;
        // when
        Optional<InputStream> is = ClassPathFileTools.getClassPathInputStream(resourceName);
        // then
        assertThat(is).isPresent();
    }

    @Test
    public void getsFilesInDirectory() {
        // when
        var found = ClassPathFileTools.getFileNamesInDirectory(TEST_DIRECTORY_PATH);
        // then
        assertThat(found).contains("test.txt");
        assertThat(found).doesNotContain("pic", "scripts", ".");
    }

    @Test
    public void doesNotGetFilesInMissingDirectory() {
        // when, then
        assertThrows(
                IllegalStateException.class,
                () -> ClassPathFileTools.getFileNamesInDirectory("missing_directory")
        );
    }

}
