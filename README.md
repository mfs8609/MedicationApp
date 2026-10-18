# MedicationApp

Base de aplicación Android nativa en Java y XML: una Activity con pantalla blanca, sin controles ni permisos. Identificador provisional: `com.example.medicationapp`; versión `0.1.0`.

## Herramientas

- Android SDK con plataforma 36 y Build Tools 36.0.0.
- JDK 21 **completo**, incluidos `javac` y `jlink`.
- Gradle Wrapper 9.4.1 y Android Gradle Plugin 9.2.0, fijados en el repositorio.
- Código Java compatible con Java 17, `minSdk 26`, `targetSdk 36`.

La combinación AGP/Gradle se comprobó en las [notas oficiales de AGP 9.2](https://developer.android.com/build/releases/agp-9-2-0-release-notes). Gradle admite ejecutar con Java 21 según su [matriz de compatibilidad](https://docs.gradle.org/current/userguide/compatibility.html).

## Preparación en PowerShell

Desde la raíz del proyecto, seleccionar el JDK 21 portable autorizado en `.tools` (o asignar `JAVA_HOME` a otro JDK 21 completo):

```powershell
$jdk = Get-ChildItem .tools -Directory -Filter 'jdk-21*' |
    Where-Object { Test-Path (Join-Path $_.FullName 'bin\jlink.exe') } |
    Select-Object -First 1
if (!$jdk) { throw 'Configurar JAVA_HOME con un JDK 21 completo' }
$env:JAVA_HOME = $jdk.FullName
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
if (!(Test-Path "$env:JAVA_HOME\bin\javac.exe") -or !(Test-Path "$env:JAVA_HOME\bin\jlink.exe")) {
    throw 'Se requiere un JDK completo con javac y jlink'
}
java -version
javac -version
jlink --version
```

Estas variables afectan solo a la sesión. Alternativamente, el SDK puede indicarse en `local.properties` con `sdk.dir=C\:/ruta/Android/Sdk`; ese archivo no se versiona. La distribución Gradle tiene checksum SHA-256 fijado en `gradle/wrapper/gradle-wrapper.properties`.

`.tools` es local y no se versiona. En otra máquina hay que proporcionar un JDK completo; el Java incluido en la extensión de VS Code revisada carece de `jlink`.

## Compilación y validación

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:lintDebug --console=plain
```

Gradle puede necesitar acceso a internet para resolver dependencias. No se requiere Android Studio ni Gradle global.

APK esperado: `app/build/outputs/apk/debug/app-debug.apk`.

Informe lint: `app/build/reports/lint-results-debug.html`.

Verificación de firma:

```powershell
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs .\app\build\outputs\apk\debug\app-debug.apk
```

Para instalar cuando se solicite usar un dispositivo de prueba:

```powershell
adb devices -l
adb -s <serial> install -r .\app\build\outputs\apk\debug\app-debug.apk
adb -s <serial> shell am start -n com.example.medicationapp/.MainActivity
```

Verificar apertura, pantalla blanca en modo claro y oscuro, rotación y legibilidad de las barras del sistema. No se han configurado emuladores como parte de este proyecto.

## Documentación

- [Guía para agentes](AGENTS.md).
- [Inventario local](docs/entorno-android.md).
- [SPEC-001 y estado de ejecución](docs/spec-001-aplicacion-en-blanco.md).

Los APK, cachés, configuración local y claves de firma se excluyen de Git. No hay configuración de firma release ni dependencias de aplicación.

Validación de SPEC-001: compilación correcta, firma debug verificada y lint con 0 errores y 5 advertencias revisadas en la especificación. Prueba en dispositivo pendiente.
