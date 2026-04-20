package com.wex.purchasetransaction.infrastructure.external.treasury;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "treasury.api")
public record TreasuryApiProperties(String baseUrl) {
}
