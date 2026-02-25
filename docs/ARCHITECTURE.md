# Arquitetura Leo Motors

## Objetivo
Organizar o app por responsabilidades para facilitar manutenção, testes e evolução.

## Camadas
- `MainActivity`: inicialização Android e entrada do Compose.
- `UI App Shell` (`ui/LeoMotorsApp.kt`): tabs, tema, fluxo de login/sync e composição das telas.
- `UI State` (`ui/LeoMotorsViewModel.kt`): estado principal de veículos, odômetros e abastecimentos.
- `UI Components`:
  - `ui/BrandingUi.kt`: top bar, splash/apresentação, badge de versão e utilitário de client id.
  - `ui/AccountSyncUi.kt`: diálogo de conta e sincronização.
  - `ui/TabsUi.kt`: telas de veículos, abastecimentos, relatórios e calculadora.
- `Data` (`data/`):
  - modelos de domínio
  - validações de entrada
  - persistência via `LocalStore` (Room + migração de legado)
  - regras de relatório
- `Cloud` (`cloud/CloudSyncService.kt`): login Google + Firebase Auth + Firestore sync com merge por entidade.
- `Reminder` (`reminder/`): agendamento e disparo de notificações.

## Regras adotadas no refactor
- `MainActivity` sem regra de negócio.
- UI separada por contexto funcional.
- Estado principal concentrado em `ViewModel`.
- Regras de cálculo/validação/persistência fora da camada de tela.
- Integração cloud encapsulada em serviço dedicado.
- Persistência local em Room com schema versionado (`app/schemas`).

## Próximos passos recomendados
- Criar interface de repositório para desacoplar `LocalStore`/cloud da UI.
- Adicionar testes unitários de merge de sync (`CloudSyncService`).
- Evoluir sync para suportar remoções e versionamento por entidade.
