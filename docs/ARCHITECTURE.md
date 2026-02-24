# Arquitetura Leo Motors

## Objetivo
Organizar o app por responsabilidades para facilitar manutenção, testes e evolução.

## Camadas
- `MainActivity`: inicialização Android e entrada do Compose.
- `UI App Shell` (`ui/LeoMotorsApp.kt`): estado global, tabs, tema, fluxo de login/sync e composição das telas.
- `UI Components`:
  - `ui/BrandingUi.kt`: top bar, splash/apresentação, badge de versão e utilitário de client id.
  - `ui/AccountSyncUi.kt`: diálogo de conta e sincronização.
  - `ui/TabsUi.kt`: telas de veículos, abastecimentos, relatórios e calculadora.
- `Data` (`data/`): modelos, persistência local e regras de relatório.
- `Cloud` (`cloud/CloudSyncService.kt`): login Google + Firebase Auth + Firestore sync.
- `Reminder` (`reminder/`): agendamento e disparo de notificações.

## Regras adotadas no refactor
- `MainActivity` sem regra de negócio.
- UI separada por contexto funcional.
- Regras de cálculo e persistência fora da camada de tela.
- Integração cloud encapsulada em serviço dedicado.

## Próximos passos recomendados
- Extrair estado de tela para `ViewModel` por feature.
- Criar interface de repositório para desacoplar `LocalStore`/cloud da UI.
- Adicionar testes unitários para `ReportCalculator` e `CloudSyncService`.
