package com.santander.geobank.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.santander.geobank.api.dto.DistanciaRequest;
import com.santander.geobank.api.dto.DistanciaResponse;
import com.santander.geobank.api.dto.RegisterBranchRequest;
import com.santander.geobank.api.dto.RegisterBranchResponse;

/**
 * Unit tests for REST API DTOs.
 *
 * Validates validation behavior, formatting, and factory methods.
 * Follows Uncle Bob's testing principles: one concept per test.
 * * @since 1.0.0
 */
public class DesafioApiDtoTest {

        @Test
        @DisplayName("CadastrarAgenciaRequest - Should validate coordinates correctly")
        void testCadastrarAgenciaRequestValidation() {
                // Arrange & Act
                var requestValido = new RegisterBranchRequest(
                                "Test Branch",
                                "Test Address",
                                -46.6566, // valid longitude
                                -23.5614 // valid latitude
                );

                var requestInvalido = new RegisterBranchRequest(
                                "Invalid Branch",
                                "Invalid Address",
                                -200.0, // invalid longitude
                                100.0 // invalid latitude
                ); // Assert
                assertTrue(requestValido.hasValidCoordinates());
                assertFalse(requestInvalido.hasValidCoordinates());

                assertEquals("(-46.656600, -23.561400)", requestValido.getCoordinatesString());
                assertEquals("(-200.000000, 100.000000)", requestInvalido.getCoordinatesString());
        }

        @Test
        @DisplayName("CadastrarAgenciaRequest - Should validate Brazilian territory")
        void testBrazilianCoordinateValidation() {
                // Arrange & Act
                var requestBrasil = new RegisterBranchRequest(
                                "Brazil Branch",
                                "BR Address",
                                -46.6566, // Longitude within Brazil
                                -23.5614 // Latitude within Brazil
                );

                var requestForaBrasil = new RegisterBranchRequest(
                                "Foreign Branch",
                                "Foreign Address",
                                0.0, // Longitude outside Brazil
                                0.0 // Latitude outside Brazil
                ); // Assert
                assertTrue(requestBrasil.isValidBrazilianCoordinates());
                assertFalse(requestForaBrasil.isValidBrazilianCoordinates());
        }

        @Test
        @DisplayName("CadastrarAgenciaResponse - Should create success response")
        void testCadastrarAgenciaResponseSucesso() {
                // Act
                var response = RegisterBranchResponse.success(
                                "123",
                                "Test Branch",
                                "Test Address",
                                -46.6566,
                                -23.5614); // Assert
                assertNotNull(response);
                assertEquals("123", response.id());
                // Test response content
                assertEquals("Test Branch", response.name());
                assertEquals("Test Address", response.address());
                assertEquals(-46.6566, response.longitude());
                assertEquals(-23.5614, response.latitude());
                assertEquals("REGISTERED", response.status());
                assertNotNull(response.createdAt());
                assertEquals("Branch successfully registered", response.message());
        }

        @Test
        @DisplayName("CadastrarAgenciaResponse - Should create error response")
        void testCadastrarAgenciaResponseErro() {
                // Act
                var response = RegisterBranchResponse.error("Invalid coordinates");

                // Assert
                assertNotNull(response);
                assertNull(response.id());
                assertNull(response.name());
                assertNull(response.address());
                assertNull(response.longitude());
                assertNull(response.latitude());
                assertEquals("ERROR", response.status());
                assertNotNull(response.createdAt());
                assertEquals("Invalid coordinates", response.message());
        }

        @Test
        @DisplayName("DistanciaRequest - Should validate coordinates")
        void testDistanciaRequestValidation() {
                // Arrange & Act
                var requestValido = new DistanciaRequest(-46.6566, -23.5614, 10);
                var requestInvalido = new DistanciaRequest(-200.0, 100.0, 10);

                // Assert
                assertTrue(requestValido.hasValidCoordinates());
                assertFalse(requestInvalido.hasValidCoordinates());

                // Test coordinates string representation
                assertEquals("User at (-46.656600, -23.561400)",
                                requestValido.getCoordinatesString());
        }

        @Test
        @DisplayName("DistanciaRequest - Should apply safe limit")
        void testDistanciaRequestLimiteSeguro() {
                // Arrange & Act
                var requestSemLimite = new DistanciaRequest(-46.6566, -23.5614, null);
                var requestLimiteBaixo = new DistanciaRequest(-46.6566, -23.5614, 5);
                var requestLimiteAlto = new DistanciaRequest(-46.6566, -23.5614, 1000);

                // Assert
                assertEquals(10, requestSemLimite.getLimiteSeguro()); // Default is 10
                assertEquals(5, requestLimiteBaixo.getLimiteSeguro()); // Keeps low value
                assertEquals(100, requestLimiteAlto.getLimiteSeguro()); // Applies maximum 100
        }

        @Test
        @DisplayName("DistanciaResponse - Should construct success response")
        void testDistanciaResponseSucesso() {
                // Arrange
                var agencias = java.util.List.of(
                                DistanciaResponse.AgenciaDistancia.from(
                                                "1", "Downtown Branch", "Main Street 1", -46.6566, -23.5614, 1.5),
                                DistanciaResponse.AgenciaDistancia.from(
                                                "2", "Central Branch", "Main Street 2", -46.6600, -23.5600, 2.3));

                // Act
                var response = DistanciaResponse.sucesso(
                                "(-46.6566, -23.5614)",
                                agencias,
                                "150ms",
                                false);

                // Assert
                assertNotNull(response);
                assertEquals("(-46.6566, -23.5614)", response.posicaoUsuario());
                assertEquals(2, response.totalAgenciasEncontradas());
                assertEquals(2, response.agencias().size());
                assertEquals("150ms", response.tempoConsulta());
                assertFalse(response.cacheUtilizado());

                // Verify first agency
                var agencia1 = response.agencias().get(0);
                assertEquals("1", agencia1.id());
                assertEquals("Downtown Branch", agencia1.nome());
                assertEquals("1,50 km", agencia1.distanciaFormatada());
        }

        @Test
        @DisplayName("AgenciaDistancia - Should format distance correctly")
        void testAgenciaDistanciaFormatting() {
                // Act
                var agencia1 = DistanciaResponse.AgenciaDistancia.from(
                                "1", "Downtown Branch", "Main Street 1", -46.6566, -23.5614, 1.234);

                var agencia2 = DistanciaResponse.AgenciaDistancia.from(
                                "2", "Central Branch", "Main Street 2", -46.6566, -23.5614, 0.567);

                // Assert
                assertEquals("1,23 km", agencia1.distanciaFormatada()); // Brazilian format (comma)
                assertEquals("567 metros", agencia2.distanciaFormatada()); // Distance < 1km in meters
        }

        @Test
        @DisplayName("DistanciaResponse - Should construct empty response")
        void testDistanciaResponseVazia() {
                // Act
                var response = DistanciaResponse.sucesso(
                                "(-46.6566, -23.5614)",
                                java.util.List.of(),
                                "50ms",
                                true);

                // Assert
                assertNotNull(response);
                assertEquals(0, response.totalAgenciasEncontradas());
                assertTrue(response.agencias().isEmpty());
                assertTrue(response.cacheUtilizado());
        }

        @Test
        @DisplayName("Request validation - Should check null coordinates")
        void testCoordinatesNullValidation() {
                // Arrange & Act
                var requestNulos = new RegisterBranchRequest(
                                "Teste", "Teste", null, null);

                // Assert
                assertFalse(requestNulos.hasValidCoordinates());
                assertEquals("(null, null)", requestNulos.getCoordinatesString());
        }

        @Test
        @DisplayName("Coordinate boundaries - Should validate exact limits")
        void testCoordinateBoundaries() {
                // Arrange & Act - exact valid limits
                var limiteValidoMin = new RegisterBranchRequest(
                                "Teste", "Teste", -180.0, -90.0);
                var limiteValidoMax = new RegisterBranchRequest(
                                "Teste", "Teste", 180.0, 90.0);

                // Arrange & Act - invalid limits
                var limiteInvalidoMin = new RegisterBranchRequest(
                                "Teste", "Teste", -180.1, -90.1);
                var limiteInvalidoMax = new RegisterBranchRequest(
                                "Teste", "Teste", 180.1, 90.1);

                // Assert
                assertTrue(limiteValidoMin.hasValidCoordinates());
                assertTrue(limiteValidoMax.hasValidCoordinates());
                assertFalse(limiteInvalidoMin.hasValidCoordinates());
                assertFalse(limiteInvalidoMax.hasValidCoordinates());
        }
}
