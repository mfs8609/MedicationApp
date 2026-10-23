# SPEC-003: Rediseño UX/UI de MedicationApp

**Estado:** propuesta lista para ejecución por Codex, pendiente de implementación y validación.
**Fecha:** 22 de octubre de 2026.
**Dependencia:** SPEC-002 implementada en `main`.
**Tipo:** mejora de experiencia de usuario e interfaz; sin nuevas funciones clínicas.

## 1. Objetivo

Rediseñar MedicationApp con una interfaz Android amigable, moderna y clara, inspirada en el mockup de tres pantallas generado en la conversación: (1) inicio con tarjetas de medicamentos, (2) formulario «Nuevo medicamento», (3) detalle de un medicamento. El mockup es una **referencia de dirección visual**, no un contrato píxel a píxel. **Este documento contiene todos los requisitos necesarios para implementarlo sin disponer de la imagen.**

Mantener Java + vistas XML, las funcionalidades de SPEC-002 y el almacenamiento Room/SQLite existentes. Optimizar legibilidad, jerarquía, prevención de errores, navegación y accesibilidad.

**Límite esencial:** el mockup es aspiracional y muestra acciones «Confirmar toma», «Tomado», «Historial», «Racha» y «tomas pendientes» que NO están implementadas. No simularlas, habilitarlas ni inventar datos de adherencia. La UI de SPEC-003 debe representar únicamente programación de tomas, sin confundir lo programado con lo realmente tomado. Esas funciones serán objeto de otra SPEC.

## 2. Repositorio y puntos de integración

Antes de editar, leer `AGENTS.md`, `docs/spec-002-gestion-medicamentos.md` y el código actual. Puntos conocidos en `main`:
- `app/src/main/java/com/example/medicationapp/MainActivity.java`: listado con filtro activos/inactivos; usa `MedicationDatabase.IO` y `NextOccurrence`.
- `app/src/main/java/com/example/medicationapp/EditorActivity.java`: alta/edición, validación, estado de formulario, confirmación de descarte, fechas, horas y recurrencias.
- `app/src/main/java/com/example/medicationapp/UiSupport.java`: helpers de interfaz.
- `app/src/main/res/layout/activity_main.xml`, `activity_editor.xml`, `medication_row.xml`, `time_row.xml`.
- `app/src/main/java/com/example/medicationapp/data/MedicationDatabase.java`: Room con esquema v1.
- `domain/MedicationPlan.java`, `PlanValidator.java`, `NextOccurrence.java`.
- Pruebas actuales en `app/src/test` y `app/src/androidTest`.

Reusar lógica y navegación existentes donde sea posible. La nueva Activity de detalle puede abrir el editor con el mismo identificador. No reescribir la capa de datos para producir un cambio visual.

## 3. Sistema de diseño

### 3.1 Identidad visual

Apariencia luminosa y serena, con referencias Material 3:
- Fondo general: azul grisáceo muy pálido (referencia #F5F9FC).
- Superficies: blanco (#FFFFFF).
- Primario: azul petróleo / teal (#087A9B), con variante más oscura para botones y texto sobre claro.
- Texto principal: azul marino oscuro (#102C44).
- Texto secundario: gris azulado (#587084).
- Éxito: verde discreto, **solo para estados verificables** (p. ej. «Activo»; nunca «Tomado» sin registros).
- Advertencia: ámbar para errores recuperables, no para «dosis pendiente» inferidas.
- Peligro: rojo para «Desactivar» únicamente cuando aparezca una confirmación.
- Radios orientativos: 16–20 dp en tarjetas, 12–16 dp en campos/botones, chips redondeados.
- Espaciado basado en múltiplos de 4 dp, típicamente 8 / 12 / 16 / 24 dp; separación suficiente para lectura.
- Iconografía vectorial local coherente (cápsula/píldora, reloj, calendario, edición, información); evitar emojis como iconos esenciales.
- Tipografía Android legible, escala con preferencias del sistema; título de pantalla 24–28 sp, títulos de tarjetas 18–20 sp, cuerpo 14–16 sp como orientación, no tamaños fijos cuando causen recortes.
- Respetar contraste WCAG AA para textos y controles, estados de foco/pressed/disabled visibles, áreas táctiles al menos 48 × 48 dp.

Evitar gradientes recargados, sombras fuertes, ilustraciones irrelevantes y elementos falsamente interactivos. No añadir avatares ni cuentas de usuario: la aplicación es offline y sin autenticación.

### 3.2 Comportamiento Android

Mantener soporte desde minSdk actual (API 26) y target SDK del proyecto, barras del sistema legibles e insets correctamente aplicados. Usar dimensiones escalables dp/sp, ScrollView en formularios, adaptación a teclados y pantallas pequeñas, modo vertical y rotación. Soportar tema oscuro **si se implementa completamente**; de lo contrario conservar contraste correcto en la presentación clara independientemente del tema del sistema y documentar la decisión.

Se permite introducir Material Components para vistas XML **solo si** la compatibilidad de la versión elegida con el proyecto, AGP, Java y minSdk está comprobada y se justifica en README; de lo contrario usar recursos Android XML y drawables locales. No incorporar Compose, Kotlin, Flutter ni librerías de UI sin propósito.

## 4. Pantalla A — Inicio «Tus medicamentos»

### RF-301 — Encabezado amigable

Mostrar «Tus medicamentos» como título claro. Opcionalmente usar saludo contextual «Buenos días / tardes / noches», calculado en el dispositivo, sin crear perfil ficticio ni suponer el nombre del usuario.

Debajo, incluir una tarjeta de resumen **derivada únicamente de medicamentos activos y próximas ocurrencias reales del plan**:
- Ejemplo cuando existan próximas ocurrencias hoy: «3 tomas programadas para hoy».
- Si hay medicamentos, pero ninguna ocurrencia hoy: «No tienes tomas programadas para hoy».
- Si no hay medicamentos activos: texto claro y acción para agregar.
- No decir «pendientes», «tomadas», «completadas», ni «vas muy bien» porque todavía no existe seguimiento de administración.
- Contar ocurrencias del día, **no medicamentos**: un mismo medicamento puede tener dos horarios hoy.
- Evitar depender solo de `NextOccurrence.next()` por medicamento al calcular el total del día: contemplar todas las horas programadas de cada medicamento dentro del día local, incluidas las anteriores al momento presente. Introducir helper de solo lectura y testeable si resulta necesario, sin crear registros de toma.

### RF-302 — Tarjetas de medicamento

Cada medicamento activo se representa con tarjeta separada:
- Icono circular neutral de píldora.
- Nombre destacado; concentración opcional.
- Resumen «1 tableta por toma» (o cantidad y unidad tal como se guardan).
- Texto de frecuencia y horarios.
- Bloque «Próxima toma programada» con fecha/hora local cuando exista; si no, explicación legible «Sin próximas tomas programadas».
- Una acción explícita «Ver detalles» o tarjeta táctil bien etiquetada que abra detalle.
- Opcional menú contextual con «Editar» y «Desactivar» con confirmación para la segunda acción.
- No incluir botones «Confirmar toma», ni chips «Tomado»/«Pendiente».

Orden sugerido: primero próximas ocurrencias crecientes, luego medicamentos sin próxima ocurrencia. Mantener orden estable ante empates (nombre + identificador). No perder acceso a medicamentos inactivos.

### RF-303 — Alta y navegación

Mostrar botón flotante circular con «+» y descripción accesible «Agregar medicamento». No superponer el botón sobre contenido o acciones. Si se adopta navegación inferior, mostrar **solo destinos funcionales**: por ejemplo «Medicamentos» y «Inactivos»; no crear pestañas «Calendario», «Historial» ni «Más» sin funcionalidad. Es aceptable mantener un botón/filtro de inactivos en el encabezado en lugar de navegación inferior: preferir la solución más simple y clara.

### RF-304 — Estados

Distinguir visualmente:
- Cargando: indicador sobrio, sin falsear contenido.
- Sin datos: icono, explicación y CTA «Agregar medicamento».
- Error de carga: explicación y «Reintentar».
- Medicamentos inactivos: lista separada/filtro con chip «Inactivo», acción «Reactivar».
- Listado normal: número y contenido reales.

Mantener refresco correcto al regresar del editor/detalle, y después de activar/desactivar.

## 5. Pantalla B — Formulario «Nuevo medicamento» / «Editar medicamento»

### RF-305 — Agrupación progresiva

Formulario de una sola pantalla desplazable, agrupado en secciones:
1. **Medicamento:** nombre, concentración.
2. **Dosis:** cantidad positiva editable y unidad (texto o selector que no impida unidades personalizadas).
3. **Frecuencia:** diario, días específicos de semana, cada N días.
4. **Fechas:** inicio y fin opcional.
5. **Horarios:** filas editables y botón «Agregar otro horario».
6. **Notas** opcionales.
7. Acción primaria «Guardar medicamento» o «Guardar cambios».

Campo de concentración con ejemplo «50 mg», sin interpretar automáticamente dosis. Mantener edición decimal de cantidad y conservar valores fraccionarios (p. ej., media tableta); si se usa control ±, permitir entrada directa y evitar asumir incrementos enteros. No modificar cantidades basándose en consejos médicos.

### RF-306 — Controles intuitivos

- Selección de fechas mediante DatePicker con representación comprensible; mantener valores ISO en el modelo. Permitir borrar fecha final.
- Selección de horario mediante TimePickerDialog o equivalente; mostrar hora respetando sistema 12/24 horas, sin alterar la representación `HH:mm` almacenada.
- Para WEEKLY, chips accesibles «L M X J V S D» con estado marcado, selección múltiple y nombre de día completo en descripción para accesibilidad. Mostrar únicamente cuando aplique.
- Para EVERY_N_DAYS, mostrar campo «Cada [N] días» únicamente cuando aplique.
- Los horarios deben poder añadirse/eliminarse con confirmación/validación adecuada cuando quede el último; seguir exigiendo al menos uno al guardar.
- Mensajes auxiliares breves: «Puedes dejar la fecha final vacía si tu tratamiento no tiene una fecha de finalización definida».
- Errores específicos al lado de cada campo; llevar foco/scroll al primer error.
- Botón guardar bien visible sin ocultar el último campo al abrir teclado o ampliar fuente; si se usa CTA anclada, respetar insets y espacio de desplazamiento.
- La opción «Activo» en edición se mantiene, pero presentarla como control explícito y comprensible; en creación se asume activo inicialmente.

### RF-307 — Protección contra pérdida de datos

Preservar el formulario ante rotación, cambio de configuración y retorno desde diálogos. Pedir confirmación al salir con cambios no guardados. Evitar dobles guardados; deshabilitar acción y mostrar progreso durante persistencia. Si falla el guardado, conservar campos y permitir reintento. No hacer queries Room en el hilo principal.

## 6. Pantalla C — Detalle de medicamento

### RF-308 — Vista de detalle real

Añadir una vista de solo lectura para un medicamento existente, navegable desde inicio:
- Cabecera con nombre y concentración; chip «Activo» o «Inactivo».
- Resumen de cantidad + unidad por toma.
- Tarjeta «Próxima toma programada» (o mensaje sin siguiente).
- Tarjeta «Horario habitual»: recurrencia, días configurados, todas las horas ordenadas, fechas de vigencia.
- Tarjeta «Notas», solo si existen; alternativa «Sin notas».
- Acciones «Editar» y «Desactivar» / «Reactivar»; pedir confirmación explícita antes de desactivar.
- Navegación atrás consistente al listado; mostrar error o estado recuperable si el elemento deja de existir.

### RF-309 — No simular historial/adherencia

No renderizar la «racha de 6 días», marcas verdes por día, registros «Tomado hoy», «Historial», progreso ni pestañas inoperantes que aparecen en el mockup. No crear historial inventado ni tabla de tomas para maquillar la interfaz. Puede incluirse una nota discreta: «El registro de tomas estará disponible en una versión posterior», **solo si aporta claridad**, sin CTA inerte.

## 7. Arquitectura y restricciones

- Preservar Room v1 y la información ya guardada; **evitar cambios de esquema**. Si algún cambio de esquema es inevitable, justificar y aplicar migración explícita con prueba, jamás destructiva.
- No cambiar semántica de `MedicationPlan`, `PlanValidator` y `NextOccurrence`; reutilizarlos.
- No agregar Internet, notificaciones, alarmas, permisos de salud ni rastreo.
- No introducir una arquitectura multiagente, DI framework ni reescritura completa.
- Separar construcción de datos de pantalla y presentación de manera suficiente para probar los cálculos y mantener Activities legibles.
- No introducir strings de UI repetidas en Java, ni texto de ejemplo personal en fixtures/logs.
- Preferir componentes reusables XML (tarjeta de medicamento, fila horario, chips) y drawable/vector resources.
- Gestionar botones rápidos para desactivar/reactivar mediante los DAO existentes, con feedback de éxito o error coherente.
- Conservar política actual de backup/privacidad y acceso local.

## 8. Plan de ejecución para Codex

1. Leer `AGENTS.md`, la SPEC-002, README, activities, XML y pruebas; registrar estado de Git sin sobrescribir trabajo ajeno.
2. Implementar recursos visuales: colores, dimens, temas, shapes, iconos vectoriales, strings.
3. Rediseñar inicio (estado vacío/carga/error, tarjetas y resumen de tomas **programadas**), conservando filtro inactivos.
4. Rediseñar editor con selectores de fecha/hora y días, notas, validaciones y estados existentes.
5. Implementar vista de detalle con navegación real y acciones Editar/Desactivar/Reactivar.
6. Añadir pruebas de helpers de UI/lógica calculable y ajustar pruebas existentes sin reducir cobertura.
7. Compilar, ejecutar lint/tests, realizar revisión manual en dispositivo/emulador si está disponible; documentar limitaciones.
8. Actualizar README y al final de este archivo incorporar «Registro de ejecución»: cambios, comandos, resultados, pendientes, capturas **solo si fueron obtenidas realmente**.

No detenerse solo por no tener acceso a la imagen del mockup: este documento define textos, estados, navegación y jerarquía visual para implementar una interfaz equivalente.

## 9. Pruebas y criterios de aceptación

### Pruebas unitarias y de regresión

- [ ] SPEC-002 sigue pasando: validación, recurrencias y cálculo de próxima ocurrencia.
- [ ] Conteo de tomas **programadas para hoy** correcto con múltiples horarios, días no programados, inactivos, fechas inicio/fin y cruce de día local.
- [ ] Próxima toma y orden de tarjetas correctos; casos sin próxima toma.
- [ ] Crear/editar con cantidades decimales, unidad personalizada, horarios múltiples, recurrencias y fecha final vacía.
- [ ] Confirmación de descarte y protección contra doble guardado se preservan.
- [ ] Desactivar/reactivar conserva datos.
- [ ] No hay consultas Room en hilo UI.

### Verificación visual y funcional (dispositivo/emulador, si disponible)

- [ ] Inicio legible con 0, 1 y varios medicamentos; todos los estados de carga/error.
- [ ] Formulario usable con teclado abierto, pantalla pequeña, tamaño de letra aumentado y rotación.
- [ ] Navegación Inicio → Detalle → Editar → Detalle/Inicio funcional.
- [ ] Controles táctiles alcanzables, foco y TalkBack con rótulos semánticos.
- [ ] Recursos traducibles y contraste adecuado de textos, estados, chips y botones.
- [ ] Ningún botón falso, pantalla vacía decorativa, confirmación de toma, historial o racha ficticia.
- [ ] Datos existentes permanecen intactos tras instalar la actualización.

### Comandos mínimos

Desde PowerShell y con el JDK completo indicado en `AGENTS.md`:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain
adb devices -l
```

Ejecutar `:app:connectedDebugAndroidTest` solo si hay dispositivo/emulador listo. No confundir compilación de tests instrumentados con ejecución de estos. Declarar advertencias de lint y bloqueos reales sin falsear verificaciones.

## 10. Entregables

- Pantallas XML y código Java modificados/añadidos.
- Recursos de tema, drawables, dimensiones, iconos y cadenas en español.
- Pantalla de detalle funcional.
- Pruebas nuevas/actualizadas.
- README y registro de ejecución con resultados reales.
- APK debug compilado localmente si el entorno lo permite; no versionar binarios.

**Definición de terminado:** la aplicación tiene un diseño coherente con las tres pantallas del mockup en cuanto a jerarquía, composición, color y facilidad de uso; todas las acciones mostradas funcionan y se nutren de datos reales disponibles en SPEC-002, sin fingir seguimiento de tomas todavía.
