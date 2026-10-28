# HU16 — cola persistente por acción

Room versión 3 conserva dos representaciones: `order_records` contiene el estado más reciente por pedido; `order_actions` conserva cada revisión pendiente y su contenido original. El estado local y su acción se guardan en la misma transacción.

Cada acción tiene `actionId = orderId_revision`, propietario, pedido, revisión, tipo y flag `pending`. Los tipos incluyen aceptación, llegada, inicio de espera, recojo, incidencia y finalización. Consultar repetidamente el estado sin cambios no crea nuevas revisiones. `waitStartedAt` permite conservar el inicio de espera aunque después se recoja el producto sin conexión.

WorkManager procesa acciones pendientes en orden de revisión por pedido. En una transacción Firebase actualiza el estado si su revisión es más reciente, publica incidencias faltantes y crea el evento inmutable en `riders/{uid}/deliveryRecords/{orderId}/actions/{actionId}`. Si el evento ya existe no lo vuelve a escribir. Una acción atrasada nunca reduce la revisión del pedido remoto.

Solo después de la confirmación remota, Room marca esa acción como sincronizada. El registro más reciente se confirma únicamente si su revisión coincide: confirmar una acción antigua no borra el pendiente de una acción nueva. Los eventos confirmados permanecen en el historial.

La migración 2→3 recupera el último registro pendiente que todavía existe y lo etiqueta `RESTORE_SNAPSHOT`. No inventa etapas antiguas que el esquema anterior no conservó. La migración 1→2→3 conserva el estado existente sin limpieza destructiva.

El worker exige la cuenta propietaria, mantiene restricción de red y reintentos exponenciales para fallos transitorios. Errores permanentes de permisos/datos detienen ese trabajo y conservan los pendientes; no se convierten en confirmaciones falsas. Al iniciar la app se programa nuevamente la sincronización del usuario autenticado.

Alcance offline: progreso permitido del pedido simulado, espera e incidencias. Los chats y tickets conservan borradores, pero este cambio no promete enviarlos automáticamente sin conexión. La integración de pedidos reales de Chaski Food sigue pendiente.

Reglas publicadas y verificadas: `78d4118d-be6e-4933-97df-ecc478e6c9d4`. Se añadió solo el bloque de acciones al ruleset vigente. El servidor exige propietario, ID determinista, revisión compatible con el pedido y evento inmutable.

Pruebas: compilación y pruebas JVM; reglas de aislamiento/inmutabilidad/duplicados; dispositivo con migración, conservación de todas las revisiones, confirmación antigua y recuperación de red mediante el worker real.
