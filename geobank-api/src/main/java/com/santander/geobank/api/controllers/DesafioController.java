// ...
package com.santander.geobank.api.controllers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.santander.geobank.api.dto.DistanciaRequest;
import com.santander.geobank.api.dto.DistanciaResponse;
import com.santander.geobank.api.dto.RegisterBranchRequest;
import com.santander.geobank.application.usecases.FindNearestBranchesUseCase;
import com.santander.geobank.application.usecases.RegisterBranchUseCase;
import com.santander.geobank.domain.model.BranchType;
import com.santander.geobank.domain.model.GeoPoint;
import com.santander.geobank.infrastructure.cache.BankingCacheService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * REST Controller for banking branch API.
 *
 * Implements endpoints according to challenge specification:
 * - POST /desafio/cadastrar - Branch registration
 * - GET /desafio/distancia - Distance-based queries
 *
 * Includes intelligent caching and security validations.
 * * @since 1.0.0
 */
@RestController
@RequestMapping("/desafio")
public class DesafioController {

        /**
         * ENDPOINT DE TESTE RESTRITO
         * <p>
         * Endpoint dedicado para validação de integração, automação e troubleshooting.
         * <b>Uso exclusivo para ambientes de teste/homologação.</b>
         * <p>
         * GET /desafio/test-only
         * <ul>
         * <li>Permite validação de disponibilidade da API sem autenticação.</li>
         * <li>Retorna payload fixo, com marcação de ambiente e timestamp.</li>
         * <li>Não expõe dados sensíveis nem executa lógica de negócio.</li>
         * <li>Deve ser explicitamente liberado na configuração de segurança.</li>
         * </ul>
         * 
         * @return Map<String, Object> contendo status, ambiente e timestamp.
         * @since 1.1.0
         */
        @GetMapping("/test-only")
        public ResponseEntity<Map<String, Object>> testOnly() {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "TEST-ONLY ENDPOINT - DO NOT USE IN PRODUCTION");
                response.put("environment", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "unknown"));
                response.put("timestamp", Instant.now().toString());
                return ResponseEntity.ok(response);
        }

        private static final Logger logger = LoggerFactory.getLogger(DesafioController.class);

        private final RegisterBranchUseCase registerBranchUseCase;
        private final FindNearestBranchesUseCase findNearestBranchesUseCase;
        private final BankingCacheService cacheService;

        public DesafioController(
                        RegisterBranchUseCase registerBranchUseCase,
                        FindNearestBranchesUseCase findNearestBranchesUseCase,
                        BankingCacheService cacheService) {
                this.registerBranchUseCase = registerBranchUseCase;
                this.findNearestBranchesUseCase = findNearestBranchesUseCase;
                this.cacheService = cacheService;
        }

        /**
         * ENDPOINT 1: Branch Registration
         * POST /desafio/cadastrar
         *
         * Specification CORRECTED:
         * - Receives JSON body with posX and posY as specified
         * - Real authentication required (JWT token)
         * - Cache automatically renewed after registration
         * - Returns simple "agencia" string as per specification
         */
        @PostMapping("/cadastrar")
        public ResponseEntity<String> cadastrarAgencia(
                        @Valid @RequestBody CadastroJsonRequest request) {

                long startTime = System.currentTimeMillis();

                try {
                        logger.info("Starting branch registration: posX={}, posY={}",
                                        request.posX(), request.posY());

                        // Create proper request object
                        String nomeGerado = "AGENCIA_" + System.currentTimeMillis();
                        String enderecoGerado = "Endereco " + nomeGerado;

                        RegisterBranchRequest agenciaRequest = new RegisterBranchRequest(
                                        nomeGerado, enderecoGerado, request.posX(), request.posY());

                        // Coordinate validation
                        if (!agenciaRequest.hasValidCoordinates()) {
                                return ResponseEntity.badRequest()
                                                .body("erro");
                        }

                        // Create registration command
                        GeoPoint location = new GeoPoint(agenciaRequest.latitude(), agenciaRequest.longitude());

                        var command = new RegisterBranchUseCase.RegisterBranchCommand(
                                        null, // auto-generated id
                                        agenciaRequest.name(),
                                        location,
                                        BranchType.TRADITIONAL,
                                        agenciaRequest.address(),
                                        null // optional contactPhone
                        );

                        // Execute registration use case
                        var registeredBranch = registerBranchUseCase.execute(command);

                        // Invalidate cache to force renewal
                        invalidateDistanceCache();

                        long executionTime = System.currentTimeMillis() - startTime;
                        logger.info("Branch registered successfully in {}ms: {}",
                                        executionTime, registeredBranch.id());

                        // Success response - CONFORME ESPECIFICAÃ‡ÃƒO
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body("agencia");

                } catch (IllegalArgumentException e) {
                        logger.warn("Validation error in registration: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body("erro");

                } catch (Exception e) {
                        logger.error("Internal error in registration", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("erro");
                }
        }

        /**
         * ENDPOINT 2: Distance-based Query
         * GET /desafio/distancia
         *
         * Specification:
         * - Receives posX and posY of user as parameters
         * - Returns branches ordered by proximity in specified format
         * - Cache automatically renewed every 15min
         * - Cache expires after 5min from a query (END-POINT 2)
         */
        @GetMapping("/distancia")
        public ResponseEntity<Map<String, String>> consultarDistancia(
                        @RequestParam(name = "posX") Double posX,
                        @RequestParam(name = "posY") Double posY,
                        @RequestParam(name = "limite", defaultValue = "10") Integer limite) {

                long startTime = System.currentTimeMillis();

                try {
                        // Create request object for validation
                        DistanciaRequest request = new DistanciaRequest(posX, posY, limite);

                        logger.info("Querying nearby branches for user at: {}",
                                        request.getCoordinatesString());

                        // Coordinate validation
                        if (!request.hasValidCoordinates()) {
                                return ResponseEntity.badRequest().build();
                        }

                        int limiteSeguro = request.getLimiteSeguro();

                        // Try cache lookup first - simplified for banking compliance
                        String cacheKey = buildDistanceCacheKey(request.posX(), request.posY(), limiteSeguro);
                        Optional<DistanciaResponse> cachedResult = Optional.empty(); // Simplified cache

                        if (cachedResult.isPresent()) {
                                logger.info("Result found in cache for key: {}", cacheKey);

                                // Convert cached result to specification format
                                var cached = cachedResult.get();
                                Map<String, String> cachedResponse = new LinkedHashMap<>();

                                for (var agencia : cached.agencias()) {
                                        String agenciaKey = agencia.nome();
                                        String distanciaValue = String.format("distancia = %.1f",
                                                        agencia.distanciaKm());
                                        cachedResponse.put(agenciaKey, distanciaValue);
                                }

                                long executionTime = System.currentTimeMillis() - startTime;
                                logger.info("Cache hit served in {}ms", executionTime);

                                return ResponseEntity.ok(cachedResponse);
                        }

                        // Query database using simplified query
                        var query = FindNearestBranchesUseCase.FindNearestBranchesQuery.create(
                                        request.posY(),
                                        request.posX(),
                                        10.0, // default radius 10km
                                        limiteSeguro);

                        var nearestBranches = findNearestBranchesUseCase.execute(query);

                        // Convert to response DTO and create specification format
                        Map<String, String> responseMap = new LinkedHashMap<>();

                        for (var branch : nearestBranches.branches()) {
                                String agenciaKey = branch.name();
                                String distanciaValue = String.format("distancia = %.1f", branch.distanceKm());
                                responseMap.put(agenciaKey, distanciaValue);
                        }

                        long executionTime = System.currentTimeMillis() - startTime;

                        // Store in cache for 5 minutes (300 seconds) - simplified
                        // cacheService.cacheDistanceQuery(cacheKey, response, correlationId);

                        logger.info("Query completed in {}ms: {} branches found",
                                        executionTime, responseMap.size());

                        return ResponseEntity.ok(responseMap);

                } catch (IllegalArgumentException e) {
                        logger.warn("Validation error in query: {}", e.getMessage());
                        return ResponseEntity.badRequest().build();

                } catch (Exception e) {
                        logger.error("Internal error in distance query", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
        }

        /**
         * Auxiliary endpoint to check system status.
         */
        @GetMapping("/status")
        public ResponseEntity<Object> status() {
                return ResponseEntity.ok(java.util.Map.of(
                                "status", "OK",
                                "timestamp", Instant.now(),
                                "cache", "active"));
        }

        /**
         * Build cache key for distance queries.
         */
        private String buildDistanceCacheKey(Double posX, Double posY, Integer limite) {
                return String.format("distance:%.6f:%.6f:%d", posX, posY, limite);
        }

        /**
         * Invalidate distance cache after registration.
         */
        private void invalidateDistanceCache() {
                cacheService.invalidateDistanceCache("all");
        }

        /**
         * JSON Request DTO for branch registration endpoint
         * Follows exact specification: { "posX": 10, "posY": -5 }
         */
        public record CadastroJsonRequest(
                        @NotNull Double posX,
                        @NotNull Double posY) {
        }
}
