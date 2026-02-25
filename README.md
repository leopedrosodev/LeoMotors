# Leo Motors

App Android nativo para controle de carro e moto, com foco em:
- consumo de combustível
- gasto semanal/mensal
- distância percorrida
- quantidade de abastecimentos
- manutenção preventiva
- calculadora de combustível
- lembretes no início e fim do mês
- login Google + sincronização na nuvem (opcional)

Versão atual: `1.2.0 (5)`

## Stack
- Kotlin
- Jetpack Compose (Material 3)
- Room (persistência local)
- Firebase Auth + Firestore (sync opcional)
- AlarmManager + Notification
- KSP (codegen Room)

## Arquitetura
- `presentation` -> `domain` -> `data`
- DI manual com `AppContainer`
- telas por feature com `UiState` + `UiEvent`
- sem acesso direto da UI a persistência/cloud

Detalhes: `docs/ARCHITECTURE.md`

## Requisitos
- Android Studio atualizado
- JDK 17
- SDK Android instalado

## Configurar Firebase (opcional)
1. Crie o projeto no Firebase.
2. Habilite Google Sign-In em Authentication.
3. Crie Firestore.
4. Registre app Android com pacote `br.com.leo.leomotors`.
5. Baixe `google-services.json`.
6. Copie para `app/google-services.json`.
7. Sincronize Gradle.

Sem esse arquivo o app funciona localmente, mas sem login/sync.

## Comandos principais
```bash
cd /home/leonardoti03/codes/github/LeoMotors

# Build debug
./gradlew :app:assembleDebug

# Testes unitários
./gradlew :app:testDebugUnitTest

# Compilar APK de testes instrumentados
./gradlew :app:assembleAndroidTest
```

## APK
Gerado em:
- `app/build/outputs/apk/debug/app-debug.apk`

Renomear para `Leo-motors.apk`:
```bash
cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/debug/Leo-motors.apk
```

## Instalar via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Estrutura atual (resumo)
```text
app/src/main/java/br/com/leo/leomotors/
  MainActivity.kt
  core/
    di/AppContainer.kt
  data/
    local/
    remote/
    repository/
    CsvExporter.kt
  domain/
    model/
    repository/
    usecase/
  presentation/
    app/
    vehicles/
    refuels/
    maintenance/
    reports/
    account/
  reminder/
  ui/theme/
```

## Observações
- Há migração automática de dados legados de `SharedPreferences` para Room.
- O app exibe versão no canto inferior direito.
- Ícone customizado está no manifest (`logo_launcher_app`).
