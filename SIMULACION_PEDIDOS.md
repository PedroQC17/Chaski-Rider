# Simulación de pedidos en Android

Chat actualizado: los canales del pedido ahora utilizan Firebase y borradores Room, con cierre de escritura por etapa y sin mensajes iniciales ficticios. La web del establecimiento y el puente con Chaski Food siguen pendientes. Véase [HU10_CHAT.md](HU10_CHAT.md).

## Identidad e historial sincronizable — 27 de octubre de 2026

Las ofertas nuevas ya no reutilizan el ID de la ruta como ID del pedido: generan un UUID por ejecución. La ruta solo queda en el almacenamiento de simulación para controlar repeticiones. Room conserva el pedido activo y una tabla de historial/registros pendientes; WorkManager envía el último estado a `riders/{uid}/deliveryRecords/{orderId}` cuando hay conexión y sesión del propietario. Las reglas de esta ruta se publicaron de forma aislada. No se envía ubicación del repartidor, no hay integración con Chaski Food ni sincronización de los chats en este bloque. Véase [IDENTIDAD_Y_REGISTRO_PEDIDOS.md](IDENTIDAD_Y_REGISTRO_PEDIDOS.md).

## Actualización HU08 — 27 de octubre de 2026

La primera confirmación de llegada guarda `arrivedAt` y `arrivalLocation` (coordenadas medidas del repartidor, no el pin del negocio). Repetir la confirmación no cambia el primer registro. Ambos datos se conservan mediante el estado JSON de Room, sin cambiar la tabla. Los registros antiguos sin coordenadas se mantienen sin inventar una posición histórica.

Después de aceptar aparece Navegar al local. Tras recoger cambia a Navegar al cliente; al finalizar desaparece. El botón abre Google Maps mediante un Intent con las coordenadas de la etapa actual. Si Maps no está instalado, intenta abrir una URL de indicaciones; si no hay aplicación compatible muestra un error. No se fuerza un modo de transporte. La ruta y las indicaciones pertenecen a la aplicación externa; no se incorporan ruta vial, ETA vial ni recálculo dentro del mapa de Chaski.

Abrir navegación no registra llegada, recojo ni entrega. Al volver, el flujo solicita nuevamente ubicación; cada confirmación sigue requiriendo GPS reciente y cercanía de 100 m. No se añade backend, publicación ni sincronización remota de pedidos.

Comprobación manual: aceptar, abrir navegación y revisar que el destino sea el local; volver y confirmar llegada dentro de 100 m; recoger y revisar que el botón dirija al cliente; entregar y comprobar que desaparece. Comprobar también el retorno sin cambiar la etapa y el comportamiento sin Google Maps instalado.

Verificación de esta actualización: 66 pruebas unitarias aprobadas sin fallos; APK debug y APK de pruebas Android compilados. Se ampliaron las pruebas Android para comprobar la recuperación de coordenadas desde Room y el cambio de botón por etapa, pero no se ejecutaron: ADB no detectó un dispositivo conectado. La apertura de Maps, el retorno y la alternativa web quedan pendientes de comprobación en celular.

La app usa un único flujo local de pedidos. Registro, disponibilidad y cuenta siguen usando Firebase. No se modificó ni desplegó el backend de pedidos y no se agregó un panel administrativo.

## Uso

1. Entrar con una cuenta aprobada que tenga foto y conectarse desde Inicio.
2. Permitir ubicación y activar GPS. Al conectarse con la app visible se inicia un servicio de ubicación con notificación permanente. La primera búsqueda ocurre a los 5 segundos y las siguientes cada 20 segundos; continúa al minimizar.
3. Si existen pedidos elegibles a menos de 5 km, hay un 70 % de probabilidad de ofrecer el más cercano. Se mide la distancia al recojo en línea recta; no representa un recorrido vial.
4. Aceptar o rechazar antes de 45 segundos. Un pedido ofrecido no vuelve a aparecer durante 5 minutos. No se inventa un pedido distante si el catálogo no cubre la ubicación.
5. Al aceptar, el mapa conserva el recojo (1) y la entrega (2). El pin azul corresponde al GPS real. No se dibujan rutas. Los controles de llegada y recojo requieren GPS a 100 m o menos del local; entrega requiere estar a 100 m o menos del destino. Para probar sin desplazarse se simula la ubicación del dispositivo, sin saltar la geocerca.
6. El pedido y sus tiempos se guardan por UID en Room antes de confirmar cada acción. Actualizar, rotar o recrear el proceso conserva el estado guardado. Una oferta expirada no se restaura como vigente.

La generación local continúa en un servicio de primer plano aunque la pantalla esté minimizada. Se detiene al desconectarse, cerrar sesión o perder elegibilidad. No hay despacho remoto real. El arranque de sesión sin red continúa dependiendo del flujo de autenticación existente.

## Datos y arquitectura

- `app/src/main/assets/demo_orders_lima.json`: 186 recorridos locales entre 62 negocios publicados en el directorio oficial de plazaVea, en 32 distritos de Lima y Callao. Direcciones y coordenadas conservan la pareja publicada por la fuente; ver procedencia al final.
- `data/orders/DemoOrderCatalog.kt`: lectura, validación y selección por cercanía.
- `data/orders/LocalDemoOfferRepository.kt`: transiciones, vencimiento, variedad y cotización coherente. Un pedido activo impide recibir otro. No hay agrupación de varios pedidos en esta entrega.
- `data/orders/DemoOrderDatabase.kt`: persistencia independiente `demo_orders.db`, sin cambiar la base de registro. El esquema Room se exporta y la base se excluye del backup.
- `ui/screens/orders/OrdersViewModel.kt` y `OrdersUiState.kt`: búsqueda con GPS, estado de pantalla, temporizadores y recuperación. Los errores conservan el último estado confirmado; cerrar un aviso no avanza el pedido.
- `OrdersMap.kt`, `OrderQuoteCard.kt`, `OrdersScreen.kt`: mapa y acciones por etapa. Inicio y pedidos usan `res/raw/map_grayscale.json`.
- `AppNavigation.kt`: un solo ViewModel para pedidos, navegación al recibir oferta o recuperar un pedido activo. La antigua ruta de demo se conserva como alias para las notificaciones existentes, sin duplicar opciones en el menú.

Las anotaciones nuevas corresponden al contrato obligatorio de Room; no se agregaron frameworks o anotaciones auxiliares.

## Referencias de diseño

El flujo aceptar → recoger → entregar toma como referencia funcional la [guía de primera entrega de Deliveroo](https://rider.deliveroo.co.uk/first-order). No se copia su interfaz ni se afirma que Chaski tenga despacho real. El fondo gris usa el [estilo JSON admitido por Maps para Android](https://developers.google.com/maps/documentation/android-sdk/styling).

Las tarifas, probabilidades, radio, tiempos y compensación son reglas de demostración. La espera se paga ficticiamente después de 5 minutos a 20 céntimos por minuto adicional y se detiene al recoger.

## Comprobación local

- Compilación debug y APK de pruebas correctos. 40 pruebas JVM sin fallos, incluyendo 15 nuevas de catálogo, transiciones, aislamiento, cotización y ViewModel; las 25 anteriores de registro continúan pasando.
- 2 pruebas instrumentadas en Pixel 7 API 36: recorrido de aceptar a entregar conservando el mismo pedido, y cierre/reapertura de Room con aislamiento entre cuentas. El panel permanece abierto y sus acciones quedan fuera del contenido desplazable.
- Las pruebas usan coordenadas controladas. La selección en la app usa DeviceLocationRepository; no se simuló la ubicación de un teléfono real. Se descartan posiciones del proveedor con más de 30 segundos de antigüedad.
- Se inspeccionaron capturas locales de oferta, aceptación y entrega. Los botones principales quedan fuera del contenido desplazable y el mapa reserva espacio para la tarjeta. El mapa se encuadra cuando está listo para mostrar marcadores, sin esperar a descargar las calles.
- Las capturas están en `captures/orders-20261012/`, excluido de Git. Un primer intento tuvo un aviso de System UI del emulador; las capturas posteriores de la app y la repetición del recorrido se realizaron sin ese aviso en las imágenes.
- No se instalaron cambios en un teléfono físico, no se publicaron funciones o reglas y no se modificaron usuarios reales.


## Ajuste de interfaz y ubicaciones del 12 de octubre

La pantalla muestra Pedidos, sin flechas para minimizar, aviso de simulación ni identificador interno. RECOJO y ENTREGA aparecen en bloques distintos con números y colores iguales a los pines. El menú lateral solo se abre mediante el botón; sus gestos están desactivados.

El catálogo ahora contiene 186 recorridos entre 62 locales públicos en 32 distritos. Nombre, dirección y coordenadas se obtuvieron juntos del directorio oficial de plazaVea el 12/10/2026:
https://m.plazavea.com.pe/nuestras-tiendas
https://m.plazavea.com.pe/controller.aspx?ope=tiendas&p1=-1&p2=-1&p3=-1

Son referencias publicadas por ese directorio; no se verificaron presencialmente y pueden estar desactualizadas. Los destinos también son direcciones de locales públicos, no domicilios de clientes reales. No implica relación comercial con estos negocios. No se inventan números de calle ni se desplazan las coordenadas para crear variantes. Cada recojo tiene tres destinos distintos cercanos. El GPS real sigue seleccionando la oferta y Room conserva el recorrido aceptado. Los pedidos ya guardados mantienen su información original hasta finalizar o cancelar.

Los botones ahora dicen Llegué al local, Confirmar recojo y Confirmar entrega. La ejecución sigue siendo local y permite avanzar sin desplazarse; no hay despacho real ni cambios en Firebase o backend.

Verificación del ajuste: 40 pruebas JVM y 2 pruebas instrumentadas pasaron. Se inspeccionaron las capturas de oferta y pedido aceptado en `captures/orders-ui-20261012/`. El primer intento instrumentado se interrumpió durante el arranque del emulador; la repetición completó ambas pruebas en 44 segundos. Los últimos cambios posteriores a esa prueba solo ajustan el texto del marcador de ubicación y del local por defecto.


## Recepción al minimizar

`OrderSearchService` mantiene un servicio de primer plano de tipo location mientras el repartidor está disponible, aprobado, habilitado y con foto. Se inicia desde la actividad visible con permiso de ubicación; no solicita ACCESS_BACKGROUND_LOCATION ni intenta arrancar desde un boot receiver. La notificación permanente indica Estás conectado. Las nuevas ofertas usan el canal de alta importancia existente, y al tocar el aviso se abre Pedidos también en release.

`OrderSearchEngine` recupera Room, obtiene GPS solo cuando corresponde buscar y comprueba cuenta y disponibilidad otra vez antes de generar la oferta. No necesita una Activity ni un ViewModel. La pantalla conserva UiState y ViewModel; en producción su buscador propio está desactivado para tener un solo buscador automático. Recibe avisos internos de cambios mientras está visible y se recupera de Room al volver.

Minimizar no equivale a forzar detención: el servicio es START_NOT_STICKY y no se promete ejecución después de forzar cierre, reiniciar o terminar el proceso. Android puede interrumpir el servicio. El usuario debe habilitar notificaciones para ver las ofertas y mantener GPS disponible. La lógica sigue siendo una simulación local, sin nuevas funciones o escrituras de revisión en Firebase. La ubicación del mapa de un pedido activo sigue actualizándose con la pantalla visible.

Referencia oficial: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start


Verificación de segundo plano: compilación debug y APK de pruebas correctos, 46 pruebas JVM sin fallos. En Pixel 7 API 36 pasaron las dos pruebas previas de recorrido y Room; después del ajuste del aviso inmediato pasó la prueba específica del servicio (8,339 s): inicio desde actividad visible, salida al launcher de Android, recepción de una actualización de búsqueda sin pantalla y detención de la notificación al desconectarse. La prueba usa un perfil solo en memoria y no escribe aprobaciones ni perfiles en Firestore. No se probó aún este cambio en un teléfono físico. El primer intento en frío del emulador se interrumpió y el siguiente detectó la demora predeterminada del aviso de servicio; se fijó FOREGROUND_SERVICE_IMMEDIATE y la comprobación posterior pasó.


## Geocercas y marcador de moto

Se reemplaza la excepción anterior de avanzar sin viajar. El catálogo sigue siendo local y la ubicación puede simularse desde el dispositivo; no se rechazan ubicaciones mock.

La regla compartida OrderLocationRules valida una distancia Haversine sin redondear de hasta 100 metros. Llegada, pedido no listo y recojo requieren cercanía al local; entrega requiere cercanía al primer destino del pedido (el flujo actual tiene una entrega). Las transiciones previas siguen siendo obligatorias. Confirmar llegada no recoge el pedido automáticamente y confirmar recojo no entrega automáticamente.

La UI deshabilita botones y explica qué destino acercar. Cada confirmación consulta nuevamente LocationRepository, en vez de confiar en la posición que aparece en el mapa. El repositorio local comprueba las coordenadas antes de guardar en Room. Sin GPS, lejos del punto o con coordenadas inválidas el paso no cambia. Alejarse después de llegar bloquea el recojo. El GPS perdido elimina las distancias y banderas de proximidad; no se guarda proximidad en Room. Al volver se verifica otra vez.

DeviceLocationRepository comprueba que la ubicación del sistema esté activa, solicita alta precisión y descarta posiciones de más de 30 segundos. RiderMapMarker dibuja una moto con conductor sobre un círculo azul, compartida entre Inicio y Pedidos. Los pines numerados de recojo y entrega mantienen sus colores y prioridad. No se añade ninguna ruta ni dependencia gráfica.


Verificación de geocercas: 50 pruebas JVM sin fallos, incluyendo borde de 99,99 m frente a 100,4 m, coordenadas inválidas, GPS perdido, uso de GPS nuevo en la confirmación y rechazo de cambios fuera del punto. Tres pruebas instrumentadas pasaron juntas en API 36 (66,675 s): botones bloqueados lejos del recojo, habilitados en el local, nuevamente bloqueados al alejarse después de llegar, recojo al volver, entrega habilitada solo en destino; persistencia de llegada en Room; servicio manteniendo búsqueda detrás del launcher y deteniéndose al desconectar. Se usó un GPS controlado en las pruebas del recorrido. El primer inicio en frío del emulador se interrumpió antes de las pruebas; la repetición pasó.

Se inspeccionó `captures/orders-geofence-20261012/orders-accepted.png`, con aviso de geocerca y botón deshabilitado a 536 m. Después de las pruebas solo se ajustó el dibujo del tanque y motor del icono; se generó nuevamente el APK. Los cambios siguen limitados a Android, sin desplegar backend ni escribir revisiones en Firebase.


## Cancelaciones e incidencias

Antes del recojo, Cancelar pedido abre un formulario con motivo y detalle (10 a 500 caracteres), explica la consecuencia y exige confirmar. El repositorio valida la etapa y guarda el registro en Room junto con el cambio de estado. La ruta cancelada queda excluida durante cinco minutos. Volver al pedido no cancela nada.

Después del recojo no hay cancelación directa. Tengo un problema permite registrar cliente sin respuesta, dirección, vehículo u otro motivo. El pedido sigue activo y conserva su reporte al reabrir la app. La entrega solo se confirma manualmente cerca del destino. Un reporte existente no se duplica. RESET solo cierra pedidos ya entregados (o un estado vacío); no puede borrar un pedido activo.

Las incidencias se guardan localmente y están aisladas por usuario. Ahora se pueden escalar con Abrir ticket de soporte: el formulario vincula la incidencia y su pedido y envía el ticket a Firestore con conexión. Las respuestas se registran manualmente desde Firebase. Véase AYUDA_SOPORTE.md; requiere publicar las reglas nuevas de soporte. La resolución administrativa de un pedido recogido no se automatiza en esta entrega. Los formularios se conservan en el ViewModel al rotar; solo los reportes confirmados quedan persistidos al cerrar el proceso.

Prueba manual: cancelar antes del recojo y volver sin confirmar; confirmar con motivo y comprobar que desaparece el pedido; recoger otro pedido y registrar una incidencia; cerrar y abrir la app para comprobar que el pedido y el reporte siguen activos. No se ofrece una opción para fingir una respuesta o autorización de soporte.

Validación de esta entrega: 54 pruebas unitarias, cero fallos; compilación debug y APK correctos. Las nuevas pruebas cubren bloqueo de RESET activo, cancelación posterior al recojo, historial por usuario, reporte sin duplicados y conservación del pedido y del formulario cuando falla el guardado. Falta comprobar visualmente el nuevo formulario en un dispositivo.
