# MedicationApp

Aplicación Android nativa en Java y XML para gestionar medicamentos y programaciones locales de toma. Permite crear, editar, desactivar y reactivar medicamentos con recurrencias diarias, semanales o cada N días. Identificador provisional: `com.example.medicationapp`; versión `0.1.0`.

Las próximas tomas son **programadas**: no hay alarmas, notificaciones ni registro de dosis realizadas.

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
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain
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

Verificar listado vacío, alta y edición de dos medicamentos ficticios, rotación del formulario sin perder cambios, confirmación al descartar, horarios y activación. No se han configurado emuladores como parte de este proyecto.

Con un dispositivo/emulador de prueba conectado y autorizado, ejecutar las pruebas de persistencia:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Estas pruebas usan una base separada `spec002-test.db` y la eliminan al finalizar. Comprueban nombres repetidos, reapertura de la base, edición, activación, rollback de una transacción fallida y restricciones de unicidad/claves foráneas. No usar datos personales como datos de prueba.

## Persistencia y programación

- Room `2.8.4` con `annotationProcessor` Java, sin plugin adicional de Room; la compatibilidad con el proyecto se comprobó compilando. [Configuración oficial de Room](https://developer.android.com/jetpack/androidx/releases/room).
- SQLite privada `medications.db`, esquema versión 1 exportado en `app/schemas`. Un medicamento tiene una programación y múltiples horarios, con claves foráneas e índices únicos.
- Las fechas se convierten explícitamente a texto ISO `AAAA-MM-DD`; la fecha final opcional es `NULL`. Horarios locales `HH:mm`, sin convertirlos a UTC al almacenar. Días ISO separados por comas (lunes=1, domingo=7), tipo de recurrencia por nombre del enum e intervalo entero. Las cantidades se guardan como texto decimal canónico para conservar precisión; instantes de auditoría en milisegundos Unix.
- `PlansDao.save` valida y guarda medicamento, programación y horarios en una transacción. El acceso a Room se ejecuta en un executor de un hilo, sin `allowMainThreadQueries` ni migraciones destructivas. Los cambios futuros de esquema requerirán una migración explícita.
- `NextOccurrence` recibe instante y zona horaria como parámetros. Calcula la primera ocurrencia estrictamente futura, dentro de fechas inclusivas; cada N días se ancla al inicio en días calendario. En cambios de horario estacional, las horas inexistentes avanzan por el salto y las ambiguas consideran ambos offsets. Las horas se muestran con el formato regional del dispositivo.
- El listado se actualiza al volver a la pantalla y cada 30 segundos mientras está visible. Los inactivos conservan su programación y no muestran próxima toma.
- El formulario conserva el borrador al recrearse y una operación de guardado en curso se conserva durante cambios de configuración, sin bloquear la orientación. Los controles se deshabilitan mientras se guarda.

## Datos y copias de seguridad

No se declara permiso de internet. Se mantienen `allowBackup=false` y `fullBackupContent=false`; las reglas de extracción excluyen todos los dominios de datos de la aplicación tanto de nube como de transferencia entre dispositivos. El autocompletado de los campos del formulario está deshabilitado. No se registran datos de medicamentos en logs.

Los datos no están cifrados por la aplicación: dependen del almacenamiento privado y la protección del sistema Android. Borrar datos o desinstalar elimina la base local; esta entrega no implementa recuperación ni exportación.

## Documentación

- [Guía para agentes](AGENTS.md).
- [Inventario local](docs/entorno-android.md).
- [SPEC-001 y estado de ejecución](docs/spec-001-aplicacion-en-blanco.md).
- [SPEC-002 y verificaciones](docs/spec-002-gestion-medicamentos.md).

Los APK, cachés, configuración local y claves de firma se excluyen de Git. No hay configuración de firma release.

Validación de SPEC-002: compilación correcta, 17 pruebas unitarias aprobadas y APK de seis pruebas instrumentadas compilado. Lint: 0 errores y 6 advertencias de versiones disponibles e icono base, revisadas en la especificación. Pruebas instrumentadas y comprobación visual pendientes: ADB no detectó dispositivos conectados.
