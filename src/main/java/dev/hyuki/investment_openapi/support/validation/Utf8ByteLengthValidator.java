package dev.hyuki.investment_openapi.support.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, CharSequence> {

  private int min;
  private int max;

  @Override
  public void initialize(Utf8ByteLength constraint) {
    min = constraint.min();
    max = constraint.max();
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    int length = value.toString().getBytes(StandardCharsets.UTF_8).length;
    return length >= min && length <= max;
  }
}
