package com.example.temporaldemo.worker.activity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata annotation for Temporal activity methods.
 *
 * <p>Used by {@code HealthCheckWorkerConfig} to automatically register
 * activities to the definition-service catalog on startup — no manual
 * hardcoded list required.
 *
 * <p>Place this alongside {@code @ActivityMethod} on the interface method:
 * <pre>
 * {@literal @}ActivityMethod
 * {@literal @}ActivityMeta(description = "Records a patient visit",
 *              inputKeys = {"patientId", "patientName"})
 * Object recordVisit(Map&lt;String, Object&gt; input);
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ActivityMeta {

    /** Human-readable description of the activity. */
    String description() default "";

    /** Names of the keys expected in the {@code input} map. */
    String[] inputKeys() default {};

    /** Output type hint, e.g. "object", "string". */
    String outputType() default "object";
}
