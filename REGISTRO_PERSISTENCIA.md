# Registro: primera mejora de la revisión

12 de octubre de 2026. Prioridad elegida: conservar el avance antes de ampliar soporte y estados administrativos. El alcance de esta entrega es Android; no modifica ni despliega los backends.

## Arquitectura y ubicación

| Responsabilidad | Archivo bajo `app/src/main/java/com/example/chaskirider/` |
|---|---|
| Formularios de datos personales, vehículo y banco | `ui/screens/onboarding/OnboardingStep1Screen.kt`, `OnboardingStep2Screen.kt`, `OnboardingStep3Screen.kt` |
| Estado único, validación, acciones y escritura ordenada del borrador | `ui/screens/onboarding/RegistrationViewModel.kt`, `RegistrationUiState.kt` |
| Aviso de recuperación, guardado o error local con reintento | `ui/screens/onboarding/RegistrationDraftNotice.kt` |
| Modelo editable y combinación con datos confirmados | `domain/model/RegistrationDraft.kt` |
| Contrato de persistencia | `domain/repository/RegistrationDraftRepository.kt` |
| Room, DAO, entidades y adaptación al dominio | `data/local/RegistrationDatabase.kt` |
| Creación e inyección de repositorios | `di/AppContainer.kt` |
| Captura, restauración, confirmación y reintento de documentos | `ui/screens/profile/documents/DocumentsViewModel.kt` |
| Archivos privados y Firebase Storage | `data/documents/RiderDocumentDataSource.kt` |
| Navegación y conexión de los estados con las pantallas | `ui/navigation/AppNavigation.kt` |

Las pantallas emiten eventos; no mantienen copias locales de los campos de negocio. El ViewModel expone StateFlow y coordina coroutines y repositorios. Room conserva el borrador al terminar el proceso; ViewModel por sí solo no ofrece esa garantía. El esquema inicial exportado está en `app/schemas/` y debe conservarse para futuras migraciones.

Se retiraron PersonalRegistrationViewModel/UiState y las operaciones de registro de RiderProfileViewModel para evitar dos responsables de las mismas acciones. Retrofit ya se usa en pedidos. El registro conserva sus repositorios Firebase: añadir otro transporte aquí no aportaría valor.

## Comportamiento

- Cada cambio de datos personales, términos, vehículo o banco se guarda por UID. Las escrituras se procesan en orden; no pueden sobrescribir una edición reciente con una anterior.
- Al restaurar se combinan los apartados editados localmente con los confirmados por el servidor. Un usuario no recibe el borrador de otro.
- La contraseña y su confirmación permanecen solamente en memoria. Tras cerrar el proceso hay que volver a escribirlas si aún no se vincularon. La vinculación conserva la cuenta Google y su correo.
- Continuar espera el guardado local y confirma el paso con el servidor. Un fallo local permite reintentar; un fallo remoto conserva el borrador. Finalizar solo se considera exitoso si el servidor acepta el registro.
- Un registro enviado o aprobado deja de ser editable y se elimina su borrador. El borrador nunca concede aprobación, disponibilidad ni permisos.
- Antes de abrir la cámara se guarda en Room el tipo de documento y la URI. El archivo queda en almacenamiento privado duradero, fuera de caché. Al recrear el ViewModel se recupera la vista previa, sin subirla automáticamente.
- Una subida fallida conserva la captura para reintentar. Confirmar con éxito o descartar elimina su referencia y archivo. Cambiar de cuenta cancela el trabajo de presentación y oculta la captura del usuario anterior.
- La base de borradores y las capturas se excluyen de copia de seguridad y transferencia entre dispositivos. Cerrar sesión conserva el borrador aislado para retomarlo con la misma cuenta; borrar datos o desinstalar elimina el almacenamiento local.

## Límites de esta entrega

- El arranque sigue consultando el perfil en Firebase con `Source.SERVER`. Los datos locales se conservan, pero reabrir sin conexión aún puede detenerse en la recuperación de sesión. No se declara completado el modo offline de HU16.
- No hay cola de envíos con WorkManager. Continuar, subir y finalizar requieren conexión y una acción explícita del usuario.
- La recuperación de captura cubre la cámara. Un archivo elegido en el selector cuya subida se interrumpe puede necesitar seleccionarse de nuevo.
- El guardado local es asíncrono: un cierre forzado antes de completar la escritura puede perder los últimos cambios que todavía indican «Guardando». No se promete recuperación tras borrar datos o desinstalar.
- Faltan pruebas integrales con cámara externa y cierre forzado del proceso durante una operación real de Firebase. Los tests de recreación usan repositorios controlados; la prueba Room cierra y vuelve a abrir una base real en Android.

## Decisiones de producto para las siguientes revisiones

- Alta libre. Revisión manual de documentos fuera de la app.
- Diferenciar observación corregible de rechazo definitivo. Soporte debe explicar los casos en que no se permite corregir.
- Diferenciar aprobación del registro y estado operativo de la cuenta: suspensión temporal, bloqueo permanente y baja.
- La baja la solicita el repartidor y la resuelve administración. Puede seguir trabajando mientras la solicitud está pendiente.
- Ayuda: categoría → problema → orientación → ticket cuando proceda. Conversación persistida y una consola mínima para que soporte responda realmente. No simular respuestas como si fueran atención humana.
- Revisar el inicio con el criterio de que cada elemento ayude a tomar una decisión; los textos flotantes de demanda de la captura original siguen pendientes de rediseño.

Estas decisiones están registradas para la siguiente entrega; no se presentan como funciones ya implementadas.

## Validación realizada

- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`: compilación y APK debug correctos.
- Validación final: `./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest --console=plain`: BUILD SUCCESSFUL.
- 18 pruebas JVM: 9 de validaciones existentes y 9 del nuevo ViewModel. Cubren recuperación de los tres formularios, exclusión de contraseña, escrituras lentas, aislamiento por cuenta, error de lectura/escritura, doble pulsación, cancelación al cambiar de usuario, vinculación y rechazo/aceptación del envío.
- 6 pruebas instrumentadas correctas en Samsung SM-A235M, Android 14: 2 de recuperación/reintento/aislamiento de capturas con repositorios controlados; 1 de persistencia real al cerrar/reabrir Room; 3 de pantallas Compose. Se corrigió el CCI vacío del caso de bicicleta existente para comprobar realmente un formulario válido.
- `git diff --check`: sin errores de espacios. El backend separado conserva los cambios que ya tenía; no se desplegó Firebase ni se enviaron registros de prueba al servidor.
- La primera ejecución instrumentada detectó una firma de test no compatible con JUnit; se corrigió y la ejecución final pasó completa.
