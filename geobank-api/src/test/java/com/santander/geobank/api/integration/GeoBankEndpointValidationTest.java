package com.santander.geobank.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Functional validation tests for API endpoints.
 *
 * Tests basic endpoint behavior without external dependencies.
 * Validates business rules, coordinate systems, and data formatting.
 * * @since 1.0.0
 */
public class GeoBankEndpointValidationTest {

        @Test
        @DisplayName("Endpoint validation - Valid Brazilian geographical coordinates")
        void testValidBrazilianCoordinates() {
                // Arrange - coordinates of major Brazilian cities
                var saoPaulo = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Sao Paulo Branch", "Paulista Avenue 1000", -46.6566, -23.5614);
                var rio = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Rio Branch", "Copacabana Beach", -43.1729, -22.9068);
                var brasilia = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Brasilia Branch", "Esplanade", -47.8825, -15.7942);

                // Assert
                assertTrue(saoPaulo.hasValidCoordinates());
                assertTrue(saoPaulo.isValidBrazilianCoordinates());

                assertTrue(rio.hasValidCoordinates());
                assertTrue(rio.isValidBrazilianCoordinates());

                assertTrue(brasilia.hasValidCoordinates());
                assertTrue(brasilia.isValidBrazilianCoordinates());
        }

        @Test
        @DisplayName("Endpoint validation - Should reject coordinates outside Brazil")
        void testInvalidNonBrazilianCoordinates() {
                // Arrange - coordinates from other countries
                var paris = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Paris Branch", "Champs Elysees", 2.2945, 48.8582);
                var newYork = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "New York Branch", "Times Square", -73.9857, 40.7484);

                // Assert
                assertTrue(paris.hasValidCoordinates()); // Valid coordinates globally
                assertFalse(paris.isValidBrazilianCoordinates()); // But not in Brazil

                assertTrue(newYork.hasValidCoordinates());
                assertFalse(newYork.isValidBrazilianCoordinates());
        }

        @Test
        @DisplayName("Distance request - Should calculate parameters correctly")
        void testDistanceRequestCalculations() {
                // Arrange
                var requestCompleto = new com.santander.geobank.api.dto.DistanciaRequest(
                                -46.6566, -23.5614, 15);
                var requestMinimo = new com.santander.geobank.api.dto.DistanciaRequest(
                                -46.6566, -23.5614, null);

                // Assert
                assertEquals(15, requestCompleto.getLimiteSeguro());
                assertEquals(10, requestMinimo.getLimiteSeguro()); // Default

                assertTrue(requestCompleto.hasValidCoordinates());
                assertTrue(requestMinimo.hasValidCoordinates());
        }

        @Test
        @DisplayName("Response building - Should construct valid responses")
        void testResponseBuilding() {
                // Test RegisterBranchResponse
                var sucessoResponse = com.santander.geobank.api.dto.RegisterBranchResponse.success(
                                "123", "Test Branch", "Test Address", -46.6566, -23.5614);
                var erroResponse = com.santander.geobank.api.dto.RegisterBranchResponse.error(
                                "Validation error");

                assertNotNull(sucessoResponse);
                assertEquals("REGISTERED", sucessoResponse.status());
                assertNotNull(sucessoResponse.createdAt());

                assertNotNull(erroResponse);
                assertEquals("ERROR", erroResponse.status());
                assertNull(erroResponse.id());

                // Test DistanciaResponse
                var agencias = java.util.List.of(
                                com.santander.geobank.api.dto.DistanciaResponse.AgenciaDistancia.from(
                                                "1", "Branch 1", "Address 1", -46.6566, -23.5614, 2.5));
                var distanciaResponse = com.santander.geobank.api.dto.DistanciaResponse.sucesso(
                                "User position", agencias, "100ms", false);

                assertNotNull(distanciaResponse);
                assertEquals(1, distanciaResponse.totalAgenciasEncontradas());
                assertFalse(distanciaResponse.cacheUtilizado());
                assertEquals("2,50 km", distanciaResponse.agencias().get(0).distanciaFormatada());
        }

        @Test
        @DisplayName("Business rules - Limits and validations applied")
        void testBusinessRulesValidation() {
                // Test maximum query limit
                var requestLimiteAlto = new com.santander.geobank.api.dto.DistanciaRequest(
                                -46.6566, -23.5614, 500);
                assertEquals(100, requestLimiteAlto.getLimiteSeguro()); // Maximum applied

                // Test minimum limit
                var requestLimiteZero = new com.santander.geobank.api.dto.DistanciaRequest(
                                -46.6566, -23.5614, 0);
                assertEquals(10, requestLimiteZero.getLimiteSeguro()); // Default applied

                // Test coordinates at valid extremes
                var extremoSul = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Southern Extreme", "RS", -53.0, -33.0); // Approximately Brazil's southern extreme
                assertTrue(extremoSul.hasValidCoordinates());
                assertTrue(extremoSul.isValidBrazilianCoordinates());

                var extremoNorte = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Northern Extreme", "RR", -60.0, 5.0); // Approximately Brazil's northern extreme
                assertTrue(extremoNorte.hasValidCoordinates());
                assertTrue(extremoNorte.isValidBrazilianCoordinates());
        }

        @Test
        @DisplayName("Error scenarios - Should handle error cases")
        void testErrorScenarios() {
                // Null coordinates
                var coordenadasNulas = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Test", "Test", null, null);
                assertFalse(coordenadasNulas.hasValidCoordinates());

                // Invalid extreme coordinates
                var coordenadasExtremas = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Extreme", "Test", -181.0, 91.0);
                assertFalse(coordenadasExtremas.hasValidCoordinates());

                // Distance request with invalid coordinates
                var distanciaInvalida = new com.santander.geobank.api.dto.DistanciaRequest(
                                -200.0, 100.0, 10);
                assertFalse(distanciaInvalida.hasValidCoordinates());
        }

        @Test
        @DisplayName("String formatting - Should format outputs correctly")
        void testStringFormatting() {
                // Test coordinate string formatting
                var request = new com.santander.geobank.api.dto.RegisterBranchRequest(
                                "Test", "Test", -46.123456, -23.987654);
                assertEquals("(-46.123456, -23.987654)", request.getCoordinatesString());

                // Test distance formatting
                var agencia1 = com.santander.geobank.api.dto.DistanciaResponse.AgenciaDistancia.from(
                                "1", "Test", "Address", -46.0, -23.0, 1.0);
                assertEquals("1,00 km", agencia1.distanciaFormatada());

                var agencia2 = com.santander.geobank.api.dto.DistanciaResponse.AgenciaDistancia.from(
                                "2", "Test", "Address", -46.0, -23.0, 0.5);
                assertEquals("500 metros", agencia2.distanciaFormatada());

                var agencia3 = com.santander.geobank.api.dto.DistanciaResponse.AgenciaDistancia.from(
                                "3", "Test", "Address", -46.0, -23.0, null);
                assertEquals("N/A", agencia3.distanciaFormatada());
        }
}
