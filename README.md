# Leo Motors

App Android nativo para controle local de veículos (carro e moto), com foco em:
- consumo de combustível
- gasto semanal/mensal
- distância percorrida
- quantidade de abastecimentos
- calculadora de combustível
- lembretes no início e fim do mês
- login Google + sincronização na nuvem (opcional)

O app funciona localmente e pode sincronizar dados com Firebase quando configurado.

## Tecnologias
- Kotlin
- Jetpack Compose (Material 3)
- Android Gradle Plugin 9
- Coil (imagem/GIF)
- Room (SQLite) para persistência local
- SharedPreferences para preferências simples (tema)
- Firebase Auth + Firestore (sincronização opcional)
- AlarmManager + BroadcastReceiver + Notification (lembretes)

## Arquitetura (resumo)
- `MainActivity`: somente bootstrap do app (edge-to-edge, lembretes e `setContent`)
- `Presentation/App Shell`: orquestração da navegação, estado e ações globais em `ui/LeoMotorsApp.kt`
- `Presentation/UI`: componentes e telas separados por responsabilidade (`ui/AccountSyncUi.kt`, `ui/BrandingUi.kt`, `ui/TabsUi.kt`)
- `State`: estado principal em `LeoMotorsViewModel`
- `Data`: armazenamento local em `LocalStore` com backend Room + migração automática do legado
- `Domain/Rules`: cálculos de relatório em `ReportCalculator`
- `Reminder`: agendamento e disparo de notificações em `reminder/`
- `Cloud`: autenticação Google/Firebase Auth e sincronização Firestore com merge por entidade em `cloud/CloudSyncService.kt`

Documentação detalhada: `docs/ARCHITECTURE.md`
Roadmap de melhorias: `docs/IMPROVEMENTS_ROADMAP.md`

## Requisitos
- Android Studio atualizado
- JDK 17
- SDK Android instalado
- (Opcional) `adb` para instalar via USB

## Configurar Google Services (Firebase)
1. Crie um projeto no Firebase Console.
2. Em `Authentication > Sign-in method`, habilite `Google`.
3. Crie o banco `Firestore`.
4. Adicione o app Android com pacote `br.com.leo.leomotors`.
5. Baixe o arquivo `google-services.json`.
6. Copie para:

```text
app/google-services.json
```

7. Sincronize o Gradle e rode o app.

Observações:
- Sem `google-services.json`, o app continua rodando localmente, mas login/sincronização não funcionam.
- `google_web_client_id` em `app/src/main/res/values/strings.xml` fica como fallback manual.

## Rodar no Android Studio
1. Abra o projeto `LeoMotors`.
2. Aguarde sincronização do Gradle.
3. Execute no emulador/celular.

## Gerar APK (Debug)
No terminal, na raiz do projeto:

```bash
cd /home/leonardoti03/codes/github/LeoMotors
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

# Build + rename em um comando
bash ./build_debug_apk.sh

# Ver dispositivos conectados
adb devices -l
```

## Melhorias recentes
- Seleção de abastecimento com cards `Carro`/`Moto` com microanimação de estado.
- DatePicker nativo para datas de odômetro e abastecimento.
- Validação de domínio para impedir:
  - odômetro menor que o último registrado
  - data no futuro
  - litros/preço inválidos
- Timeout e proteção de estado para sync em nuvem (evita ficar preso em `Processando...`).
- Persistência migrada para Room com migração automática dos dados legados.
- Estado principal movido para `ViewModel`.
- Sync em nuvem com merge por entidade (veículos/odômetros/abastecimentos por `id`).

## Estrutura principal
```text
app/
  src/main/java/br/com/leo/leomotors/
    MainActivity.kt
    ui/
      LeoMotorsApp.kt
      LeoMotorsViewModel.kt
      BrandingUi.kt
      AccountSyncUi.kt
      TabsUi.kt
    cloud/
      CloudSyncService.kt
    data/
      Models.kt
      LocalStore.kt
      ReportCalculator.kt
      local/
        LeoMotorsDatabase.kt
        LeoMotorsDao.kt
        LocalEntities.kt
        LocalMappers.kt
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
- Para login/sync, configure `app/google-services.json`.
