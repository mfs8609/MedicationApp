# Guía de trabajo

## Objetivo
Crear MedicationApp como aplicación Android nativa en Java. Usar vistas y recursos XML; mantener una estructura sencilla y agregar dependencias solo cuando sean necesarias. No asumir requisitos funcionales a partir del nombre del proyecto.

## Entorno
- Windows y PowerShell. Ver el inventario comprobado en `docs/entorno-android.md`.
- SDK instalado en `$env:LOCALAPPDATA\Android\Sdk`.
- El `java` del PATH es Java 8. Antes de ejecutar Gradle, seleccionar explícitamente un JDK comprobado con `java` y `javac`.
- La extensión Red Hat Java incluye Java 21 con `javac`, pero carece de `jlink.exe`: no es suficiente para compilar este proyecto Android. Seleccionar un JDK 21 completo y comprobar `java`, `javac` y `jlink`. Revisar lo instalado antes de descargar herramientas.
- El usuario autorizó un JDK 21 portable dentro de `.tools` para SPEC-001. Preferir ese JDK local; consultar README para seleccionarlo. No versionar `.tools`.
- Gradle 9.2.0 y 9.4.1 están en la caché de distribuciones. No hay Gradle global ni Android command-line tools detectados.

Preparación con un JDK completo (solo afecta a la sesión actual):

```powershell
$env:JAVA_HOME = 'C:\ruta\al\jdk-21'
if (!(Test-Path "$env:JAVA_HOME\bin\javac.exe") -or !(Test-Path "$env:JAVA_HOME\bin\jlink.exe")) { throw 'Se requiere un JDK completo con javac y jlink' }
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
java -version
javac -version
jlink --version
```

## Al crear el proyecto
- Revisar el estado de Git y conservar cambios del usuario.
- Crear un módulo `app`, Manifest, fuentes Java y recursos XML. Documentar el identificador de aplicación y las decisiones de SDK en README.
- Consultar la compatibilidad oficial entre Android Gradle Plugin, Gradle y JDK antes de fijar versiones. La presencia en caché no demuestra compatibilidad entre todas las versiones.
- Preferir `compileSdk = 36`, disponible localmente, si es compatible con la configuración elegida. Definir `minSdk` y `targetSdk` según los requisitos y documentarlos.
- Incluir Gradle Wrapper completo (`gradlew`, `gradlew.bat`, JAR y properties); usarlo para todas las compilaciones posteriores. Fijar versiones, sin rangos dinámicos.
- Si se necesita `local.properties`, generarlo con `sdk.dir` local correctamente escapado. No versionarlo ni guardar rutas personales en archivos de configuración compartidos.

## Ciclo de trabajo
1. Leer el código relevante y aplicar el cambio mínimo que complete el requisito.
2. Una vez exista el proyecto, ejecutar desde su raíz:

   ```powershell
   .\gradlew.bat --version
   .\gradlew.bat :app:assembleDebug :app:lintDebug
   ```

3. Ejecutar `:app:testDebugUnitTest` cuando haya lógica o pruebas unitarias pertinentes. Para pruebas instrumentadas, comprobar primero un dispositivo conectado o un emulador listo.
4. Informar qué se verificó, qué quedó bloqueado y la ruta del APK generado; no afirmar una compilación exitosa sin comprobar el resultado.

APK debug esperado para un módulo sin variantes adicionales: `app/build/outputs/apk/debug/app-debug.apk`.

Instalación cuando se solicite probar en dispositivo:

```powershell
adb devices -l
adb -s <serial> install -r .\app\build\outputs\apk\debug\app-debug.apk
```

No hay AVD ni imágenes de sistema detectados. No asumir que el emulador instalado ya puede iniciar Android. Si faltan dependencias o hay restricciones de red/escritura, informar el error concreto; no modificar configuraciones globales como solución automática.

## Límites y entrega
- No versionar APK, salidas de compilación, claves, contraseñas ni configuración privada. Respetar `.gitignore`.
- Usar la firma debug para desarrollo. La firma release requiere una configuración explícita; no generar ni reemplazar claves de publicación por iniciativa propia.
- No incorporar datos personales de salud a fixtures o logs. Solicitar solo los permisos Android necesarios para las funciones implementadas.
- Mantener README con instrucciones reproducibles. Terminar cada tarea con un resumen breve del cambio, validación y pendientes reales.
