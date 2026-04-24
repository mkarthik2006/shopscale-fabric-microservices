package com.shopscale.order.service;

import com.shopscale.common.dto.PriceResponseDto;
import com.shopscale.common.dto.StandardResponse;
import com.shopscale.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class PriceClientService {

    private static final Logger log = LoggerFactory.getLogger(PriceClientService.class);

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public PriceClientService(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.builder().build();
    }

    public BigDecimal resolveUnitPrice(String sku) {
        String normalizedSku = sku == null ? "" : sku.trim().toUpperCase(Locale.ROOT);
        if (normalizedSku.isBlank()) {
            throw new BusinessException("SKU is required for price resolution");
        }

        ServiceInstance target = resolvePriceServiceInstance();
        String url = target.getUri() + "/api/v1/prices/" + normalizedSku;
        try {
            StandardResponse<PriceResponseDto> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || response.data() == null || response.data().price() == null) {
                throw new BusinessException("Price service returned invalid payload for SKU: " + normalizedSku);
            }
            return response.data().price();
        } catch (Exception ex) {
            log.error("Unable to resolve authoritative price | sku={} endpoint={}", normalizedSku, url, ex);
            throw new BusinessException("Failed to resolve price for SKU: " + normalizedSku);
        }
    }

    private ServiceInstance resolvePriceServiceInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances("price-service");
        if (instances == null || instances.isEmpty()) {
            throw new BusinessException("Price service is unavailable");
        }
        return instances.getFirst();
    }
}
