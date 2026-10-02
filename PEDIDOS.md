# Primera entrega: ofertas (HU07)

Abrir build **debug** → menú lateral → **Demo de pedidos**. Requiere perfil aprobado, habilitado y foto.

1. «Recibir oferta de ejemplo»: aparece el plazo y cotización sobre el mapa.
2. Aceptar y generar adicionales hasta tres; el orden de entregas se optimiza con la matriz de ejemplo del backend.
3. Rechazar o dejar vencer un adicional conserva el lote anterior. Volver a la pantalla recupera el estado remoto.
4. «Simular recojo» impide añadir pedidos; «Reiniciar demostración» limpia solo esta demo.
5. «Recibir en 5 s y probar el aviso»: cambiar de aplicación sin cerrar el proceso. Android necesita permitir notificaciones y tener sonido habilitado. Al pulsar el aviso se abre la demo.

Los pedidos, ubicación del repartidor, distancias y líneas son sintéticos, identificados en pantalla. El mapa base sí es Google Maps. No se solicitan rutas reales ni se conectó todavía Food. No afecta disponibilidad de trabajo ni dinero real.

## Estructura

- `domain/orders`: modelo de cotización/oferta y contrato del repositorio.
- `data/orders`: Retrofit, DTO y mapeo, autenticación Firebase, notificación Android.
- `ui/screens/orders`: ViewModel con StateFlow/UiState, pantalla, mapa y componente de desglose.
- `di/AppContainer`: inyección del repositorio; navegación conserva un ViewModel para el flujo y limpia estado al cambiar de cuenta.
- Todos los textos visibles están en `res/values/strings.xml`; IDs de pedidos y códigos de transporte son datos.
- Backend separado: `Chaski_Rider_Backend/functions/offers`; contrato en `Chaski_Rider_Backend/CONTRATO_PEDIDOS.md`.

El servidor confirma decisiones, cupo, vencimiento e importes. La UI no acepta optimistamente ni recalcula el pago. Ante fallo de conexión consulta estado; si no puede confirmar, pide actualizar antes de permitir más acciones.

Pendiente: proveedor de rutas reales y perfil de vehículo, acuerdo de integración con Food, asignación real, FCM dirigido con recuperación tras muerte del proceso, demanda (HU05), llegada/espera (HU08), tracking en segundo plano (HU09). No se activaron servicios de rutas adicionales. Esta entrega no implementa seguimiento continuo.

No se ejecutaron pruebas. Se comprobaron compilación Kotlin y recursos; el usuario realizará la validación en dispositivo.
