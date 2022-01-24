package io.github.joakimkistowski.annotationconfigurator;

import lombok.Getter;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationPropertyConfigContextTest {

    static final String TEST_PROPERTIES_FILE_PATH = "annotationpropertyconfigcontexttest-config.properties";
    static final String TEST_PROPERTIES_OVERRIDE_FILE_PATH = "annotationpropertyconfigcontexttest-override-config.properties";
    static final String TEST_PROPERTIES_BROKEN_FILE_PATH = "annotationpropertyconfigcontexttest-broken-config.properties";

    @Test
    public void readsStringProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getStringSetting()).isEqualTo("stringSetting");
    }

    @Test
    public void readsBooleanProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.isBooleanSetting()).isFalse();
    }

    @Test
    public void readsBooleanClassProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.isBooleanSetting()).isFalse();
    }

    @Test
    public void readsIntProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getIntSetting()).isEqualTo(5);
    }

    @Test
    public void readsIntClassProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getIntClassSetting()).isEqualTo(6);
    }

    @Test
    public void readsFloatProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getFloatSetting()).isEqualTo(4.0f, Offset.offset(0.0001f));
    }

    @Test
    public void readsFloatClassProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getFloatClassSetting()).isEqualTo(6.0f, Offset.offset(0.0001f));
    }

    @Test
    public void readsDoubleProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getDoubleSetting()).isEqualTo(5.0, Offset.offset(0.0001));
    }

    @Test
    public void readsDoubleClassProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getDoubleClassSetting()).isEqualTo(7.0, Offset.offset(0.0001));
    }

    @Test
    public void readsEnumClassProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getEnumClassSetting()).isEqualTo(PropertyEnum.PROP1);
    }

    @Test
    public void readsListClassProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getListSetting()).containsExactly(5, 55, 555);
    }

    @Test
    public void overridesProperties() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(
                List.of(TEST_PROPERTIES_FILE_PATH, TEST_PROPERTIES_OVERRIDE_FILE_PATH)
        );
        assertThat(settings.getIntSetting()).isEqualTo(55);
    }

    @Test
    public void overridesPropertiesUsingEnvironmentVariables() {
        WorkingPropertySettingsReader settings = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getPath()).isNotEmpty();
    }

    @Test
    public void ignoresMissingPropertyFile() {
        WorkingPropertySettingsReader settings
                = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH, "missing.properties"));
        assertThat(settings.getIntSetting()).isEqualTo(5);
    }

    @Test
    public void ignoresNullPropertyFile() {
        List<String> files = new ArrayList<>();
        files.add(TEST_PROPERTIES_FILE_PATH);
        files.add(null);
        WorkingPropertySettingsReader settings
                = new WorkingPropertySettingsReader(files);
        assertThat(settings.getIntSetting()).isEqualTo(5);
    }

    @Test
    public void ignoresEmptyPropertyFile() {
        WorkingPropertySettingsReader settings
                = new WorkingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH, ""));
        assertThat(settings.getIntSetting()).isEqualTo(5);
    }

    @Test
    public void skipsPropertiesWithEmptyNames() {
        SkippingPropertySettingsReader settings = new SkippingPropertySettingsReader(List.of(TEST_PROPERTIES_FILE_PATH));
        assertThat(settings.getUnnamedSetting()).isNull();
    }

    @Test
    public void breaksOnUnknownPropertyType() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> new BrokenPropertySettingsReader(TEST_PROPERTIES_BROKEN_FILE_PATH));
    }

    @Test
    public void breaksOnUnsettableField() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> new FinalPropertySettingsReader(List.of(TEST_PROPERTIES_BROKEN_FILE_PATH)));
    }

    @Getter
    public static class WorkingPropertySettingsReader {

        @Config("PATH")
        private String path = "";

        @Config("STRING_SETTING")
        private String stringSetting = "string";

        @Config("INT_SETTING")
        private int intSetting = 1;

        @Config("DOUBLE_SETTING")
        private double doubleSetting = 1.0;

        @Config("FLOAT_SETTING")
        private float floatSetting = 1.0f;

        @Config("BOOLEAN_SETTING")
        private boolean booleanSetting = true;

        @Config("INT_CLASS_SETTING")
        private Integer intClassSetting = 1;

        @Config("FLOAT_CLASS_SETTING")
        private Float floatClassSetting = 1.0f;

        @Config("DOUBLE_CLASS_SETTING")
        private Double doubleClassSetting = 1.0;

        @Config("BOOLEAN_CLASS_SETTING")
        private Boolean booleanClassSetting = true;

        @Config("ENUM_CLASS_SETTING")
        private PropertyEnum enumClassSetting = PropertyEnum.PROP2;

        @Config("LIST_SETTING")
        private List<Integer> listSetting = List.of();

        public WorkingPropertySettingsReader(List<String> propertyFilesOrdered) {
            new AnnotationPropertyConfigContext(propertyFilesOrdered)
                    .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
                            this.getClass(), (field, value) -> field.set(this, value)
                    );
        }
    }

    public enum PropertyEnum {
        PROP1, PROP2
    }

    @Getter
    public static class BrokenPropertySettingsReader {
        @Config("UNSUPPORTED_TYPE_SETTING")
        private PrintWriter setting = null;

        public BrokenPropertySettingsReader(String propertyFile) {
            new AnnotationPropertyConfigContext(propertyFile)
                    .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
                            this.getClass(), (field, value) -> field.set(this, value)
                    );
        }
    }

    @Getter
    public static class FinalPropertySettingsReader {
        @Config("UNSUPPORTED_TYPE_SETTING")
        private final String setting = null;

        public FinalPropertySettingsReader(List<String> propertyFilesOrdered) {
            new AnnotationPropertyConfigContext(propertyFilesOrdered)
                    .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
                            this.getClass(), (field, value) -> field.set(this, value)
                    );
        }
    }

    @Getter
    public static class SkippingPropertySettingsReader {
        @Config("")
        private String unnamedSetting = null;

        public SkippingPropertySettingsReader(List<String> propertyFilesOrdered) {
            new AnnotationPropertyConfigContext(propertyFilesOrdered)
                    .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
                            this.getClass(), (field, value) -> field.set(this, value)
                    );
        }
    }

}
