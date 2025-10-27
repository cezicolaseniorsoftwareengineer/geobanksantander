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
public class GeoBankApiDtoTest {

        @Test
        @DisplayName("RegisterBranchRequest - Should validate coordinates correctly")
        void testRegisterBranchRequestValidation() {
                // Arrange & Act
                var validRequest = new RegisterBranchRequest(
                                "Test Branch",
                                "Test Address",
                                -46.6566, // valid longitude
                                -23.5614 // valid latitude
                );

                var invalidRequest = new RegisterBranchRequest(
                                "Invalid Branch",
                                "Invalid Address",
                                -200.0, // invalid longitude
                                100.0 // invalid latitude
                );
                // Assert
                assertTrue(validRequest.hasValidCoordinates());
                assertFalse(invalidRequest.hasValidCoordinates());

                assertEquals("(-46.656600, -23.561400)", validRequest.getCoordinatesString());
                assertEquals("(-200.000000, 100.000000)", invalidRequest.getCoordinatesString());
        }

        @Test
        @DisplayName("RegisterBranchRequest - Should validate Brazilian territory")
        void testBrazilianCoordinateValidation() {
                // Arrange & Act
                var brazilRequest = new RegisterBranchRequest(
                                "Brazil Branch",
                                "BR Address",
                                -46.6566, // Longitude within Brazil
                                -23.5614 // Latitude within Brazil
                );

                var foreignRequest = new RegisterBranchRequest(
                                "Foreign Branch",
                                "Foreign Address",
                                0.0, // Longitude outside Brazil
                                0.0 // Latitude outside Brazil
                );
                // Assert
                assertTrue(brazilRequest.isValidBrazilianCoordinates());
                assertFalse(foreignRequest.isValidBrazilianCoordinates());
        }

        @Test
        @DisplayName("RegisterBranchResponse - Should create success response")
        void testRegisterBranchResponseSuccess() {
                // Act
                var response = RegisterBranchResponse.success(
                                "123",
                                "Test Branch",
                                "Test Address",
                                -46.6566,
                                -23.5614);
                // Assert
                assertNotNull(response);
                assertEquals("123", response.id());
                assertEquals("Test Branch", response.name());
                assertEquals("Test Address", response.address());
                assertEquals(-46.6566, response.longitude());
                assertEquals(-23.5614, response.latitude());
                assertEquals("REGISTERED", response.status());
                assertNotNull(response.createdAt());
                assertEquals("Branch successfully registered", response.message());
        }

        @Test
        @DisplayName("RegisterBranchResponse - Should create error response")
        void testRegisterBranchResponseError() {
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
                                                "1", "Branch 1", "Address 1", -46.6566, -23.5614, 1.5),
                                DistanciaResponse.AgenciaDistancia.from(
                                                "2", "Branch 2", "Address 2", -46.6600, -23.5600, 2.3));

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
                assertEquals("Branch 1", agencia1.nome());
                assertEquals("1,50 km", agencia1.distanciaFormatada());
        }

        @Test
        @DisplayName("AgenciaDistancia - Should format distance correctly")
        void testAgenciaDistanciaFormatting() {
                // Act
                var agencia1 = DistanciaResponse.AgenciaDistancia.from(
                                "1", "Branch 1", "Address 1", -46.6566, -23.5614, 1.234);

                var agencia2 = DistanciaResponse.AgenciaDistancia.from(
                                "2", "Branch 2", "Address 2", -46.6566, -23.5614, 0.567);

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
        @DisplayName("RegisterBranchRequest - Should check null coordinates")
        void testCoordinatesNullValidation() {
                // Arrange & Act
                var nullRequest = new RegisterBranchRequest(
                                "Test", "Test", null, null);

                // Assert
                assertFalse(nullRequest.hasValidCoordinates());
                assertEquals("(null, null)", nullRequest.getCoordinatesString());
        }

        @Test
        @DisplayName("RegisterBranchRequest - Should validate exact coordinate boundaries")
        void testCoordinateBoundaries() {
                // Arrange & Act - exact valid limits
                var validMinRequest = new RegisterBranchRequest(
                                "Test", "Test", -180.0, -90.0);
                var validMaxRequest = new RegisterBranchRequest(
                                "Test", "Test", 180.0, 90.0);

                // Arrange & Act - invalid limits
                var invalidMinRequest = new RegisterBranchRequest(
                                "Test", "Test", -180.1, -90.1);
                var invalidMaxRequest = new RegisterBranchRequest(
                                "Test", "Test", 180.1, 90.1);

                // Assert
                assertTrue(validMinRequest.hasValidCoordinates());
                assertTrue(validMaxRequest.hasValidCoordinates());
                assertFalse(invalidMinRequest.hasValidCoordinates());
                assertFalse(invalidMaxRequest.hasValidCoordinates());
        }
}
