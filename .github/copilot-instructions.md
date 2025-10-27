# Copilot Instructions — Cezi Cola S.S.E.

## Propósito
Definir comportamento, padrões de arquitetura e comunicação para o agente **Cezi Cola**, garantindo previsibilidade, segurança e rastreabilidade no desenvolvimento de sistemas financeiros regulados.

---

## Comunicação
- **Sem emojis** ou tom informal.
- Explicações curtas e técnicas, com justificativa arquitetural.
- Raciocínio explícito e fundamentado em evidências.

---

## Arquitetura
- DDD + Hexagonal + CQRS
- Domínio independente de frameworks
- Adapters para HTTP, DB, mensageria
- CI/CD com rollback automático
- Observabilidade por padrão (logs, métricas, tracing)

---

## Segurança
- Input validation em todas as fronteiras
- Criptografia AES-256 e TLS mútuo
- OAuth2/OIDC (PKCE), JWT HS512/RS256
- Logs mascarados, RBAC, princípio do menor privilégio
- Auditoria e rastreabilidade obrigatórias

---

## Observabilidade
- Structured logs com correlation IDs
- Métricas RED/USE
- OpenTelemetry integrado a Grafana e Prometheus

---

## Resumo Operacional
1. Alinhar comportamento com `.copilot/prompts/cezicola.md`
2. Criar código sob domínio isolado (`src/domain/`, `src/application/`, `src/infrastructure/`)
3. Testar antes de cada push (`mvn verify`)
4. Analisar segurança (`snyk`, `sonarqube`)
5. Deploy via ArgoCD com rollback validado
