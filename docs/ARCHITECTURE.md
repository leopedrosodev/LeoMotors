# Arquitetura Leo Motors (v1.2.0)

## Objetivo
Aplicar separação por camadas e feature para reduzir acoplamento e melhorar testabilidade.

## Camadas
- `presentation`: Compose UI + ViewModels + contratos de estado/evento por feature.
- `domain`: modelos de domínio, interfaces de repositório e casos de uso.
- `data`: Room (local), Firebase (remoto), mapeadores e implementações de repositório.
- `core`: DI manual (`AppContainer`) e utilitários compartilhados.

## Estrutura principal
- `MainActivity`: bootstrap Android somente.
- `core/di/AppContainer.kt`: composição de dependências da aplicação.
- `data/local/`: banco Room (`LeoMotorsDatabase`), DAOs, entities e snapshot local.
- `data/local/migration/`: importação legada de `SharedPreferences` para Room.
- `data/repository/`: implementações de `VehicleRepository`, `RefuelRepository`, `MaintenanceRepository`, `SettingsRepository`, `SnapshotRepository` e `SyncRepository`.
- `data/remote/RemoteSnapshotMapper.kt`: compatibilidade de schema remoto (incluindo documentos antigos sem manutenção).
- `domain/repository/Repositories.kt`: contratos de abstração.
- `domain/usecase/`: regras de negócio por feature.
- `presentation/app/LeoMotorsRoot.kt`: shell da aplicação (tabs, toolbar, tema, login Google).
- `presentation/vehicles|refuels|maintenance|reports|account/`: ViewModels e telas específicas.

## Fluxo de dados
1. UI dispara `UiEvent`.
2. ViewModel orquestra casos de uso.
3. Caso de uso chama interface de repositório (`domain`).
4. Repositório concreto (`data`) lê/escreve em Room e/ou Firebase.
5. `Flow` retorna para ViewModel e atualiza `UiState`.

## Persistência e migração
- Persistência local principal: Room.
- Migração legada: `LegacyImportManager` importa dados de `SharedPreferences` no primeiro boot.
- Critérios da migração:
  - preserva IDs antigos
  - evita duplicidade
  - marca flag transacional `legacyImportDone`

## Sincronização
- `SyncRepositoryImpl` encapsula Firebase Auth + Firestore.
- Estratégia de conflito atual: timestamp `updatedAtMillis` mais recente vence.
- Compatibilidade mantida para snapshots antigos (sem `maintenanceRecords`).

## Testes
- Unit tests:
  - regras de relatório
  - status de manutenção
  - exportação CSV
  - compatibilidade de mapper remoto
  - comportamento de ViewModels
- Instrumentation tests:
  - migração legada Room
  - smoke de navegação/salvamento/persistência

## Decisões de arquitetura
- 1 módulo Android (`app`) com separação forte por pacotes.
- DI manual (sem Hilt/Koin) para manter simplicidade do projeto.
- UI sem dependência direta de storage/cloud.
