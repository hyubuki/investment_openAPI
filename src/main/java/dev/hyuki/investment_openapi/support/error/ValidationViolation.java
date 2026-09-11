package dev.hyuki.investment_openapi.support.error;

public record ValidationViolation(String field, String reason) {
}
