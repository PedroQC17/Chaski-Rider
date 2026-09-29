# Revisión estática del flujo MVVM

Se revisaron las doce rutas activas de AppNavigation y sus operaciones de datos.

| Pantalla / operación | ViewModel | Estado observado | Repositorio |
|---|---|---|---|
| Acceso con Google | AuthViewModel | AuthUiState | AuthRepository |
| Acceso con correo | AuthViewModel | AuthUiState | AuthRepository |
| Recuperación y contraseña | AuthViewModel | AuthUiState | AuthRepository |
| Datos personales del registro | RiderProfileViewModel | RiderProfileUiState | RiderProfileRepository |
| Vehículo del registro | RiderProfileViewModel | RiderProfileUiState | RiderProfileRepository |
| Banco y envío del registro | RiderProfileViewModel | RiderProfileUiState | RiderProfileRepository |
| Archivos del registro | DocumentsViewModel | DocumentsUiState | DocumentRepository |
| Estado del registro y actualización | AuthViewModel | AuthUiState | AuthRepository |
| Inicio y disponibilidad | RiderProfileViewModel | RiderProfileUiState | RiderProfileRepository |
| Perfil y resumen | RiderProfileViewModel / AuthViewModel | Perfil / sesión compartida | RiderSession |
| Edición de datos personales | RiderProfileViewModel | RiderProfileUiState | RiderProfileRepository |
| Edición de vehículo | RiderProfileViewModel | RiderProfileUiState | RiderProfileRepository |
| Documentos: foto, subida y consulta | DocumentsViewModel | DocumentsUiState | DocumentRepository |
| Notificaciones y contador en inicio | NotificationsViewModel | NotificationsUiState | NotificationsRepository |
| Cierre de sesión | AuthViewModel | AuthUiState | AuthRepository |

AppNavigation observa los StateFlow con collectAsStateWithLifecycle y entrega los
datos a las vistas mediante parámetros. RiderSession comunica los cambios de perfil
a los ViewModel; ninguna pantalla consulta Firestore, Storage ni NotificationsStore.

Se movió la preparación del archivo de cámara desde ProfileDocumentsScreen al flujo
DocumentsViewModel → DocumentRepository → RiderDocumentDataSource. La inicialización
de notificaciones salió de MainActivity y quedó en data, invocada por la raíz de
composición AppContainer. GoogleSignInClient está en ui/platform: presenta Credential
Manager; el token se envía a AuthViewModel y la autenticación real ocurre en data.

Los campos no guardados, diálogos, permisos y selectores conservan estado visual
local de Compose. Son parte de la View y no equivalen a acceso directo al Model.
Crear un ViewModel para cada icono o componente visual no es necesario para MVVM.
Las aperturas de archivos y navegación son efectos de UI; no escriben datos remotos.
Las notificaciones de FCM entran por el servicio Android en data y vuelven a las
pantallas mediante repositorio → NotificationsViewModel → NotificationsUiState.

Esta verificación es estática. No confirma compilación ni ejecución en dispositivo;
no se ejecutaron pruebas por indicación del propietario. El backend trasladado sigue
pendiente de despliegue; disponibilidad y edición en ciertos estados dependen de él.
Los pedidos reales todavía no están implementados y no forman parte del flujo actual.
