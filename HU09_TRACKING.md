# Seguimiento desde el recojo

Implementación del 27 de octubre de 2026.

## Regla de negocio

La posición del repartidor solo se solicita para compartir cuando su registro local corresponde al mismo usuario/pedido, está en `ON_THE_WAY` y contiene hora de recojo. La búsqueda de ofertas y el GPS usado para llegada siguen siendo funciones separadas. Aceptar o llegar al local no inicia el envío de tracking.

El seguimiento no depende de continuar Disponible. Una incidencia que mantiene el pedido activo no lo detiene. Entregar o cancelar autorizadamente cambia el registro local y detiene el servicio. Cerrar sesión o cambiar el usuario autenticado también lo detiene.

## Android

`OrderTracking` contiene la regla y comprueba el pedido antes y después de obtener GPS. `TrackingLocationSource` obtiene posición, precisión y hora real de la medición con Fused Location. Se descartan coordenadas inválidas, posiciones de más de 30 segundos, mediciones futuras y precisiones peores de 100 m.

`OrderTrackingService` es un servicio foreground de tipo location con notificación persistente. Se inicia desde la UI visible al recoger o recuperar un pedido recogido; continúa al minimizar o abrir Google Maps. Observa el registro Room para cancelar la tarea cuando cambia de etapa. Consulta aproximadamente cada 15 segundos después de finalizar la operación anterior. No es una frecuencia exacta garantizada.

Al aceptar un pedido se detiene el servicio de búsqueda de ofertas. Al cerrar ese pedido y volver a buscar puede iniciarse nuevamente. No se requiere permiso de ubicación en segundo plano: el servicio de ubicación se inicia mientras la app está visible, con los permisos existentes.

La notificación informa si está enviando, esperando GPS, esperando sincronización del pedido o reintentando por conectividad. Si no es posible iniciar el servicio por restricciones/permisos, debe abrirse Chaski de nuevo y revisar los permisos. Tras una detención forzada o muerte del proceso no se promete seguimiento continuo; al reabrir se recupera el pedido y se intenta reanudar. No se usa WorkManager para iniciar GPS en segundo plano.

## Firebase de Rider

Ruta: `riders/{uid}/deliveryRecords/{orderId}/tracking/current`.

Campos: `orderId`, `riderId`, `latitude`, `longitude`, `accuracyMeters`, `recordedAt` (milisegundos del dispositivo) y `updatedAt` (timestamp del servidor).

La transacción comprueba que el pedido remoto esté en `ON_THE_WAY`; si aún no se sincronizó el recojo espera a otra consulta. Solo conserva la última posición. No publica muestras anteriores a la ya registrada. Las reglas validan propietario, etapa, campos, precisión, coordenadas y antigüedad; permiten un margen de transporte de 45 segundos respecto al servidor. No se acumula una cola de posiciones para reproducir un recorrido al reconectar.

Al sincronizar `DELIVERED` o `CANCELLED`, `OrderSyncWorker` actualiza el pedido y elimina `tracking/current` en la misma transacción. Una transacción de ubicación concurrente debe reevaluar el estado del pedido y no puede recrear la posición después de finalizar. Si se finaliza sin internet, el servicio se detiene localmente y la eliminación remota espera al envío pendiente; el último punto remoto no debe interpretarse como movimiento actual.

Las reglas de tracking se añadieron de forma aislada a las reglas publicadas. No se publicaron funciones ni las reglas de soporte pendientes. Ruleset verificado: `projects/chaski-rider/rulesets/48cb1527-23a7-4511-88ac-64f85da8495f`.

## Integración pendiente con Chaski Food

No se concedió acceso público a las posiciones ni acceso automático a usuarios del otro proyecto Firebase. Chaski Food necesitará integrar un backend confiable que valide cliente y pedido antes de entregar la posición. El consumidor deberá exigir estado `ON_THE_WAY` y mostrar antigüedad de la muestra; una muestra antigua debe marcarse como desactualizada, no como seguimiento en vivo. El modelo no inventa identificadores de clientes.

## Verificación

72 pruebas unitarias aprobadas. Seis pruebas de reglas de pedidos/tracking aprobadas en Firebase local contra la vista previa exacta de producción. Cubren prohibición antes del recojo, aislamiento, muestras inválidas/antiguas, revisiones y eliminación al finalizar. App y APK de pruebas compilados.

Pasaron las seis pruebas Android en el celular conectado: las cuatro pruebas de migración/Room/flujo local, recuperación real de sincronización tras desconectar Wi-Fi/datos y seguimiento real del servicio. Esta última comprobó ausencia de publicación antes del recojo, envío con GPS del dispositivo tras recoger, otra muestra más reciente después de pulsar Inicio para minimizar la app, eliminación de la posición al entregar y rechazo de una publicación posterior. Se esperó otro intervalo para comprobar que no reapareciera. Se utilizaron pedidos técnicos separados del pedido activo, sin cambiar cuentas ni perfiles. La prueba de sincronización restauró los estados de conectividad.

Referencia Android: https://developer.android.com/develop/background-work/services/fgs/service-types#location.
