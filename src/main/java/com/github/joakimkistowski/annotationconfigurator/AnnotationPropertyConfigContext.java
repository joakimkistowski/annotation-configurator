package com.github.joakimkistowski.annotationconfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Reads configuration properties from property files and environment variables
 * and provides methods to inject these config properties into other classes' attributes using the @{@link Config}-Annotation.
 *
 * <p>
 * Example use:
 * </p>
 *
 * <pre>
 * {@code
 * new AnnotationPropertyConfigContext("myfile.properties")
 *                     .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
 *                             this.getClass(), (field, value) -&gt; field.set(this, value)
 *                     );
 * }
 * </pre>
 *
 * @author Joakim von Kistowski
 */
public final class AnnotationPropertyConfigContext {

    private static final Logger log = LoggerFactory.getLogger(AnnotationPropertyConfigContext.class);

    private final Properties settings;

    /**
     * Create a new AnnotationPropertyConfigContext that reads configurations from a given property file.
     * @param propertyFile The property file to read from.
     */
    public AnnotationPropertyConfigContext(String propertyFile) {
        this(List.of(propertyFile));
    }

    /**
     * Create a new AnnotationPropertyConfigContext that reads configurations from an ordered list of property files.
     * Properties in files placed later in the list will override properties of files specified earlier in the list.
     * @param propertyFilesOrdered The property files to read from.
     */
    public AnnotationPropertyConfigContext(List<String> propertyFilesOrdered) {
        settings = readProperties(propertyFilesOrdered);
    }



    private Properties readProperties(List<String> configFilesOrdered) {
        Properties mergedProperties = new Properties();
        for (String configFile : configFilesOrdered) {
            mergedProperties.putAll(readPropertyFile(configFile));
        }
        return mergedProperties;
    }

    private Properties readPropertyFile(String filePathInResources) {
        Properties mergedProperties = new Properties();
        if (isNullOrEmpty(filePathInResources)) {
            return mergedProperties;
        }
        ClassPathFileTools.getClassPathInputStream(filePathInResources).ifPresent(is -> {
            try {
                Properties props = new Properties();
                props.load(is);
                mergedProperties.putAll(props);
                log.info("Added properties from file: {}", filePathInResources);
            } catch (IOException e) {
                log.error("Error reading properties file", e);
                throw new UncheckedIOException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("Error closing properties file", e);
                }
            }
        });
        return mergedProperties;
    }

    /**
     * Looks for attributes annotated with @{@link Config} in the given class
     * and sets these attributes to values stored in this {@link AnnotationPropertyConfigContext}.
     *
     * <p>Example:</p>
     *
     * <p>
     * Given that MY_CONFIG_FLAG is a value that was read from a properties file or environment variable
     * and given a class that has the following attribute:
     * </p>
     * <pre>
     *{@code
     * {@literal @}Config("MY_CONFIG_FLAG")
     * private String myConfigFlag;
     * }
     * </pre>
     *
     * <p>
     *     Calling the following code will cause myConfigFlag to be set to the previously read value:
     * </p>
     * <pre>
     * {@code
     * myAnnotationPropertyConfigContext
     *                     .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
     *                             this.getClass(), (field, value) -&gt; field.set(this, value)
     *                     );
     * }
     * </pre>
     *
     * @param thisClass The class in which to set the attributes. Usually the calling class.
     * @param fieldSetter A lambda expression specifying the function for setting fields.
     *                    Usually: (field, value) -&gt; field.set(this, value)
     */
    public void configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(Class<?> thisClass, FieldSetter fieldSetter) {
        for (Field field : thisClass.getDeclaredFields()) {
            Config configAnnotation = field.getAnnotation(Config.class);
            if (configAnnotation != null) {
                if (isNullOrEmpty(configAnnotation.value())) {
                    log.warn("Field {} does not specify a setting value (a name) in its @{} annotation; skipping ...",
                            field.getName(), Config.class.getSimpleName());
                    continue;
                }
                String property = getPropertyFromEnvVarOrPropertiesMap(settings, configAnnotation.value());
                if (property == null) {
                    log.debug("No configuration for found for setting {}; leaving at default value", configAnnotation.value());
                    continue;
                }
                setField(configAnnotation.value(), field, property, fieldSetter);
            }
        }
    }

    private static void setField(String settingAnnotationValue, Field field, String property, FieldSetter fieldSetter) {
        try {
            Object toSet;
            if (field.getType().isAssignableFrom(List.class)) {
                toSet = convertToListType(field, property);
            } else {
                toSet = convertToExpectedType(field.getType(), field.getName(), property);
            }
            fieldSetter.setField(field, toSet);
            if (isPasswordSetting(settingAnnotationValue)) {
                log.info("Configured setting {}", settingAnnotationValue);
            } else {
                log.info("Configured setting: {} = {}", settingAnnotationValue, property);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to set field \"" + field.getName() + "\"", e);
        }
    }

    private static Object convertToExpectedType(Class<?> fieldType, String fieldName, String property) {
        if (fieldType.equals(String.class)) {
            return property;
        } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
            return Integer.parseInt(property);
        } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            return Boolean.parseBoolean(property);
        } else if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
            return Float.parseFloat(property);
        } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return Double.parseDouble(property);
        } else if (fieldType.isEnum()) {
            return Enum.valueOf((Class<Enum>) fieldType, property);
        } else {
            throw new IllegalStateException("Illegal property type for field \"" + fieldName
                    + "\"; must be one of: String, boolean, int, double");
        }
    }

    private static List<?> convertToListType(Field field, String property) {
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        if (listType.getActualTypeArguments().length < 1) {
            throw new IllegalStateException("Unable to get generic list type. List has no actual type arguments.");
        }
        Class<?> listTypeClass = (Class<?>) listType.getActualTypeArguments()[0];
        String[] listElements = property.split(",");
        return Arrays.stream(listElements).map(String::strip)
                .map(element -> convertToExpectedType(listTypeClass, field.getName(), element))
                .collect(Collectors.toList());
    }

    private static boolean isPasswordSetting(String settingAnnotationValue) {
        String lowerCaseSettingAnnotationValue = settingAnnotationValue.toLowerCase(Locale.ROOT);
        return lowerCaseSettingAnnotationValue.contains("password")
                || lowerCaseSettingAnnotationValue.contains("key");
    }

    /**
     * Reads the configuration property from the environment or from pre-parsed
     * property files. Kinda slow, call only on initialization.
     *
     * @param properties The existing properties.
     * @param name       The property (and env var) name.
     */
    private static String getPropertyFromEnvVarOrPropertiesMap(Properties properties, String name) {
        String prop = System.getenv(name);
        if (prop != null) {
            return prop.strip();
        }
        return properties.getProperty(name);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * Functional interface for setting a fields value.
     * Is usually implemented by the calling class using the following lambda: (field, value) -&gt; field.set(this, value)
     */
    public interface FieldSetter {
        /**
         * Setter to set the field.
         * Is usually implemented by the calling class using the following lambda: (field, value) -&gt; field.set(this, value)
         * @param field The field to set.
         * @param value The value to set the field with.
         * @throws IllegalAccessException If the calling class does not have sufficient privileges for setting the field.
         */
        void setField(Field field, Object value) throws IllegalAccessException;
    }
}
