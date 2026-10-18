# Inventario local para Android en Java

Revisión realizada el 18 de octubre de 2026 en Windows, desde `D:\Development\MedicationApp`. No se instalaron herramientas ni se cambiaron variables persistentes.

## Resultado

Actualización durante la ejecución de SPEC-001: el entorno Java 21 de Red Hat ejecuta `javac` y Gradle, pero **no contiene `bin/jlink.exe`**. La tarea `:app:compileDebugJavaWithJavac` falla al transformar `core-for-system-modules.jar`. Por tanto, la comprobación inicial no demostraba que fuera un JDK completo apto para compilar este proyecto. Es necesario seleccionar un JDK completo antes de repetir la compilación; no se encontró otro en las ubicaciones revisadas.

Resolución autorizada por el usuario: se descargó Eclipse Temurin JDK `21.0.12.1+1` en `.tools/jdk-21.0.12.1+1`, verificando el SHA-256 del ZIP publicado por Adoptium. Con este JDK completo, AGP 9.2.0 y Gradle 9.4.1 se generó el APK debug, lint terminó sin errores y se verificó la firma. No se cambiaron variables persistentes. El inventario inicial que sigue se conserva como registro previo a esta incorporación; consultar README y SPEC-001 para el estado operativo actual.

Hay componentes instalados para desarrollar en Java, empaquetar y firmar APK de desarrollo. Se verificó la ejecución de Java/Javac 21, Gradle 9.4.1, ADB, AAPT2 y apksigner. Todavía no se comprobó una compilación Android completa: este repositorio contiene inicialmente solo README y `.gitignore`, sin módulo Android ni Gradle Wrapper.

## Componentes encontrados

| Componente | Evidencia local | Estado |
| --- | --- | --- |
| Java del PATH | Oracle Java 1.8.0_503, `C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe` | No usar como selección predeterminada para este flujo |
| JDK 21 | `%USERPROFILE%\.vscode\extensions\redhat.java-1.56.0-win32-x64\jre\21.0.12.1-win32-x86_64` | `java -version` y `javac -version` ejecutados correctamente |
| Otro runtime Java | Extensión `redhat.java-1.57.0-win32-x64`, carpeta `jre\25.0.4.1-win32-x86_64` | Localizado; no validado para compilar |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` | Instalado |
| Plataformas | `android-36`, `android-36.1` | Ambos `android.jar` presentes |
| Build Tools | `36.0.0`, `36.1.0`, `37.0.0` | En 36.0.0 se ejecutaron AAPT2 y apksigner; D8 y zipalign presentes |
| Platform Tools | ADB 1.0.41, paquete 37.0.0-14910828 | `adb version` correcto; no se comprobó conexión con un teléfono |
| Emulador | 36.5.11 | Ejecutable presente; lista de AVD vacía |
| Imágenes de sistema | Sin carpeta `system-images` | No detectadas |
| SDK command-line tools | Sin carpeta `cmdline-tools`; `sdkmanager` no encontrado en PATH | No detectadas en la ubicación estándar |
| Gradle | Distribuciones 9.2.0 y 9.4.1 bajo `%USERPROFILE%\.gradle\wrapper\dists` | 9.4.1 ejecutado con JDK 21; sin comando global |
| Android Gradle Plugin | Caché `com.android.tools.build\gradle\9.2.0` | Evidencia de descarga previa; no prueba una compilación exitosa |
| VS Code | Instalado; extensiones Red Hat Java, Java Extension Pack y Gradle for Java | Adecuado como editor para el flujo por terminal |
| Licencia SDK | Archivo `licenses\android-sdk-license` | Presente; no se verificaron licencias de paquetes adicionales |
| Firma debug | `%USERPROFILE%\.android\debug.keystore` | Archivo presente; no se leyó su contenido |

No se detectaron variables `JAVA_HOME`, `ANDROID_HOME` o `ANDROID_SDK_ROOT` en la sesión examinada. `javac`, `adb`, `gradle` y `sdkmanager` no se resolvieron por PATH. Android Studio no se localizó en las rutas habituales revisadas; eso no descarta instalaciones en otras ubicaciones.

Los logs de daemons Gradle registran ejecuciones anteriores con Java 21 y un JDK 17 dentro de una ruta `TestAndroid`. Esa ruta de proyecto no estuvo disponible en la comprobación actual; no se recuperó ni modificó el proyecto anterior.

## Lo pendiente para una aplicación

1. Crear el proyecto Android y su Wrapper, fijando una combinación compatible de AGP, Gradle y JDK según documentación oficial.
2. Preparar las variables de la sesión como indica `AGENTS.md` y resolver la ubicación del SDK.
3. Compilar y ejecutar lint; comprobar el APK resultante. Puede requerirse red para dependencias ausentes de la caché.
4. Para pruebas interactivas, conectar un teléfono con depuración USB o instalar una imagen de sistema y crear un AVD. La generación del APK no requiere un emulador.
5. Para distribución release, definir más adelante la firma y custodia de claves.

La ruta del JDK pertenece a una extensión y puede desaparecer tras una actualización. Para un entorno estable a largo plazo puede convenir un JDK independiente; no es una instalación realizada ni un requisito demostrado por esta revisión.
