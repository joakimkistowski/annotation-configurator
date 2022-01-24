package io.github.joakimkistowski.annotationconfigurator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a class attribute as a configuration property to be set.
 *
 * @author Joakim von Kistowski
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    /**
     * The property name.
     * This is the name under which the given configuration property is found in a properties file or the name of the environment variable.
     * @return The property name.
     */
    String value();
}
