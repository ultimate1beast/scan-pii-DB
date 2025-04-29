package com.privsense.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de validation pour les seuils entre 0 et 1.
 * Cette annotation simplifie la validation des valeurs de seuil en combinant
 * DecimalMin et DecimalMax dans une seule annotation.
 */
@DecimalMin(value = "0.0", inclusive = true)
@DecimalMax(value = "1.0", inclusive = true)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface Threshold {
    String message() default "La valeur doit être comprise entre 0 et 1";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}