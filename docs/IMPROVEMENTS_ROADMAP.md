# Roadmap de Melhorias

## Fase 1 (concluida)
- Refactor de UI por responsabilidade.
- UX de seleção de veículo no abastecimento (cards com animação).
- DatePicker nativo para lançamentos.
- Validação de domínio para odômetro/abastecimento.
- Timeout e proteção de estado no sync em nuvem.

## Fase 2 (em andamento)
- Persistência migrada de `SharedPreferences + JSON` para `Room` com migração automática do legado.
- Estado principal movido para `ViewModel` no app shell.
- Merge de sync por entidade (substituiu overwrite por timestamp global).
- Expandir testes unitários para sync e regras de domínio.

## Fase 3
- Exportar/importar backup local (JSON) pelo usuário.
- Gerenciar remoções e versionamento por entidade no sync.
- Melhorar telemetria de erros de sync.
- Pipeline de release (assinado) com versionamento/changelog.
