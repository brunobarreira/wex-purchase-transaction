package com.wex.purchasetransaction.infrastructure.external.treasury;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrencyMapper {

    private final CurrencyMappingProperties mappingProperties;

    public Optional<String> toTreasuryName(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> mappings = mappingProperties.getMappings();
        return Optional.ofNullable(mappings.get(isoCode.toUpperCase()));
    }
}
