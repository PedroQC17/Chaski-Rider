# Chat del pedido

Actualización del 27 de octubre de 2026.

## Implementado en Android

Los mensajes del pedido se guardan en Firebase, no en una lista temporal del ViewModel. Se eliminaron los mensajes iniciales ficticios y el identificador de respaldo `demo_order` del flujo de producción. Los ejemplos de @Preview solo sirven para diseño y no se insertan en conversaciones.

La estructura es UI → ChatUiState → ChatViewModel → OrderChatRepository / ChatDraftStore. AppContainer construye FirebaseOrderChatRepository y RoomChatDraftStore. Los listeners solo se abren al consultar el chat y se cancelan al cerrarlo, cambiar de canal/pedido/cuenta o destruir el ViewModel.

El negocio permite envío en ACCEPTED/AT_MERCHANT y queda en lectura tras recoger. El cliente permite envío hasta finalizar. El resumen de un pedido entregado conserva acceso al chat en lectura; iniciar otro pedido no reutiliza su conversación. Una cancelación limpia el chat activo. Las restricciones se aplican en ViewModel, repositorio y reglas de Firestore, no solo mediante botones.

El dictado modifica el borrador para revisión y nunca envía automáticamente. Se maneja el fallo al abrir el reconocedor externo. El mecanismo sigue siendo RecognizerIntent, no SpeechRecognizer embebido.

## Almacenamiento

Mensajes: `riders/{uid}/deliveryRecords/{orderId}/chats/{MERCHANT|CUSTOMER}/messages/{messageId}`.

Campos: `orderId`, `channel`, `senderId`, `senderRole`, `text`, `createdAt`. El timestamp lo establece el servidor. El nombre visible se deriva del rol; un timestamp aún desconocido se muestra como pendiente, no como 1970. Los documentos de canal no necesitan campos para contener la subcolección.

Borradores: Room, `chat_drafts.db`, tabla `chat_drafts`, clave `uid/orderId/channel`. Contiene ID estable del mensaje y texto. Se serializan los guardados; borrar tras envío exige coincidencia de ID y contenido para no eliminar texto más reciente. La base se excluye de copias de seguridad automáticas, igual que otros borradores del app.

Los mensajes ya confirmados se recuperan de Firebase y de su caché nativa. La UI distingue información guardada pendiente de actualización. Las respuestas entran mediante listener; no se generan automáticamente.

## Envío y errores

Los envíos requieren conexión: una transacción confirma la etapa remota y crea el mensaje. No existe envío automático diferido de chat con WorkManager. Sin conexión o ante error se conserva el borrador; el repartidor decide cuándo reintentar y las restricciones de etapa siguen vigentes.

El ID de mensaje permanece estable al reintentar el mismo contenido. Si ya existe, se comprueba contenido y autor y no se crea otro documento. Si una confirmación ambigua llega después y el usuario edita el texto, se utiliza otro ID cuando el mensaje confirmado ya está observado. No se inventa un éxito por timeout. El borrador se elimina únicamente después de completar el envío y su limpieza local.

## Permisos y la otra parte

El repartidor solo puede consultar sus conversaciones y crear mensajes con su propio ID y rol RIDER. No puede editar/borrar mensajes ni suplantar MERCHANT/CUSTOMER. La recepción de otros roles está preparada; los permisos para la futura web y el puente con Chaski Food todavía no están implementados. Las respuestas técnicas se inyectaron exclusivamente en un pedido aislado mediante credenciales administrativas del CLI, no desde la app del repartidor.

El usuario eligió una sola cuenta de pruebas para representar a los negocios simulados en la futura web. Esa cuenta deberá tener acceso únicamente a los canales MERCHANT autorizados para esa simulación. No se creó la cuenta ni se publicó una web en este bloque. No se usará como cuenta de soporte ni se le concederá acceso al canal del cliente.

El cliente de Chaski Food pertenece a otro proyecto Firebase. Almacenar mensajes en el Firebase de Rider no significa que ese cliente ya pueda recibirlos. Faltan su autenticación/autorización y el puente de integración. No se inventaron IDs de clientes. Soporte mantiene su repositorio, tickets y canales independientes.

## Reglas y verificación

Se añadió únicamente el bloque de permisos de mensajes a las reglas que estaban publicadas, preservando pedidos/tracking y los demás permisos. Ruleset verificado por lectura exacta: `projects/chaski-rider/rulesets/31a2d155-a792-45e8-91cb-477632d21a26`. No se publicaron funciones, Storage ni reglas de soporte pendientes.

77 pruebas unitarias aprobadas. Cuatro pruebas de reglas aprobadas contra la vista previa exacta de producción: aislamiento, identidad/inmutabilidad, cierre por etapa y lectura de respuesta confiable.

Seis pruebas Android aprobadas en el celular: chat real con Firebase, borradores Room y las cuatro pruebas de regresión del pedido local. El chat real comprobó envío, reintento idempotente sin duplicado, recuperación en otro ViewModel, recepción de respuesta técnica del establecimiento, bloqueo de negocio tras recoger, envío al canal cliente y cierre al entregar. La prueba Room comprobó reapertura, separación de canales/usuarios y protección frente a una limpieza antigua. Se revisó una captura real de la conversación.

Los pedidos técnicos no reemplazan el pedido activo del usuario. La respuesta técnica no representa una integración ya completada con la web del establecimiento.
