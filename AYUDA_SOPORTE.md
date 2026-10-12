# Ayuda y soporte

Ayuda se abre desde el menú lateral, Perfil y el estado del registro. Una incidencia registrada durante un pedido incluye un botón para abrir un ticket con el local, las direcciones, la etapa y el texto ya vinculados. Los identificadores internos no se muestran al repartidor.

El recorrido es categoría → tema → orientación → Necesito más ayuda → descripción → Crear ticket. Mis tickets muestra abiertos y cerrados. El repartidor puede añadir información a un ticket abierto y leer las respuestas reales registradas por soporte. No hay respuestas automáticas ni un panel de administración.

## Persistencia y arquitectura

- `SupportScreen` presenta `SupportUiState`; `SupportViewModel` controla la navegación interna, la sesión, el formulario y las corrutinas.
- `support_drafts.db` guarda en Room el borrador del ticket y los mensajes todavía no confirmados, separados por usuario y ticket. Se excluye de las copias de seguridad. Las escrituras se ordenan y el envío espera a guardar el borrador.
- `FirebaseSupportRepository` usa Firestore: `riders/{uid}/supportTickets/{ticketId}` y su subcolección `messages`. No necesita Retrofit: el SDK de Firebase ya proporciona estas operaciones.
- Las transacciones requieren conexión y evitan duplicados usando identificadores estables conservados en Room. Solo se limpia el borrador tras confirmar la transacción. El envío espera como máximo 15 segundos; un tiempo agotado conserva el texto y permite revisar el ticket antes de reintentar. No hay envío automático en segundo plano.
- Los listeners muestran los tickets y las respuestas, incluyendo la caché del SDK cuando está disponible. Al cambiar de cuenta, se cancela el trabajo de la cuenta anterior y se limpia su estado visible.
- Un borrador anterior no se reemplaza automáticamente al abrir una incidencia de otro pedido. Descartarlo requiere confirmación.

Abrir, responder o cerrar un ticket **no cancela ni completa el pedido** ni modifica aprobaciones, ganancias o documentos. El contexto del pedido es informativo y procede de la simulación local. La resolución operativa de devoluciones o cancelaciones autorizadas tras el recojo queda fuera de esta entrega.

## Firebase y prueba manual

La integración necesita publicar el bloque de reglas de soporte antes de enviar tickets desde el APK. En esta entrega se editan y prueban las reglas locales; no se despliegan ni se modifican datos del proyecto real. El archivo del backend contiene además cambios anteriores pendientes: no publicarlo entero sin revisar esos cambios.

Consulta `../Chaski_Rider_Backend/SOPORTE_FIREBASE.md` para responder manualmente desde la consola. Crear un mensaje con `sender = SUPPORT` y una fecha `createdAt` lo muestra en la conversación; cambiar `status` a `CLOSED` impide nuevas respuestas del repartidor.

Pruebas manuales:

1. Abrir Ayuda desde Perfil o el menú; elegir categoría y tema, leer la orientación y crear un ticket.
2. Escribir un borrador, salir y volver; cerrar y abrir la app; comprobar el texto conservado. Cambiar de cuenta y comprobar que no aparece el borrador anterior.
3. Sin conexión, intentar enviar: conservar el texto y mostrar el error. Con conexión, reintentar y comprobar que aparece un solo ticket.
4. Responder desde la consola; verificar el mensaje en la app y enviar información adicional como repartidor.
5. Cerrar el ticket desde la consola; comprobar que desaparece el control de envío.
6. En un pedido recogido, registrar una incidencia y abrir su ticket. Comprobar el contexto y que el pedido sigue activo.

Los fixtures de respuesta en las pruebas Android son datos de prueba; el código de producción no inventa mensajes de soporte.

Referencias: [transacciones de Firestore](https://firebase.google.com/docs/firestore/manage-data/transactions), [listeners](https://firebase.google.com/docs/firestore/query-data/listen) y [reglas de acceso](https://firebase.google.com/docs/firestore/security/rules-conditions).
# Verificación del 12 de octubre de 2026

Compilación limpia de la app y del APK de pruebas completada. Las 64 pruebas unitarias pasaron sin fallos. Las dos pruebas de soporte en el celular conectado también pasaron: recuperación y aislamiento de borradores con Room, y flujo de ticket, mensajes, respuesta de soporte y cierre. Se revisaron las capturas de las tres pantallas.

Las seis pruebas de reglas de soporte pasaron en Firebase local. La prueba de interfaz usa un repositorio de prueba y no escribe datos en Firebase real. Sigue pendiente publicar el bloque de reglas de soporte y comprobar un ticket con una respuesta manual en el proyecto real.

