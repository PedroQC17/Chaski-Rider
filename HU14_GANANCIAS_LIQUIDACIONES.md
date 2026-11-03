# HU14 — ganancias y liquidaciones

## Comportamiento implementado

- Cada pedido guarda su cotización aceptada en el JSON de `order_records`, separado por `uid` y `orderId`. El cierre y su importe quedan en la misma transacción de Room; no se añade una segunda ganancia en memoria. Repetir una entrega no duplica el historial.
- Los nuevos pedidos simulados pagan S/1 por kilómetro sumando repartidor → local y local → cliente. Se redondea el total de distancia al céntimo más próximo (mitad hacia arriba). No hay base fija, mínimo garantizado ni compensación automática por espera. La propina íntegra se suma aparte.
- Cancelación: S/0 de pago y S/0 de propina, aunque conserve su cotización aceptada para trazabilidad. Después del recojo se conserva la regla de resolver con soporte, sin cancelación directa.
- Una cotización histórica guardada se conserva; no se recalcula con la tarifa nueva. Un pedido antiguo sin cotización fiable muestra «No disponible» y no entra en el total monetario ni en pendientes liquidables.
- Hoy, semana de lunes a domingo y mes son periodos de calendario de Lima, no ventanas de 24 horas o siete días. También hay selección de rango inclusivo de fechas. Totales y gráfico usan los pedidos visibles del filtro; el gráfico agrupa todos por día.
- Se eliminan las entregas, liquidaciones y cuenta bancaria ficticias que venían precargadas.
- Cambiar de cuenta limpia registros y detalle seleccionado. Los cambios en Room y las confirmaciones de Firebase actualizan la pantalla con Flow, ViewModel y UiState.
- Las liquidaciones pendientes se derivan de entregas con importe conocido no incluidas en una confirmación. La app solo lee confirmaciones; nunca escribe pagos.

## Confirmación manual de un pago simulado

En Firebase Console → Firestore, usar la colección:

`riders/{uid}/settlements/{periodId}`

El documento principal de una semana usa como ID el lunes: por ejemplo `2026-10-26`. Campos:

| Campo | Tipo Firestore | Contenido |
| --- | --- | --- |
| `periodStart` | string | `2026-10-26` (lunes del periodo, hora de Lima) |
| `status` | string | `PAID` |
| `amountCents` | number entero | Suma de pago Chaski más propina de los pedidos incluidos, expresada en céntimos |
| `paidAt` | timestamp | Fecha real de la confirmación manual simulada, posterior al cierre semanal |
| `orderIds` | array de strings | IDs exactos de las entregas incluidas, sin repetidos |

Pasos:

1. Esperar a que termine el domingo en Lima y que los pedidos pendientes se sincronicen.
2. Revisar `riders/{uid}/deliveryRecords`: incluir solo `DELIVERED` con `earningsQuote` válida, dentro de la semana y no pagados anteriormente. Usar `deliveredAt`, no `acceptedAt`, para asignar la semana. Nunca sumar cotizaciones de cancelados.
3. Sumar `earningsQuote.totalCents`; comprobar Chaski y propina por separado. Los importes aceptados se sincronizan en ese mapa, sin información de PIN.
4. Crear manualmente el documento de confirmación con esos IDs y total. La app lo mostrará como pagado con su fecha. Una semana en curso o un documento incompleto/inconsistente no se muestra como pagado.
5. Si llega tarde otra entrega por sincronización offline, NO modificar la confirmación anterior. Crear otra confirmación, por ejemplo `2026-10-26-ajuste-1`, con el mismo `periodStart` y únicamente los pedidos adicionales. Cada pedido debe aparecer en un solo pago.

La app contrasta los registros locales disponibles con la confirmación administrativa; avisa de duplicados o discrepancias. Una confirmación de otra cuenta no es legible. Los documentos administrativos pueden conservarse aunque el dispositivo no tenga todo el historial local.

## Trazabilidad del código

- `domain/earnings/EarningsPolicy.kt`: tarifa, conservación de cotización, cancelación en cero, calendario y pendientes semanales.
- `domain/orders/OrderRecord.kt`: instantánea de la cotización aceptada.
- `data/orders/DemoOrderDatabase.kt`: guarda estado, historial y acción pendiente juntos; mantiene la primera cotización del pedido. No cambió el esquema SQL v3, por lo que no hace falta una migración nueva.
- `data/orders/LocalDemoOfferRepository.kt`: prepara las nuevas ofertas con la tarifa acordada.
- `data/orders/OrderSyncWorker.kt`: sincroniza la cotización aceptada junto al pedido y mantiene la cola existente.
- `data/earnings/LocalEarningsRepository.kt`: observa Room por cuenta y las confirmaciones de Firebase; sin datos precargados.
- `data/earnings/SettlementParser.kt`: valida fecha, periodo cerrado, céntimos y pedidos de la confirmación manual.
- `ui/screens/earnings/`: ViewModel, UiState y pantalla, con filtros, errores, detalle y liquidaciones.
- Backend `firestore.rules`: cotización inmutable tras creación y liquidaciones de solo lectura por su propietario.

## Validación y límites

- 90 pruebas JVM aprobadas, incluidas nueve nuevas de tarifa, cancelación, datos antiguos, calendario de Lima, rango, liquidaciones, aislamiento y recuperación tras error.
- Tres pruebas nuevas en el celular: persistencia e inmutabilidad del importe al reabrir Room, cancelación en cero, pantalla y cambio de cuenta. Seis pruebas existentes de pedidos/migración también aprobadas.
- Siete pruebas de integración de Firestore/PIN aprobadas, tres de ellas nuevas para cotización y liquidaciones.
- APK instalada en el celular conectado. Reglas publicadas mediante Firebase CLI; no se desplegaron funciones ni sitios web ni se generaron transferencias.
- Las distancias de las ofertas siguen siendo geográficas simuladas; no se ha habilitado un servicio de ruta vial mínima. Este cálculo NO es una cotización de pago real de Chaski Food.
- Room conserva el historial del teléfono al cerrar y reabrir la app. La recuperación completa de entregas tras borrar datos, reinstalar o cambiar de dispositivo aún no está implementada; las liquidaciones confirmadas sí se consultan desde Firebase. No se inventó una recuperación de importes antiguos inexistentes.
- HU15 aún requiere su dashboard operativo (entregas, cancelaciones y duración media). No se declara implementada por tener un gráfico de ganancias.
