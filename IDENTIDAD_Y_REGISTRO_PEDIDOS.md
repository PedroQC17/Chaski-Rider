# Identidad y registro de pedidos

## Verificación en celular — 27 de octubre de 2026

Se instalaron los APK conservando los datos existentes. Pasaron las cuatro pruebas Android de migración, cola por revisiones/historial, recuperación/aislamiento con Room y recorrido de aceptación-recojo-entrega con geocercas.

También pasó una prueba de integración real de `OrderSyncWorker` con la sesión existente y Firebase de producción, utilizando registros técnicos separados del pedido activo. Se comprobó envío de ACCEPTED, conservación pendiente al desconectar Wi-Fi/datos, reintento automático de WorkManager al recuperar conexión (sin reprogramar el trabajo desde la prueba), confirmación de ON_THE_WAY y actualización a DELIVERED en el mismo documento. Una lectura independiente por API confirmó DELIVERED, revisión 3, mismo ID y ausencia de ubicación del repartidor. Se restauraron Wi-Fi/datos; no se cambiaron perfiles ni credenciales. Los registros técnicos se identifican por el negocio `Prueba tecnica de sincronizacion`.

No se verificó un cambio real entre dos cuentas mediante inicio de sesión; el aislamiento por UID se comprobó con las pruebas de Room. La prueba de pérdida de red afectó al teléfono real. Estos resultados actualizan las notas de pruebas pendientes conservadas más abajo.

Actualización: 27 de octubre de 2026.

## Qué cambia

Cada nueva oferta genera un `orderId` UUID distinto del ID de la ruta del catálogo. El identificador del catálogo permanece únicamente en `DemoOrderState.catalogRouteId`, para controlar la repetición de rutas. Aceptación, navegación, incidencias, contexto de soporte y futuro seguimiento utilizan el identificador del pedido. Una ruta repetida genera otro pedido.

Los pedidos locales antiguos conservan su identificador para no romper referencias existentes. No se reescriben tickets antiguos. La separación de identificadores se garantiza para las nuevas ofertas generadas por esta versión.

El primer aceptado registra `acceptedAt`. Llegar conserva `arrivedAt` y la ubicación local de llegada; recoger conserva `pickedUpAt`; entregar conserva `deliveredAt`. No se permite recoger después de entregar ni cancelar directamente después de recoger. No hay creación de pedidos reales por parte de Chaski Food en esta entrega.

## Room y sincronización

Room conserva el pedido activo en `demo_orders`. La versión 2 de la misma base añade `order_records`, con clave compuesta `(uid, orderId)`, contenido del registro, revisión y marca pendiente. La migración 1→2 crea la tabla sin borrar el estado existente. El historial de registros completados/cancelados no se elimina al solicitar el siguiente pedido.

Guardar estado activo y actualizar su registro pendiente ocurre dentro de una transacción Room. Si el contenido no cambia, no aumenta la revisión ni se programa otro trabajo. La cola conserva la última revisión de cada pedido, no una bitácora de todas las acciones. Los horarios de los hitos se mantienen en ese registro.

WorkManager ejecuta `OrderSyncWorker` con conectividad, autenticación del mismo usuario y reintento exponencial para errores transitorios. Cada cambio relevante programa trabajo; abrir sesión programa la revisión de pendientes. Si cambia la cuenta, no se envían los pendientes de la otra cuenta. Los errores permanentes de permisos no se reintentan indefinidamente; el registro permanece pendiente y el siguiente inicio de sesión puede volver a programarlo.

Después de una confirmación remota, Room solo marca como enviada esa misma revisión. Si durante el envío se guardó una nueva, sigue pendiente. La transacción Firestore no sobrescribe revisiones remotas iguales o posteriores. Las solicitudes repetidas utilizan el mismo ID de documento.

La copia local sigue disponible sin internet. WorkManager no garantiza envío inmediato; no se debe interpretar el avance local como confirmación de recepción por Chaski Food. No existe todavía descarga del historial desde la nube a otro dispositivo.

## Firebase de Rider

Ruta: `riders/{uid}/deliveryRecords/{orderId}`.

Campos: `orderId`, `riderId`, `status`, `revision`, `merchantName`, `pickup`, `destination`, `acceptedAt`, `arrivedAt`, `pickedUpAt`, `deliveredAt`, `cancelledAt`, `updatedAt`.

Estados: `ACCEPTED`, `AT_MERCHANT`, `ON_THE_WAY`, `DELIVERED`, `CANCELLED`. Los horarios de negocio son milisegundos del dispositivo; `updatedAt` es timestamp del servidor. Los horarios desconocidos de un pedido antiguo no se inventan.

`pickup` y `destination` son destinos del pedido. Este documento **no contiene la ubicación actual del repartidor ni sus coordenadas de llegada**. La ubicación de llegada queda en Room. No hay envío de tracking antes del recojo ni implementación de tracking en vivo en esta entrega.

Este registro es una copia del avance generado por Rider, no una autoridad para asignación, cobros o pedidos de Chaski Food. No contiene un cliente inventado ni concede acceso a usuarios de otro proyecto Firebase. Al integrar pedidos reales, el origen de ofertas deberá suministrar el ID real y la asignación deberá validarse en un backend confiable.

## Reglas publicadas

Se consultaron las reglas publicadas y se añadió únicamente el bloque de `deliveryRecords`, preservando las demás reglas. Ruleset verificado: `projects/chaski-rider/rulesets/7fc04605-a1e3-41b9-a072-4f7d518e4bf5`. El contenido publicado coincidió exactamente con la vista previa aislada. No se desplegaron funciones, Storage ni los demás cambios pendientes del backend. Las reglas de soporte siguen pendientes.

Solo el propietario puede leer/escribir su registro. Se validan campos, coordenadas de los destinos, consistencia básica de etapas, aumento de revisión y conservación de identidad/horarios ya confirmados. Se rechazan borrados y retrocesos de etapa. Se permite que un primer envío contenga una etapa avanzada si hubo trabajo sin conexión.

## Prueba manual pendiente

1. Instalar la versión nueva conservando datos; comprobar recuperación del pedido antiguo.
2. Aceptar una oferta nueva y revisar su `orderId` en el registro local y en Firebase.
3. Llegar, recoger y entregar; comprobar el mismo documento y las horas. No debe haber posición del repartidor.
4. Sin conexión, avanzar las etapas permitidas; reconectar y comprobar sincronización del último estado.
5. Completar, pedir otro y verificar que conserva el registro anterior y genera otro ID.
6. Cambiar de cuenta con pendientes y comprobar que no se publican bajo el nuevo usuario.

Verificación automatizada: 68 pruebas unitarias sin fallos; app debug y APK de pruebas Android compilados. Tres pruebas de reglas aprobadas con las reglas locales y nuevamente con la vista previa exacta de producción. Las pruebas Android cubren migración y confirmación de revisión pendiente, pero no pudieron ejecutarse porque ADB no detectó un dispositivo. No se escribieron pedidos de prueba en Firebase real.

Referencias: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started y https://firebase.google.com/docs/firestore/manage-data/transactions.
