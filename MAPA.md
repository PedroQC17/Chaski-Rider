# Mapa del área de trabajo

La pantalla usa Google Maps Compose (Maps SDK for Android). Hasta configurar una clave muestra un fondo neutro con un aviso; no inicializa el mapa ni presenta una ubicación ficticia del repartidor.

## Configuración

1. Habilitar Maps SDK for Android en el proyecto de Google Cloud elegido y revisar su facturación.
2. Crear una clave restringida a aplicaciones Android: paquete `com.example.chaskirider` y SHA-1 del certificado de firma correspondiente (depuración o distribución). Restringir también su API a Maps SDK for Android.
3. Añadir `MAPS_API_KEY=tu_clave` a `local.properties`, que no debe subirse a Git. También se admite la variable de entorno `MAPS_API_KEY`.
4. Sincronizar Gradle y recompilar. La configuración se incorpora al manifiesto durante la compilación.

Actualización: Maps SDK for Android ya está habilitado en `chaski-rider` y la clave de depuración está configurada localmente, restringida al paquete y certificado Android. El usuario confirmó que el mapa carga. La clave de distribución requerirá su certificado de firma. Consultar los precios vigentes antes de activar otros servicios: https://developers.google.com/maps/billing-and-pricing/pricing

## Arquitectura

`HomeRoute` gestiona permisos y ciclo de vida. `HomeScreen`, `WorkMap` y `ConnectionPanel` representan `HomeUiState` y envían acciones a `HomeViewModel`. El ViewModel obtiene la sesión mediante `RiderSession`, cambia la disponibilidad mediante `RiderProfileRepository` y solicita ubicación mediante `LocationRepository`. `DeviceLocationRepository` implementa esta última con el proveedor de ubicación de Android. No hay seguimiento en segundo plano.

La cámara parte de Lima cuando no hay ubicación del dispositivo; esto no representa la posición del usuario. La ubicación se solicita al pulsar el control correspondiente y se vuelve a comprobar al regresar a la pantalla. El permiso aproximado también se admite.

El menú lateral sustituye la barra inferior y reúne mapa, perfil, notificaciones, contraseña y cierre de sesión. Los textos visibles están en `res/values/strings.xml`.

El mapa usa su SDK directamente. No requiere Retrofit; las futuras llamadas REST deben implementarse en la capa de datos detrás de repositorios.

La disponibilidad solamente cambia después de que el servidor confirme la operación. La acción `availability` ya está publicada. La primera demostración de ofertas está separada del mapa de trabajo: consultar `PEDIDOS.md`.
