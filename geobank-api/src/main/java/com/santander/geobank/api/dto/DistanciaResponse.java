package com.santander.geobank.api.dto;

import java.util.List;

/**
 * DTO de resposta para consulta de distÃ¢ncia.
 *
 * Retorna lista de agÃªncias ordenadas por proximidade conforme especificaÃ§Ã£o.
 */
public record DistanciaResponse(
        String posicaoUsuario,
        Integer totalAgenciasEncontradas,
        List<AgenciaDistancia> agencias,
        String tempoConsulta,
        Boolean cacheUtilizado) {

    /**
     * DTO aninhado representando agÃªncia com distÃ¢ncia calculada.
     */
    public record AgenciaDistancia(
            String id,
            String nome,
            String endereco,
            Double posX,
            Double posY,
            Double distanciaKm,
            String distanciaFormatada) {

        /**
         * Factory method para criar AgenciaDistancia.
         */
        public static AgenciaDistancia from(
                String id,
                String nome,
                String endereco,
                Double posX,
                Double posY,
                Double distancia) {
            return new AgenciaDistancia(
                    id,
                    nome,
                    endereco,
                    posX,
                    posY,
                    distancia,
                    formatarDistancia(distancia));
        }

        private static String formatarDistancia(Double distancia) {
            if (distancia == null)
                return "N/A";
            if (distancia < 1.0) {
                return String.format("%.0f metros", distancia * 1000);
            }
            return String.format("%.2f km", distancia);
        }
    }

    /**
     * Factory method para criar resposta de sucesso.
     */
    public static DistanciaResponse sucesso(
            String posicaoUsuario,
            List<AgenciaDistancia> agencias,
            String tempoConsulta,
            Boolean cacheUtilizado) {
        return new DistanciaResponse(
                posicaoUsuario,
                agencias.size(),
                agencias,
                tempoConsulta,
                cacheUtilizado);
    }

    /**
     * Factory method para resposta vazia.
     */
    public static DistanciaResponse vazia(String posicaoUsuario, String tempoConsulta) {
        return new DistanciaResponse(
                posicaoUsuario,
                0,
                List.of(),
                tempoConsulta,
                false);
    }
}

