# SPEC-002: Gestión de medicamentos y programación de tomas

Estado: propuesta para revisión; implementación pendiente.

Fecha: 18 de octubre de 2026.

## Objetivo

Agregar a MedicationApp la administración local de múltiples medicamentos y sus programaciones de toma. El usuario podrá crear, consultar, editar, desactivar y reactivar medicamentos, con horarios y frecuencias independientes. Los datos persistirán en SQLite mediante Room, sin requerir conexión a internet.

Esta especificación **no implementa avisos ni registra si una dosis fue tomada**. Una toma *programada* nunca debe presentarse como una toma *realizada*.

## Resultado visible

- La pantalla principal muestra los medicamentos activos registrados con nombre, concentración (si existe), cantidad por toma, frecuencia resumida y próxima toma programada.
- Se puede acceder a medicamentos inactivos y reactivarlos.
- Se puede agregar un medicamento sin recurrir a un catálogo externo.
- Un formulario permite editar información, recurrencia, fecha de inicio y fin, y uno o varios horarios.
- Cuando no hay medicamentos, aparece un estado vacío con acción para agregar el primero.
- Cuando no hay próxima toma futura, se muestra un mensaje explicativo, no una fecha ficticia.
- Las entradas y mensajes de validación se presentan en español.

## Alcance

### Incluido

1. Listar medicamentos activos e inactivos.
2. Crear medicamentos con información básica y programación.
3. Editar medicamentos y sus programaciones.
4. Desactivar y reactivar medicamentos sin perder sus datos.
5. Guardar y consultar todo localmente con Room/SQLite.
6. Calcular la próxima ocurrencia programada.
7. Validaciones de formulario y pruebas de lógica/persistencia.

### Excluido

- Alarmas, notificaciones y permisos relacionados.
- Confirmación, omisión o historial de tomas efectivas.
- Recomendaciones médicas, cálculo o modificación automática de dosis.
- Sincronización, autenticación, servicios remotos y catálogos de medicamentos.
- Eliminación permanente de medicamentos.
- Intervalos contados desde la última dosis y tomas «según necesidad».
- Exportación/importación y copias de seguridad implementadas desde la aplicación.

## Requisitos funcionales

### RF-001 — Listado

La pantalla principal presenta los medicamentos activos. Cada elemento muestra:
- Nombre y concentración opcional.
- Cantidad y unidad por toma.
- Frecuencia y horarios en una descripción legible.
- Próxima toma planificada calculada con la hora del dispositivo.

Debe existir una acción para consultar medicamentos inactivos. El listado vacío debe ser comprensible y ofrecer «Agregar medicamento».

### RF-002 — Creación

Registrar:
- Nombre obligatorio.
- Concentración opcional como texto (por ejemplo, `50 mg`).
- Cantidad numérica positiva por toma.
- Unidad no vacía (por ejemplo, `tableta`).
- Notas opcionales.
- Estado activo inicial.
- Fechas de creación y última modificación.
- Una programación con fecha de inicio, fecha de fin opcional, recurrencia y uno o más horarios.

No usar el nombre como clave única: pueden existir dos medicamentos con el mismo nombre.

### RF-003 — Recurrencia

Admitir estos tipos:
- **DAILY:** todos los días.
- **WEEKLY:** uno o varios días de la semana elegidos por el usuario.
- **EVERY_N_DAYS:** cada N días calendario a partir de la fecha de inicio, con N entero positivo.

Cada programación contiene una o varias horas locales; varias tomas en un día se configuran agregando horas. Almacenar la hora como hora local sin zona UTC implícita; mostrarla según la configuración regional. Rechazar horas duplicadas en una misma programación.

Una programación empieza en `startDate`, inclusive, y termina en `endDate`, inclusive, cuando exista. Para DAILY y WEEKLY se consideran únicamente las fechas dentro del intervalo. EVERY_N_DAYS se calcula usando días calendario desde `startDate`, no bloques de 24 horas desde una toma confirmada.

### RF-004 — Edición y consistencia

Permitir modificar nombre, concentración, cantidad, unidad, notas y programación. Guardar los cambios relacionados en una **transacción** para no dejar horarios incompletos si ocurre un fallo. Al cambiar horarios o frecuencia, la próxima ocurrencia debe reflejar la versión actualizada.

Pedir confirmación antes de descartar cambios sin guardar.

### RF-005 — Activación

Desactivar no elimina el medicamento ni su programación. Los medicamentos desactivados no tienen próxima toma calculada mientras estén inactivos. Reactivar restaura su programación guardada, respetando fechas de inicio y finalización.

### RF-006 — Cálculo de próxima toma

Dado un medicamento activo, su programación y el instante actual, calcular la primera ocurrencia **estrictamente posterior** al instante actual, respetando frecuencia, horarios y rango de fechas.

No retornar ocurrencias posteriores a `endDate`; si no existe ninguna, devolver ausencia de resultado. Si el medicamento está inactivo, no generar próxima toma.

La función de cálculo debe ser independiente de la Activity, de Room y de llamadas directas al reloj, recibiendo el instante de referencia como parámetro para facilitar pruebas.

Una ocurrencia programada no implica que el usuario haya tomado el medicamento.

## Modelo local propuesto

Usar una versión de **Room 2.x compatible con Java** sobre SQLite; confirmar versión exacta y compatibilidad con el Gradle/AGP existentes antes de fijarla. Mantener un único módulo `app`, Java y recursos XML según [AGENTS.md](../AGENTS.md).

### `Medication`

| Campo | Descripción |
| --- | --- |
| `id` | Clave primaria autogenerada |
| `name` | Nombre no vacío |
| `strength` | Concentración opcional |
| `doseQuantity` | Cantidad numérica positiva |
| `doseUnit` | Unidad obligatoria |
| `notes` | Notas opcionales |
| `active` | Booleano |
| `createdAt` | Instante de creación |
| `updatedAt` | Instante de actualización |

### `MedicationSchedule`

| Campo | Descripción |
| --- | --- |
| `id` | Clave primaria autogenerada |
| `medicationId` | Clave foránea a `Medication` |
| `recurrenceType` | `DAILY`, `WEEKLY` o `EVERY_N_DAYS` |
| `daysOfWeek` | Días ISO lunes=1 ... domingo=7, aplicable a WEEKLY |
| `intervalDays` | Entero positivo, aplicable a EVERY_N_DAYS |
| `startDate` | Fecha local de inicio |
| `endDate` | Fecha local final opcional |

### `ScheduleTime`

| Campo | Descripción |
| --- | --- |
| `id` | Clave primaria autogenerada |
| `scheduleId` | Clave foránea a `MedicationSchedule` |
| `time` | Hora local programada |

Relaciones: un medicamento tiene una programación administrable en este MVP; una programación tiene uno o más horarios. Declarar unicidad para `MedicationSchedule.medicationId` y para la combinación `(ScheduleTime.scheduleId, ScheduleTime.time)`. Esto deja abierta la ampliación futura del modelo mediante migración explícita, sin añadir múltiples programaciones a la interfaz actual.

Añadir claves foráneas e índices relevantes. Elegir conversiones de tipos explícitas para fechas, horas y días de semana; documentar sus representaciones. No operar la base de datos en el hilo principal. No utilizar `fallbackToDestructiveMigration` para actualizaciones ordinarias: introducir migraciones cuando cambie el esquema.

## Reglas de validación

- Recortar espacios al validar el nombre y la unidad; no admitir valores vacíos.
- `doseQuantity` debe ser mayor que 0.
- Debe existir al menos una hora válida y no repetida.
- WEEKLY exige al menos un día seleccionado.
- EVERY_N_DAYS exige `intervalDays >= 1`.
- `endDate` no puede preceder a `startDate`.
- Los campos específicos de una recurrencia no deben interpretarse para otra.
- Las validaciones deben ejecutarse antes de persistir y los errores deben aparecer junto al campo pertinente.

## Interfaz

- Mantener la solución Android nativa Java + XML.
- Ampliar la pantalla inicial con un listado y botón visible de agregar.
- Incorporar formulario de medicamento y edición reutilizando los mismos campos.
- Permitir seleccionar recurrencia, días para WEEKLY y agregar/quitar horas.
- Usar componentes Android/Material solo si se justifican y son compatibles con el proyecto; evitar dependencias visuales innecesarias.
- Mantener textos visibles y accesibles en español; no incrustar textos de UI en clases Java cuando puedan declararse en recursos.

## Consideraciones de datos y privacidad

- Los datos se almacenan en el dispositivo, sin envío a servidores.
- No introducir permiso de internet por esta función.
- Revisar las reglas actuales de `allowBackup` y extracción de datos antes de almacenar información de salud; documentar la política escogida. No habilitar una transferencia implícita de datos sensibles sin decidirlo explícitamente.
- No incluir nombres/dosis personales reales en código, fixtures, capturas ni logs.

## Estrategia de implementación propuesta

1. Revisar el proyecto y las instrucciones vigentes; confirmar versiones compatibles de Room 2.x y procesador de anotaciones Java.
2. Incorporar entidades, conversiones de tipos, DAO y base de datos Room.
3. Implementar validaciones y cálculo de recurrencia en clases Java probables de forma aislada.
4. Crear el formulario y listado, conectados a persistencia sin bloquear el hilo principal.
5. Añadir pruebas y ejecutar las verificaciones acordadas en `AGENTS.md`.
6. Actualizar README con decisiones de persistencia y comprobaciones reproducibles.

## Pruebas mínimas

- Crear dos medicamentos distintos con igual nombre y verificar que ambos persisten.
- Crear, leer, editar, desactivar y reactivar.
- Reabrir la base de datos y comprobar persistencia.
- Rechazar nombre vacío, dosis no positiva, unidad vacía, horarios duplicados y recurrencias incompletas.
- Confirmar atomicidad de la edición de medicamento + horarios.
- DAILY con varias horas y cruce de medianoche.
- WEEKLY con varios días y avance a la semana siguiente.
- EVERY_N_DAYS anclado a fecha de inicio, incluido cambio de mes.
- Fechas inicial/final inclusivas, horario justo en el instante de referencia y ausencia de próximas tomas.
- Medicamento inactivo y finalización de programación.

Priorizar pruebas unitarias para recurrencia/validaciones y pruebas instrumentadas de Room cuando exista dispositivo/emulador disponible. Informar explícitamente las pruebas que no pudieron ejecutarse.

## Criterios de aceptación

- [ ] La aplicación permite registrar y editar al menos dos medicamentos con programaciones independientes.
- [ ] Se pueden desactivar y reactivar sin perder datos.
- [ ] Los datos sobreviven al cierre/reinicio de la aplicación.
- [ ] Se calcula una próxima toma correcta para DAILY, WEEKLY y EVERY_N_DAYS.
- [ ] Los formularios rechazan datos inválidos con mensajes útiles.
- [ ] No se implementan alarmas, registro de tomas ni servicios remotos.
- [ ] `:app:assembleDebug` y `:app:lintDebug` terminan satisfactoriamente o se documenta el bloqueo real.
- [ ] `:app:testDebugUnitTest` se ejecuta cuando las pruebas estén disponibles.
- [ ] Las verificaciones instrumentadas se realizan en dispositivo/emulador disponible o se reportan como pendientes.
- [ ] La entrega resume archivos modificados, decisiones técnicas, pruebas y pendientes, sin atribuir resultados no comprobados.

## Autorización y seguimiento

Documento elaborado a partir del alcance conversado. Su incorporación al repositorio **no autoriza implementar código automáticamente**; la ejecución se solicitará por separado. Mantener el registro de ejecución y las verificaciones de SPEC-001 como referencia, sin asumir que sustituyen las pruebas de esta entrega.
