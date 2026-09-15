package dev.hyuki.investment_openapi.account.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AccountNumberProperties.class)
public class AccountConfiguration {
}
