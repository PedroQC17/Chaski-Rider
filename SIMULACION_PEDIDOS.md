# Simulación de pedidos en Android

La app usa un único flujo local de pedidos. Registro, disponibilidad y cuenta siguen usando Firebase. No se modificó ni desplegó el backend de pedidos y no se agregó un panel administrativo.

## Uso

1. Entrar con una cuenta aprobada que tenga foto y conectarse desde Inicio.
2. Permitir ubicación y activar GPS. Con la app abierta, la primera búsqueda ocurre a los 5 segundos y las siguientes cada 20 segundos.
3. Si existen pedidos elegibles a menos de 5 km, hay un 70 % de probabilidad de ofrecer el más cercano. Se mide la distancia al recojo en línea recta; no representa un recorrido vial.
4. Aceptar o rechazar antes de 45 segundos. Un pedido ofrecido no vuelve a aparecer durante 5 minutos. No se inventa un pedido distante si el catálogo no cubre la ubicación.
5. Al aceptar, el mapa conserva el recojo (1) y la entrega (2). El pin azul corresponde al GPS real. No se dibujan rutas. Los controles de llegada, recojo y entrega permiten avanzar explícitamente la simulación sin desplazarse.
6. El pedido y sus tiempos se guardan por UID en Room antes de confirmar cada acción. Actualizar, rotar o recrear el proceso conserva el estado guardado. Una oferta expirada no se restaura como vigente.

La generación ocurre mientras la app está en primer plano. No se implementó despacho en segundo plano ni recepción real de pedidos. El arranque de sesión sin red continúa dependiendo del flujo de autenticación existente.

## Datos y arquitectura

- `app/src/main/assets/demo_orders_lima.json`: 105 pedidos ficticios, 35 zonas de Lima y Callao. Nombres, puntos y referencias son de prueba; no son negocios o domicilios de clientes verificados.
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
- 2 pruebas instrumentadas en Pixel 7 API 36: recorrido de aceptar a entregar conservando el mismo pedido, y cierre/reapertura de Room con aislamiento entre cuentas. La pantalla también permite minimizar y expandir el panel.
- Las pruebas usan coordenadas controladas. La selección en la app usa DeviceLocationRepository; no se simuló la ubicación de un teléfono real. Se descartan posiciones del proveedor con más de 2 minutos de antigüedad.
- Se inspeccionaron capturas locales de oferta, aceptación y entrega. Los botones principales quedan fuera del contenido desplazable y el mapa reserva espacio para la tarjeta. El mapa se encuadra cuando está listo para mostrar marcadores, sin esperar a descargar las calles.
- Las capturas están en `captures/orders-20261012/`, excluido de Git. Un primer intento tuvo un aviso de System UI del emulador; las capturas posteriores de la app y la repetición del recorrido se realizaron sin ese aviso en las imágenes.
- No se instalaron cambios en un teléfono físico, no se publicaron funciones o reglas y no se modificaron usuarios reales.
