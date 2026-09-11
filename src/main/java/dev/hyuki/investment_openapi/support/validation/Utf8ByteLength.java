package dev.hyuki.investment_openapi.support.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = Utf8ByteLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8ByteLength {

  String message() default "must be between {min} and {max} UTF-8 bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int min() default 0;

  int max() default Integer.MAX_VALUE;
}
