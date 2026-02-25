# Roadmap de Melhorias

## Fase 1 (em andamento)
- Refactor de UI por responsabilidade.
- UX de seleção de veículo no abastecimento (cards com animação).
- DatePicker nativo para lançamentos.
- Validação de domínio para odômetro/abastecimento.
- Timeout e proteção de estado no sync em nuvem.

## Fase 2 (próxima)
- Migrar persistência de `SharedPreferences + JSON` para `Room`.
- Criar camada de repositório para desacoplar UI do armazenamento.
- Mover estado de telas para `ViewModel` por feature.
- Expandir testes unitários de cálculos e validações.

## Fase 3
- Conflito de sincronização por entidade (não apenas timestamp global).
- Exportar/importar backup local (JSON) pelo usuário.
- Melhorar telemetria de erros de sync.
- Pipeline de release (assinado) com versionamento/changelog.
