# Revisión del backlog frente al código actual

## Corte actualizado después de tracking y chat

Esta sección es el diagnóstico vigente. La tabla inferior conserva la revisión anterior a esas implementaciones y no debe usarse como estado actual de HU09/HU10/WorkManager.

| Sprint | Estado actual frente al Excel |
|---|---|
| 1: HU01-HU04 | Base técnica, autenticación, perfil y disponibilidad implementados; cierre literal pendiente por automóvil, uso activo de Retrofit en un flujo REST y despacho remoto de ofertas por FCM. No basta incluir dependencias para demostrar el requisito. |
| 2: HU05-HU07 | Ofertas y cotización simuladas funcionales. Sin fuente funcional de demanda, reserva multiusuario/asignación real por vehículo, ni cálculo vial propio. El origen de pedidos sigue siendo el catálogo local. |
| 3: HU08-HU10 | Implementación Android de llegada/recojo, tracking y chat persistente probada. Llegada guarda hora y coordenadas en Room; tracking se publica solo después del recojo, continúa minimizado y se elimina al finalizar. Chat tiene borradores, remitente/hora, dictado y restricciones por etapa. Navegación/ruta/recálculo se delegan a Google Maps: requiere aceptar esa interpretación de filas 52-53/59-60. Falta el interlocutor web del negocio y la integración real del cliente de Chaski Food. El dictado utiliza RecognizerIntent, no SpeechRecognizer embebido. |
| 4: HU11-HU13 | Entrega básica, incidencias locales y soporte Android. Faltan PIN/evidencia según las condiciones del pedido, registro económico completo, ampliación de ayuda/emergencia, publicación/verificación de soporte y web de atención. La sincronización de estados de pedido no incluye por sí sola el detalle de todas las incidencias. |
| 5: HU14-HU16 | Room y WorkManager con reintentos ya implementados y probados con pérdida real de red. Faltan ganancias/liquidaciones y dashboard. Para cerrar HU16 literalmente, revisar fila 105: la cola actual conserva la última revisión por pedido y no cada acción individual con su propio ID; los detalles de espera/incidencias no integran todavía todos los registros remotos. |
| 6: HU17-HU20 | Pendientes las tres funciones IA y verificación de release/distribución en Google Play. |

Los tres procesos de negocio exigidos incluyen el económico, todavía pendiente. Material, Authentication, MVVM/UiState, corrutinas, GPS/cámara/dictado, Room/SQLite y WorkManager tienen implementación. El uso de Firebase/SDK no demuestra por sí solo consumo de APIs REST mediante Retrofit; conviene integrarlo cuando exista una API necesaria, no añadir una llamada sin utilidad.

La última entrega pasó 77 pruebas unitarias, cuatro pruebas nuevas de reglas de chat y seis pruebas Android de chat/persistencia/regresión de pedidos. La entrega anterior de tracking pasó además sus pruebas en celular y reglas. Esto no certifica todas las historias del Excel. Esta revisión no ejecutó pruebas nuevas ni modificó el workbook ni la lógica Android.

Prioridad siguiente: terminar la web del negocio con una única cuenta operadora de pruebas (decisión del usuario), manteniendo soporte separado. Después cerrar HU11-HU13 y abordar el proceso económico/dashboard. La integración con Chaski Food sigue dependiendo del otro equipo y no se sustituye por una simulación silenciosa.

## Diagnóstico histórico anterior a las últimas entregas

Actualización posterior a la revisión: se añadió `arrivalLocation` al primer registro de llegada y navegación externa por botón, autorizada por el usuario. La navegación cambia de local a cliente después del recojo. Se verificaron 66 pruebas unitarias y compilación de app y APK de pruebas. Las observaciones inferiores sobre coordenadas pendientes corresponden al diagnóstico inicial; esta brecha ya fue corregida para nuevas llegadas. Sigue pendiente probar Maps en dispositivo y confirmar si la navegación externa satisface el criterio académico de ruta/recálculo. HU09 y HU10 conservan las demás brechas indicadas.

Fecha: 27/10/2026. Fuente: `C:/Users/Angel/Downloads/ChaskiRiders-backlogpropuesta (2).xlsx`, hojas PLANIFICACION y REQUISITOS.

Alcance: revisión estática del código Android y contraste con los criterios del Excel. No se ejecutaron nuevas pruebas, no se verificó el despliegue de Firebase y no se modificó el backlog ni la lógica de la app. Las verificaciones de pruebas de conversaciones anteriores no equivalen a pruebas nuevas de todos los criterios.

## Conclusión

La app mejoró su flujo local de reparto, pero no corresponde declarar cerrados todos los criterios hasta sprint 3. Existen además avances de sprint 4 (incidencias y soporte) y de sprint 5 (persistencia local con Room).

## Cobertura

| Sprint | Historia | Situación observada |
|---|---|---|
| 1 | HU01 Arquitectura | UI, UiState, ViewModel, repositorios, dominio/datos, corrutinas y Material implementados. Existe Retrofit en RealOfferRepository, pero el flujo de pedidos seleccionado es local; no demuestra por sí mismo consumo REST activo en HU07-HU09. Manejo completo de fallos requiere verificación funcional. |
| 1 | HU02 Acceso | Firebase Authentication, Google, acceso por correo/contraseña vinculada, sesión y restricciones implementados. La revisión estática no certifica todos los casos en producción. |
| 1 | HU03 Perfil/documentos | Borradores Room, cámara, previsualización y representación de revisión documental implementados. Automóvil exigido por fila 21 no está permitido en selección ni validación. Revisión administrativa actualizada requiere comprobar despliegue. |
| 1 | HU04 Disponibilidad | Disponibilidad remota y búsqueda local condicionada por habilitación/disponibilidad; notificaciones locales y receptor FCM. No confundir la notificación local de oferta con un despacho remoto de ofertas por FCM. |
| 2 | HU05 Demanda | Existe infraestructura de zonas, pero el repositorio seleccionado no suministra demanda. No hay una fuente de demanda funcional demostrada. |
| 2 | HU06 Ofertas | Catálogo JSON, proximidad, exclusiones temporales, aceptación/rechazo y bloqueo de un segundo pedido local. No hay reserva compartida entre repartidores ni asignación por compatibilidad de vehículo en este catálogo. |
| 2 | HU07 Información y pago | Negocio/direcciones, desglose y total de la simulación. Distancias geográficas y tiempos estimados locales; no equivalen a rutas viales ni tarifas confirmadas por backend. |
| 3 | HU08 Recojo | Geocerca 100 m, GPS reciente, hora de llegada, pedido no listo y espera/compensación local. Faltan ruta/recálculo y persistir coordenadas del evento de llegada; snapshot guarda arrivedAt pero no posición de llegada. |
| 3 | HU09 Traslado/tracking | Hora de recojo, etapa en camino, GPS local periódico, distancia al cliente y parada del bucle al terminar. No hay ubicación compartida remotamente ni servicio foreground de tracking durante entrega; OrderSearchService es de búsqueda y no reemplaza ese seguimiento. Faltan navegación/recálculo y recuperación del tracking compartido. |
| 3 | HU10 Chat/dictado | Canales, remitente/hora y dictado con revisión implementados. ChatViewModel conserva mensajes en memoria y agrega mensajes de ejemplo. No hay repositorio remoto ni persistencia del chat de cliente/negocio. sendMessage no valida permisos de etapa por sí mismo. El soporte remoto es otro flujo. |
| 4 | HU11 Entrega | Entrega por proximidad y hora final local. Sin flujo de PIN/evidencia exigidos condicionalmente ni registro económico histórico completo. |
| 4 | HU12 Incidencias | Motivos, descripción, confirmación, registro local y cancelación antes de recojo. Después del recojo conserva pedido y deriva a soporte. Falta demostrar cumplimiento remoto extremo a extremo. |
| 4 | HU13 Soporte | UI, ViewModel, borradores Room y repositorio Firestore de tickets/respuestas implementados. Deben verificarse reglas/despliegue y respuesta manual real. Contexto usa etapa pickedUp, orderId y problemId; revisar fidelidad de categoría y estado frente a filas 84-85. No se evidencia acción de emergencia. |
| 5 | HU14 Ganancias | Cotización y desglose no equivalen a historial de ganancias/liquidaciones con filtros. Historia pendiente. |
| 5 | HU15 Dashboard | Sin dashboard diario/semanal implementado. |
| 5 | HU16 Offline/sincronización | Room guarda pedido y borradores. No existe cola de acciones pendientes sincronizada con WorkManager; persistencia local no completa esta historia. |
| 6 | HU17-HU19 IA | No se evidencian las tres funciones de IA exigidas. Dictado no sustituye esas historias. |
| 6 | HU20 Publicación | No se verificó publicación en Google Play ni cierre de criterios release. APK debug no demuestra este requisito. |

## Decisiones del usuario que difieren del documento

- Mantener pedidos simulados es una decisión de alcance. No permite afirmar que hay asignación multiusuario real.
- El usuario pidió mapa sin polilínea. PLANIFICACION filas 52-53 y 59-60 pide ruta y recálculo. Debe acordarse si navegación externa u otro alcance satisface a la profesora; no se revierte la preferencia visual automáticamente.
- No construir consola administrativa. El soporte se responde manualmente desde Firebase; esto no impide implementar el lado Android.
- Automóvil aparece en el Excel pero la implementación actual admite bicicleta y motocicleta. Resolver por cambio de alcance acordado o implementación, no declarar cumplimiento implícito.

## Orden recomendado

1. Cerrar criterios de HU08: registro de coordenadas del evento de llegada y decisión explícita sobre navegación sin dibujar una ruta en el mapa principal.
2. Cerrar HU09: separar tracking del pedido de búsqueda de ofertas; definir persistencia, publicación remota autorizada y continuidad durante el reparto.
3. Cerrar HU10: chat persistente y remoto, validación de canales/etapas y prueba del interlocutor. No requiere crear una app administrativa para una prueba manual en Firebase.
4. Revisar diferencias arrastradas de sprint 1-2 y acordar cuáles pertenecen a la simulación aceptada.
5. Verificar soporte en Firebase real y continuar sprint 4 antes de ganancias/dashboard/IA.

## Requisitos transversales

Hay implementación de Authentication, Material, MVVM con UiState, corrutinas, Room/SQLite y recursos GPS/cámara/dictado. Retrofit está disponible, pero su presencia no certifica el consumo del flujo de pedidos actual. WorkManager, dashboard, tres funciones de IA, proceso económico completo y publicación en Play siguen pendientes o sin evidencia de cierre.
