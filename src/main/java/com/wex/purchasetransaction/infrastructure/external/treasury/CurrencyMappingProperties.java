package com.wex.purchasetransaction.infrastructure.external.treasury;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "treasury.currency")
@Getter
@Setter
public class CurrencyMappingProperties {

    private Map<String, String> mappings;
}
