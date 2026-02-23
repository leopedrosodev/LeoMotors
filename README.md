# Leo Motors

App Android nativo para controle local de veículos (carro e moto), com foco em:
- consumo de combustível
- gasto semanal/mensal
- distância percorrida
- quantidade de abastecimentos
- calculadora de combustível
- lembretes no início e fim do mês

Tudo funciona **offline**, sem backend.

## Tecnologias
- Kotlin
- Jetpack Compose (Material 3)
- Android Gradle Plugin 9
- Coil (imagem/GIF)
- SharedPreferences + JSON (persistência local)
- AlarmManager + BroadcastReceiver + Notification (lembretes)

## Arquitetura (resumo)
- `Presentation`: telas Compose em `MainActivity`
- `Data`: armazenamento local em `LocalStore`
- `Domain/Rules`: cálculos de relatório em `ReportCalculator`
- `Reminder`: agendamento e disparo de notificações em `reminder/`

## Requisitos
- Android Studio atualizado
- JDK 17
- SDK Android instalado
- (Opcional) `adb` para instalar via USB

## Rodar no Android Studio
1. Abra o projeto `LeoMotors`.
2. Aguarde sincronização do Gradle.
3. Execute no emulador/celular.

## Gerar APK (Debug)
No terminal, na raiz do projeto:

```bash
cd /home/leonardoti03/AndroidStudioProjects/LeoMotors
./gradlew :app:assembleDebug
```

APK gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Renomear APK para `Leo-motors.apk`
Após gerar:

```bash
cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/debug/Leo-motors.apk
```

Arquivo final:

```text
app/build/outputs/apk/debug/Leo-motors.apk
```

## Instalar no celular via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Se quiser instalar o nome customizado:

```bash
adb install -r app/build/outputs/apk/debug/Leo-motors.apk
```

## Comandos úteis
```bash
# Limpar build
./gradlew clean

# Rebuild debug
./gradlew :app:assembleDebug

# Ver dispositivos conectados
adb devices -l
```

## Estrutura principal
```text
app/
  src/main/java/br/com/leo/leomotors/
    MainActivity.kt
    data/
      Models.kt
      LocalStore.kt
      ReportCalculator.kt
    reminder/
      Reminder.kt
  src/main/res/
    drawable/
      logo_launcher_app.png
      logo_leo_motors.png
      logo_leo_motors_dark.png
      intro_presentation.gif
```

## Observações
- O app usa ícone customizado pelo `AndroidManifest.xml`.
- Se o launcher mostrar ícone antigo, desinstale e instale novamente (cache do launcher).
- Dados ficam salvos localmente no aparelho.

