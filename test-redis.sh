#!/bin/bash
# Teste simples do Redis

echo "=== TESTE REDIS GEOBANK ==="

# 1. Verificar se Redis container está rodando
echo "1. Status do container Redis:"
docker ps | grep redis

# 2. Testar conectividade Redis
echo -e "\n2. Teste de conectividade:"
docker exec geobank-api-redis-1 redis-cli ping

# 3. Testar operações básicas
echo -e "\n3. Teste SET/GET:"
docker exec geobank-api-redis-1 redis-cli set "geobank:test" "funcionando"
docker exec geobank-api-redis-1 redis-cli get "geobank:test"

# 4. Verificar configuração
echo -e "\n4. Configuração Redis:"
docker exec geobank-api-redis-1 redis-cli info server | head -5

echo -e "\n=== TESTE CONCLUÍDO ==="
