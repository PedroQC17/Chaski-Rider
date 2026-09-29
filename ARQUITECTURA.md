# Arquitectura de Chaski Rider

La aplicación utiliza MVVM con tres grupos: `ui`, `domain` y `data`.

Flujo: interacción en Screen → método del ViewModel → contrato de repositorio →
implementación en data → Firebase → resultado → UiState → recomposición de Screen.

## Presentación

- `ui/screens/auth`: AuthViewModel y AuthUiState gestionan inicio de sesión,
  restauración de sesión, recuperación y contraseña.
- `ui/screens/profile`: RiderProfileViewModel y RiderProfileUiState gestionan
  datos personales, vehículo, banco, disponibilidad y envío del registro.
- `ui/screens/profile/documents`: DocumentsViewModel y DocumentsUiState gestionan
  selección para subir, carga, errores y apertura de documentos.
- `ui/screens/notifications`: NotificationsViewModel y NotificationsUiState
  presentan las notificaciones y el contador de mensajes sin leer.
- `ui/navigation/AppNavigation.kt`: compone las pantallas, observa los cuatro
  StateFlow con collectAsStateWithLifecycle y conecta eventos con ViewModel.
- `ui/screens/home/components` y `ui/screens/profile/components`: componentes
  visuales separados de las pantallas.

Cada ViewModel mantiene MutableStateFlow privado y expone StateFlow de solo lectura.
Los repositorios se reciben por constructor; los ViewModel no obtienen Firebase ni
leen AppContainer. AppContainer actúa como raíz de composición para las dependencias.
Las pantallas reciben datos y callbacks. El estado visual local (diálogos, permisos,
deslizadores y texto aún no guardado) puede mantenerse con remember en Compose.
Credential Manager y la apertura externa de archivos necesitan el contexto de la UI;
Firebase Authentication y las operaciones de datos quedan en data.

## Dominio y datos

`domain/model` contiene los modelos y validaciones. `domain/repository` declara
AuthRepository, RiderProfileRepository, DocumentRepository, NotificationsRepository
y RiderSession.

`data/repository` implementa estos contratos. `data/remote` consulta Firestore,
invoca Functions y traduce errores. `data/documents` administra Storage y archivos
temporales. `data/session` mantiene una única sesión observable, compartida por los
ViewModel. `data/notifications` recibe FCM y administra mensajes y suscripciones.
Los contratos de documentos usan Uri porque este es un proyecto Android; esa parte
del dominio no es un módulo Kotlin independiente de Android.

## Alcance actual

La simulación de pedidos se eliminó. No hay todavía repositorio, ViewModel ni flujo
de pedidos reales. Se implementarán cuando se definan sus requisitos y backend.
Las notificaciones existentes siguen siendo un almacén en memoria.

El backend está únicamente en la carpeta hermana Chaski_Rider_Backend. Los cambios
locales trasladados de backend/index.js y backend/validation.js siguen pendientes
de despliegue. Sus políticas de edición y las reglas de Storage deben revisarse
juntas antes de publicarse: permitir cambios en Functions no habilita por sí solo
subidas de documentos para perfiles aprobados.

La conexión ViewModel–UiState existía previamente, pero AuthViewModel acumulaba
responsabilidades y algunas vistas accedían directamente a NotificationsStore.
Esta reorganización separa esas responsabilidades.

No se ejecutaron pruebas, compilación, commits, push ni despliegues durante esta
reorganización. Se revisaron referencias y diferencias de archivos de forma estática.
