package com.santander.geobank.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.santander.geobank.application.usecases.FindNearestBranchesUseCase;
import com.santander.geobank.application.usecases.RegisterBranchUseCase;
import com.santander.geobank.domain.ports.BranchRepository;
import com.santander.geobank.domain.ports.CachePort;
import com.santander.geobank.domain.ports.DomainEventPublisher;
import com.santander.geobank.domain.services.BranchDistanceCalculator;

/**
 * ConfiguraÃ§Ã£o da camada de API REST.
 *
 * Define beans para controllers e configuraÃ§Ãµes web.
 * Implementa CORS e validaÃ§Ãµes de entrada.
 */
@Configuration
public class ApiConfiguration implements WebMvcConfigurer {

    /**
     * CORS configuration for production.
     * Restricted to Santander domains and localhost for development.
     */
    @Override
    public void addCorsMappings(@org.springframework.lang.NonNull CorsRegistry registry) {
        // Legacy endpoint support
        registry.addMapping("/desafio/**")
                .allowedOriginPatterns("https://*.santander.com.br", "http://localhost:*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // New GeoBank standardized endpoints
        registry.addMapping("/geobank/**")
                .allowedOriginPatterns("https://*.santander.com.br", "http://localhost:*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Use Case de registro de agÃªncias.
     */
    @Bean
    public RegisterBranchUseCase registerBranchUseCase(
            BranchRepository branchRepository,
            DomainEventPublisher eventPublisher,
            CachePort cachePort) {
        return new RegisterBranchUseCase(branchRepository, eventPublisher, cachePort);
    }

    /**
     * Use Case de busca por proximidade.
     */
    @Bean
    public FindNearestBranchesUseCase findNearestBranchesUseCase(
            BranchRepository branchRepository,
            BranchDistanceCalculator distanceCalculator,
            CachePort cachePort,
            DomainEventPublisher eventPublisher) {
        return new FindNearestBranchesUseCase(
                branchRepository,
                distanceCalculator,
                cachePort,
                eventPublisher);
    }

    /**
     * Implementation of DomainEventPublisher for demo purposes.
     */
    @Bean
    public DomainEventPublisher domainEventPublisher() {
        return new DomainEventPublisher() {
            @Override
            public void publish(Object event) {
                System.out.println("Event published: " + event.getClass().getSimpleName());
            }

            @Override
            public void publishAll(Object... events) {
                for (Object event : events) {
                    publish(event);
                }
            }
        };
    }

    /**
     * Branch distance calculator service.
     */
    @Bean
    public BranchDistanceCalculator branchDistanceCalculator() {
        return new BranchDistanceCalculator();
    }
}

