# HU11 — regla acordada y corrección del 02/11/2026

Solo pedidos pagados por el cliente exigen PIN válido de cuatro dígitos. Ninguno exige foto y una foto no sustituye al PIN. La pantalla de entrega actual no ofrece captura de evidencia. Los campos antiguos se conservan para compatibilidad de datos, no como requisitos.

La simulación del catálogo mezcla pedidos pagados y no pagados mediante `paidByCustomer`. Es información simulada, no confirmación de un cobro real. Cuando se integre Chaski Food, el estado de pago deberá proceder de su backend autorizado: una app de repartidor no es una autoridad de pagos.

## Probar como cliente desde Firebase

1. Aceptar un pedido nuevo. Si está pagado, al sincronizarlo WorkManager solicita al backend preparar su PIN.
2. En la consola Firestore, buscar `deliveryPins/{uid}_{orderId}`. Solo un operador con acceso administrativo puede consultar este documento.
3. El campo `pin` es un string de cuatro dígitos, incluidos ceros iniciales. Consultarlo para representar al cliente; no cambiarlo manualmente.
4. Cerca del destino, introducir ese código en Rider y confirmar. La app no recibe el PIN esperado: envía únicamente el que escribió el repartidor para validarlo.

Para pedidos no pagados, no aparece ese requisito. Los pedidos antiguos sin la nueva marca conservan su condición anterior sin PIN; para probar ambos casos usar pedidos nuevos del catálogo.

El backend genera una vez con aleatoriedad criptográfica, conserva el código al reintentar y lo marca como verificado solo si coincide. Tras cinco intentos incorrectos hay espera de diez minutos. No se registra el código en logs ni se publica a lectores Rider. El texto del PIN en Firebase es exclusivamente la contraparte manual de esta simulación; un sistema real debe resolver la entrega del código al cliente con Chaski Food.

## Cierre y sincronización

La geocerca de 100 m se valida en dominio y no puede saltarse por `isDemo`. El PIN requiere conexión y un pedido recogido/sincronizado. Si falla o no hay conexión, el pedido permanece activo. Firestore impide sincronizar como entregado un pedido marcado pagado sin comprobación del servidor, y no permite cambiar esa marca tras crear el registro.

Se eliminó la escritura directa duplicada de entrega, la URL ficticia de foto y la subida de imagen dentro del repositorio de simulación. El cierre pasa por Room y WorkManager. Repetir la confirmación no duplica el evento de cierre ni vuelve a ejecutar el callback económico. El callback se ejecuta después de guardar y conserva la propina original, sin repartirla artificialmente como 15 %.

Esto no completa HU14: ganancias aún necesita persistencia por usuario, tarifas coherentes e historial de liquidaciones verificables. Tampoco completa HU15. La navegación por Maps, soporte e integración real de Chaski Food no cambian de alcance.

Siguiente etapa HU14, aprobada por el cliente: periodos semanales de lunes a domingo en America/Lima. Las liquidaciones permanecen pendientes hasta registrar manualmente en Firebase un pago simulado; Android no confirma transferencias automáticamente.

## Seguridad publicada

- Storage: `80f5c9cf-4e7c-4fad-b53e-17ed441a6bfe`, coincidencia exacta con el archivo local. Eliminado el permiso general público. Documentos y perfil mantienen sus permisos específicos; evidencias antiguas solo son legibles por su propietario. Subidas de evidencia compatibles se limitan a dueño, pedido existente, JPEG de hasta 5 MB y archivo nuevo.
- Firestore: `91e7a09a-62b2-48ab-b4dc-8373bef0f254`, coincidencia exacta con la vista previa probada. Archivo local reconciliado con las reglas publicadas.
- Nueva callable `deliveryPin`, región `us-central1`. No se actualizaron funciones existentes ni Hosting.

## Verificación

81 pruebas JVM aprobadas. Tres pruebas aisladas de Storage y cuatro de PIN/Firestore aprobadas. Prueba real en celular: el documento privado no es legible por Rider; PIN incorrecto conserva el pedido; correcto permite sincronizar entrega sin evidencia. Regresión de Room, pedidos, soporte y sincronización sin conexión. La prueba de pantalla fue actualizada para desplazarse a los botones dentro del contenido desplazable; no se eliminó su validación de geocerca.
