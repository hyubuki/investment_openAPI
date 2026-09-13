package dev.hyuki.investment_openapi.user.service;

import dev.hyuki.investment_openapi.auth.token.TokenPair;

public record RegisteredUserSession(
    RegisteredUser user,
    TokenPair tokens
) {
}
