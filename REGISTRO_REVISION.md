# Registro y revisión: segunda mejora

12 de octubre de 2026. Esta entrega implementa la revisión del registro en Android y las funciones del backend. Continúa la persistencia de formularios documentada en `REGISTRO_PERSISTENCIA.md`.

## Comportamiento del repartidor

| Estado de registro | Qué se explica | Acción disponible |
|---|---|---|
| INCOMPLETE | Todavía faltan datos o documentos; no se ha enviado la solicitud. | Continuar el registro. |
| PENDING_REVIEW | La solicitud ya está enviada. No hace falta repetirla. | Consultar el resultado. |
| NEEDS_CORRECTION | La solicitud puede continuar; se muestra el motivo y cada documento observado. | Corregir y volver a enviar. |
| REJECTED | La revisión cierra la solicitud; se muestra el motivo de la decisión. | Cerrar sesión; si falla la consulta, reintentar. No editar, reenviar ni trabajar. |
| APPROVED | El registro está aprobado. | Continuar al acceso de trabajo; la foto de perfil sigue siendo necesaria. |

Si `isEnabled=false`, la restricción de cuenta tiene prioridad sobre la revisión. Esta entrega no introduce los estados de suspensión temporal, bloqueo permanente ni baja: pertenecen a la siguiente revisión del estado operativo de la cuenta.

La pantalla de revisión tiene `RegistrationReviewUiState` y `RegistrationReviewViewModel`. El estado se obtiene del perfil remoto, no se infiere a partir de un borrador de Room. La consulta se realiza al abrir o retomar esta pantalla y mediante Actualizar estado; no hay sondeo periódico. Un error mantiene el último resultado y permite reintentar. Cambiar de cuenta cancela la consulta anterior.

## Corrección de documentos

`documentReviews` es un mapa por tipo de documento: `dniFront`, `dniBack`, `bankStatement`, `driverLicense` y `soat` cuando correspondan al vehículo. Cada valor contiene `status` y `reason`.

- PENDING: el documento aún no tiene aprobación del revisor.
- APPROVED: el revisor aprobó el documento junto con el registro.
- OBSERVED: necesita reemplazo y contiene instrucciones concretas.

El estado de subida continúa separado: Subido solo confirma que el archivo llegó al servidor. No significa aprobación. Antes de reenviar es necesario sustituir cada documento requerido observado. Reenviar la misma ruta no elimina la observación. Android bloquea el botón y el servidor también rechaza el intento.

Una sustitución crea un archivo con otro UUID y reinicia la revisión de ese documento a PENDING. El resto de archivos y los datos del formulario se conservan. El motivo general continúa visible en los tres pasos hasta un nuevo envío exitoso. Una observación general de datos personales o bancarios puede expresarse en `reason` sin observar documentos.

## Backend y administración

La fuente de despliegue sigue siendo `C:/Users/Angel/Desktop/Chaski_Rider_Backend/functions`. La copia de registro en `Chaski_Rider/backend` conserva las mismas reglas de negocio; sus funciones de ofertas siguen teniendo diferencias independientes de esta entrega.

`reviewRider` exige el claim administrativo `riderReviewer=true` y acepta:

```json
{
  "uid": "UID_DEL_REPARTIDOR",
  "status": "NEEDS_CORRECTION",
  "submissionVersion": 1,
  "reason": "Reemplaza la foto del DNI; no se puede leer el número.",
  "documentObservations": {
    "dniFront": "Toma una foto nítida con el documento completo."
  }
}
```

Las decisiones posibles son APPROVED, NEEDS_CORRECTION y REJECTED. Corrección y rechazo requieren un motivo de hasta 1000 caracteres. `documentObservations` solo corresponde a NEEDS_CORRECTION y solo admite documentos requeridos por el vehículo. Aprobar el registro aprueba todos los documentos requeridos; los documentos no observados en una solicitud de corrección permanecen PENDING, sin inventar aprobaciones parciales.

La cuenta revisada debe estar habilitada y en PENDING_REVIEW. Cada envío incrementa `submissionVersion`; el revisor debe leer la versión actual y enviarla con su decisión. Un reintento de submit mientras sigue pendiente no incrementa la versión. Esto impide aprobar una solicitud nueva con información de una revisión anterior.

La decisión y el historial `riders/{uid}/registrationReviews/{submissionVersion}` se escriben en la misma transacción. Quedan el motivo, los documentos, la fecha y el UID del revisor. El cliente no puede escribir el perfil ni modificar el historial. Los revisores pueden leer el historial mediante sus permisos.

Los rechazos definitivos conservan las reservas de DNI y celular. Crear otra cuenta no permite reutilizar esos datos para eludir la decisión. Los bloqueos operativos de cuentas ya aprobadas serán un flujo separado.

Storage exige explícitamente que el archivo aún no exista (`resource == null`), además del propietario, estado editable, tipo, tamaño y formato permitidos. Los clientes no pueden sobrescribir archivos que ya fueron revisados. La foto de perfil también tiene esta protección.

## Compatibilidad y publicación

- Los perfiles anteriores sin `documentReviews` mantienen una revisión documental pendiente en la UI. No se inventa una aprobación.
- Un perfil pendiente antiguo sin versión puede revisarse enviando explícitamente `submissionVersion: 0`.
- Las versiones antiguas de Android no conocen REJECTED. Actualizar la app antes de habilitar rechazos con el nuevo backend.
- Los formularios y Room conservan su esquema actual; estos campos de revisión proceden del servidor.
- No se publicaron funciones ni reglas durante esta entrega. Para activar el flujo remoto hay que publicar `riderRegistration`, `reviewRider`, Firestore Rules y Storage Rules desde el backend separado.
- La revisión se hace fuera de la app del repartidor usando el callable administrativo. La consola de soporte y los tickets continúan pendientes; no se muestra un botón que afirme crear un ticket inexistente.

## Archivos relevantes

| Área | Archivos |
|---|---|
| Dominio Android | `domain/model/RegistrationStatus.kt`, `RiderUser.kt`, `DocumentReview.kt` |
| Estado y consulta de revisión | `ui/screens/auth/RegistrationReviewUiState.kt`, `RegistrationReviewViewModel.kt` |
| Pantalla de resultado | `ui/screens/auth/RegistrationStatusScreen.kt` |
| Corrección y reenvío | `ui/screens/onboarding/RegistrationViewModel.kt`, `RegistrationUiState.kt`, `OnboardingStep1/2/3Screen.kt` |
| Permisos de captura y navegación | `ui/screens/profile/documents/DocumentsViewModel.kt`, `ui/navigation/AppNavigation.kt` |
| Reglas y transacciones del servidor | `functions/registration-review.js`, `functions/index.js` en el backend separado |
| Acceso y archivos inmutables | `firestore.rules`, `storage.rules` en el backend separado |

Los archivos Android se encuentran bajo `app/src/main/java/com/example/chaskirider/`.

## Validación local realizada

- Android compila y genera el APK debug. 25 pruebas JVM correctas: 11 del formulario, 5 de consulta/estados de revisión y 9 de validación existentes.
- La ejecución completa de `:app:connectedDebugAndroidTest` pasó 13 pruebas en Pixel 7 API 36 (Android 16): captura y restricciones, persistencia de Room, pantallas de revisión, deserialización de REJECTED y del mapa documental con el mapper de Firestore, y pantallas de registro existentes.
- Tras ajustar el título de espera y retirar Actualizar estado del rechazo definitivo sin error, se repitió RegistrationReviewScreensTest: 6/6 correctas y BUILD SUCCESSFUL.
- Backend separado: 14 pruebas unitarias correctas. La copia de registro dentro de Android también pasa su suite completa de 22 pruebas, que incluye sus pruebas independientes de ofertas y cotización.
- Integración Firebase local: 7 pruebas correctas, con Auth, Firestore, Storage y Functions en el proyecto demo-chaski-rider. Incluyen sustituir un documento observado, reenviar, impedir revisiones antiguas, aprobar, guardar auditoría y bloquear todos los cambios tras un rechazo.
- Se corrigieron fixtures antiguos de banco/CCI y se alineó el bucket de Storage del test con el de Functions. Se añadió la condición explícita de archivo inexistente y se verificó que una sobrescritura se deniega incluso cuando los bytes son iguales.
- Inspección visual de capturas de espera, corrección y rechazo. Las capturas locales se guardan en `captures/registration-review-20261012/`, excluido de Git; corresponden a la ejecución previa al último ajuste de título y botón. Los últimos ajustes quedaron verificados por la repetición de las 6 pruebas de revisión. El runner retiró los archivos del emulador al finalizar esa ejecución.
- Los tests no revisaron usuarios reales ni publicaron cambios. El entorno de Functions local utiliza Node 24; el despliegue sigue configurado para Node 22.
- El arranque totalmente sin conexión y los flujos reales de soporte/suspensión/baja continúan pendientes.
