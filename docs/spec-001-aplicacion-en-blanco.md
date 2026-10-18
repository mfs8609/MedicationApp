# SPEC-001: Aplicación Android en blanco

Estado: implementación y validaciones de compilación completadas; prueba en dispositivo pendiente.

Fecha: 18 de octubre de 2026.

## Objetivo

Crear la base mínima de MedicationApp como aplicación Android nativa en Java, compilable desde PowerShell y VS Code, y generar un APK debug. Esta primera entrega permitirá comprobar el flujo de desarrollo antes de incorporar funciones.

El usuario solicitó ejecutar esta especificación después de su revisión.

## Resultado visible

- La aplicación aparece en el lanzador con el nombre `MedicationApp` y un icono local sencillo.
- Al abrirla, muestra una única pantalla con fondo blanco y sin contenido: sin texto, botones, menús ni navegación interna.
- El fondo permanece blanco aunque el dispositivo use modo oscuro. Las barras del sistema mantienen iconos legibles.
- La pantalla puede rotar y recrearse sin errores. No hay estado de usuario que conservar.
- El inicio puede mostrar la pantalla de arranque estándar de Android con el icono de la aplicación. No se agrega una pantalla de bienvenida propia.

## Alcance técnico propuesto

| Decisión | Propuesta |
| --- | --- |
| Nombre del proyecto y aplicación | `MedicationApp` |
| Módulos | Un módulo `app` |
| Lenguaje de la aplicación | Java |
| Interfaz | Una `MainActivity` y un layout XML vacío |
| Activity base | `android.app.Activity`, sin bibliotecas de interfaz adicionales |
| Namespace y applicationId | `com.example.medicationapp`, identificador provisional para desarrollo |
| SDK mínimo | API 26, decisión inicial revisable según los dispositivos que se quieran soportar |
| SDK de compilación | API 36, instalado según el inventario local |
| SDK objetivo | API 36 |
| Versión inicial | `versionCode = 1`, `versionName = "0.1.0"` |
| JDK de compilación | JDK 21 ya identificado; comprobar nuevamente su ruta al ejecutar |
| Compatibilidad del código Java | Java 17 mediante `sourceCompatibility` y `targetCompatibility` |
| Scripts de compilación | Gradle Groovy: `settings.gradle`, `build.gradle` y `app/build.gradle` |
| Distribución inicial | APK debug firmado con la clave de desarrollo |

El identificador provisional no representa un dominio propio ni una identidad definitiva de publicación. Debe revisarse antes de distribuir la aplicación como producto.

Las versiones exactas de Android Gradle Plugin y Gradle se fijarán al ejecutar, después de consultar su compatibilidad oficial con JDK 21 y API 36. Se evaluarán primero las versiones instaladas descritas en [el inventario](entorno-android.md), sin asumir compatibilidad por estar en caché. Si la combinación exige cambiar las decisiones de esta tabla, se documentará el motivo antes de continuar con ese cambio de alcance.

## Archivos esperados

```text
MedicationApp/
  AGENTS.md
  README.md
  .gitignore
  settings.gradle
  build.gradle
  gradle.properties
  gradlew
  gradlew.bat
  gradle/wrapper/
    gradle-wrapper.jar
    gradle-wrapper.properties
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
      java/com/example/medicationapp/MainActivity.java
      res/
        layout/activity_main.xml
        values/strings.xml
        values/colors.xml
        values/styles.xml
        drawable/...
        mipmap-anydpi-v26/...
  docs/
    entorno-android.md
    spec-001-aplicacion-en-blanco.md
```

Las carpetas de iconos podrán ajustarse a los recursos efectivamente utilizados. Los iconos se crearán con recursos vectoriales/XML sencillos, sin servicios externos.

`local.properties`, si se necesita, será un archivo local excluido de Git. Las salidas `build/` y el APK también permanecerán fuera del control de versiones.

## Requisitos de implementación

1. Seguir [AGENTS.md](../AGENTS.md), conservar los archivos y cambios existentes y trabajar en la raíz del repositorio actual.
2. Incluir un Gradle Wrapper completo con versiones fijas y checksum de distribución. Las compilaciones habituales deben usar `gradlew.bat`.
3. Configurar únicamente los repositorios necesarios para resolver el plugin y las dependencias. No agregar dependencias de aplicación para esta pantalla vacía.
4. Declarar una sola Activity de entrada con los filtros de lanzador y la configuración de exportación necesaria. No declarar permisos Android, servicios, proveedores ni receptores adicionales.
5. Usar recursos XML para nombre, colores, tema, icono y layout. No agregar barra de título ni barra de acciones a la pantalla vacía.
6. No incorporar Kotlin, Compose, NDK, bases de datos, acceso a red, analítica ni funciones relacionadas con medicamentos.
7. Mantener la configuración específica del equipo fuera de los archivos compartidos y no cambiar variables de entorno persistentes.
8. Actualizar README con los requisitos, preparación de la sesión, compilación, validación y ubicación del APK.

## Secuencia de ejecución futura

1. Revisar Git, instrucciones y disponibilidad del SDK/JDK; verificar la compatibilidad oficial de las versiones elegidas.
2. Crear el proyecto, recursos y Gradle Wrapper; registrar las versiones concretas en los archivos de compilación y README.
3. Configurar el SDK local y ejecutar:

   ```powershell
   .\gradlew.bat --version
   .\gradlew.bat :app:assembleDebug :app:lintDebug
   ```

4. Comprobar que el APK existe en `app/build/outputs/apk/debug/app-debug.apk` y verificar su firma debug con `apksigner verify`.
5. Si hay un dispositivo de prueba disponible y se solicita su uso, instalar el APK, abrir la Activity y comprobar el resultado visible. No instalar ni crear un emulador como parte de esta entrega.
6. Entregar el resumen de cambios, versiones utilizadas, resultados de validación y ruta del APK. Registrar cualquier validación pendiente.

Las descargas que necesite Gradle se resolverán durante la ejecución, sujetas al acceso disponible. Una dependencia no disponible debe reportarse como bloqueo concreto; no como una compilación verificada.

## Criterios de aceptación

- [x] Existe un proyecto Android de un módulo con fuentes Java e interfaz XML, según las decisiones aprobadas.
- [x] El Wrapper completo permite ejecutar Gradle con el JDK seleccionado sin depender de Gradle global.
- [x] `:app:assembleDebug` termina correctamente.
- [x] `:app:lintDebug` termina sin errores; cualquier advertencia queda revisada y documentada, sin supresiones generales para ocultarla.
- [x] Existe un APK debug con firma verificada en la ruta documentada.
- [x] El Manifest identifica `MedicationApp`, contiene la Activity de entrada y no solicita permisos.
- [x] README permite repetir la compilación desde PowerShell.
- [x] No se incluyen claves privadas, rutas personales, APK ni salidas de compilación en los archivos que se proponen versionar.
- [x] Si se realiza la prueba en dispositivo, se confirma apertura sin cierre inesperado, pantalla blanca sin controles, rotación y legibilidad de las barras del sistema. Si no se realiza, se declara expresamente como pendiente y no se afirma validación visual.

No se crearán pruebas unitarias para una pantalla sin lógica. Las pruebas instrumentadas y la matriz de dispositivos se incorporarán cuando haya comportamiento que justifique su mantenimiento.

## Fuera de alcance

- Gestión de medicamentos, recordatorios, notificaciones y datos de pacientes.
- Persistencia, autenticación, API o servicios externos.
- Diseño de marca o navegación de la aplicación definitiva.
- Instalación de Android Studio, JDK, SDK adicionales o imágenes de emulador.
- APK release, claves de publicación, App Bundle o publicación en tiendas.
- Cambios globales del equipo, commits o publicación del repositorio.

## Revisión y autorización

El usuario autorizó la ejecución mediante «ejecuta el spec». Durante la compilación se descubrió que Java 21 de VS Code carece de `jlink.exe`. El usuario autorizó expresamente descargar un JDK 21 completo en `.tools`, sin modificar la configuración global. Esta autorización amplía exclusivamente la exclusión de instalar un JDK indicada anteriormente.

## Registro de ejecución

- Android Gradle Plugin fijado en `9.2.0`, Gradle Wrapper en `9.4.1` con checksum SHA-256 oficial y Build Tools en `36.0.0`.
- Compatibilidad consultada en las [notas oficiales de AGP](https://developer.android.com/build/releases/agp-9-2-0-release-notes) y la [matriz de Gradle](https://docs.gradle.org/current/userguide/compatibility.html).
- Proyecto creado sin dependencias de aplicación, con una Activity Java y layout XML vacío.
- Primer intento: Wrapper correcto; compilación detenida por ausencia de `jlink.exe` en Java de VS Code. Se corrigió el inventario y se autorizó usar un JDK portable local.
- Prueba en dispositivo no ejecutada: pendiente de disponer de un dispositivo y de solicitar su uso. No se afirma validación visual, de rotación ni de ejecución real.
- JDK portable usado: Eclipse Temurin `21.0.12.1+1`, descargado desde la API oficial de Adoptium en `.tools/jdk-21.0.12.1+1`. Checksum del ZIP verificado: `f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e`.
- `gradlew.bat --version`: Gradle 9.4.1 ejecutado con ese JDK 21.
- `:app:assembleDebug :app:lintDebug`: `BUILD SUCCESSFUL`. Lint: **0 errores, 5 advertencias**, sin baseline ni supresiones.
- `apksigner verify --verbose`: correcto, firma v2 con certificado Android Debug.
- Inspección del APK con AAPT2: identificador `com.example.medicationapp`, versión `0.1.0`, minSdk 26, targetSdk 36, Activity de lanzador correcta y ningún permiso solicitado.
- APK final: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256: `139b0a9bd82decd81d6450440e45ddd6322b414ee99cd0dd48aec1a55825c5c4`.
- `git diff --check` sin incidencias. `.tools`, `local.properties` y APK comprobados como ignorados por Git. No se crearon commits.

### Advertencias de lint revisadas

| Advertencia | Resolución para esta entrega |
| --- | --- |
| `OldTargetApi` | Mantener targetSdk 36, según el alcance aprobado; no declarar pruebas en versiones superiores. |
| `AndroidGradlePluginVersion` | Mantener Gradle 9.4.1, compatible con AGP 9.2.0; no actualizar automáticamente por disponibilidad de otra versión. |
| `GradleDependency` | Mantener compileSdk 36, instalado y previsto en la spec. |
| `DataExtractionRules` | Se conserva `allowBackup=false` y no hay persistencia de datos. Definir reglas explícitas de extracción y transferencia al incorporar almacenamiento. |
| `MonochromeLauncherIcon` | La variante base no declara monocromo; la variante `mipmap-anydpi-v33` sí lo incluye para las versiones que lo soportan. Revisar su apariencia al probar en dispositivo. |

Los criterios marcados relativos a validación visual indican que su ausencia se ha declarado expresamente; no equivalen a una prueba ejecutada en Android.
