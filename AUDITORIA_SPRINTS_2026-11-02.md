# Auditoría de sprints 4, 5 y 6 — 02/11/2026

## Actualización posterior: ganancias y liquidaciones (HU14)

Se sustituyeron los datos precargados/en memoria por historial de Room por usuario; se guarda la cotización aceptada, S/1 por km entre ambos trayectos y propina íntegra. Cancelaciones generan S/0. Hay periodos de calendario de Lima, rango de fechas y confirmaciones semanales manuales de Firebase, sin permisos de escritura de pagos desde Android. Los pedidos antiguos sin importe se identifican y excluyen de totales, sin inventarlos. Detalle, validaciones y límites en `HU14_GANANCIAS_LIQUIDACIONES.md`. HU15 sigue parcial. No se modificaron los estados de Jira. Las secciones de diagnóstico siguientes documentan el estado anterior a las correcciones.

## Corrección posterior: seguridad y entrega con PIN

La sección de hallazgos inferior conserva el diagnóstico anterior a corregir. Storage público quedó cerrado y verificado; las reglas locales se reconciliaron. La nueva regla del cliente sustituye el requisito de foto: solo pedidos pagados requieren PIN de cuatro dígitos generado y validado por servidor; ninguno exige foto. Se restauró geocerca, se eliminó la escritura directa/URL ficticia y se recuperaron las pruebas. Detalles y pruebas: `HU11_PIN_ENTREGA.md`. La actualización de HU14 se detalla arriba; HU15 mantiene pendientes sus métricas operativas.

Código Android revisado: `main`, commit `b1226ad`. Jira se interpreta exclusivamente desde la captura del usuario: HU11/HU14 en Listo; HU15/HU16 en curso. No se consultó ni modificó Jira. Excel: `ChaskiRiders-backlogpropuesta (2).xlsx`, PLANIFICACION filas 71–131 y hoja REQUISITOS. Se respetan las decisiones de alcance: pedidos simulados, bicicleta/moto, navegación externa, HU19 cambiada a pago, S/1 por km en ambos trayectos y propina íntegra.

Esta entrega es una auditoría. No cambia lógica Android, reglas publicadas, datos remotos ni estados de Jira. Las pruebas de reglas se hicieron con una copia de producción en un emulador local, sin acceder a fotos de usuarios. El backend tiene trabajo local sin publicar en Git; no equivale al último commit remoto de ese repositorio.

## Dictamen

| Historia | Evidencia actual | Dictamen |
|---|---|---|
| HU11 Entrega | Cámara/previsualización, nota, campos en Room/worker | No debe considerarse cerrada: se puede entregar lejos del cliente, falta PIN/condiciones, la evidencia puede quedar solo en el teléfono y se reutiliza entre pedidos. |
| HU12 Incidencias | Código de categorías, cancelación previa al recojo y registro persistente conservado | Avance previo conservado; requiere regresión con el nuevo cierre. No ejecutada hoy porque las pruebas Android no compilan. |
| HU13 Soporte | Tickets, contexto, borradores, web y emergencia conservados | Implementación previa conservada. Intercambio Google de web publicada y notificaciones push siguen sin certificación nueva. |
| HU14 Ganancias | Pantalla, filtros, detalle y liquidaciones de muestra | Prototipo visual, incompleto funcionalmente: datos en memoria, sin aislamiento por usuario, propina incorrecta y pagos ficticios presentados como confirmados. |
| HU15 Dashboard | Gráfico de hasta seis pedidos dentro de Ganancias | Parcial; no constituye aún un dashboard diario/semanal con métricas operativas persistidas. |
| HU16 Offline | Room v3, acciones con ID estable, WorkManager y migraciones conservados | Base implementada, pero la nueva subida de evidencia y escritura directa rompen la coherencia del cierre. No hay regresión vigente aprobada. |
| HU17 Demanda | Modelo local entrenado con datos sintéticos | Prototipo independiente; no conectado a Android ni datos reales. |
| HU18 Asistente | Guías y conector Gemini desactivado | Preparado parcialmente; modo sin clave no usa IA, proveedor real no probado. |
| HU19 Pago inteligente | Modelo sintético de tiempo y fórmula S/1 por km | Prototipo independiente. IA no modifica dinero, bonos pendientes. Excel original aún describe análisis de fotos. |
| HU20 Publicación | Empaquetado debug | No certifica release firmado, instalación por Play, ficha, privacidad ni publicación. No se consultó Play Console. |

## Hallazgos prioritarios

### P0 — Storage público en producción

La versión publicada contiene `match /{path=**} { allow read, write: if true; }`. Permite acceso sin autenticación al bucket, incluyendo rutas con reglas específicas: los permisos coincidentes se combinan con OR. Contradice el archivo local, que termina en denegación. Confirmado por lectura de reglas, sin probar accesos a documentos/fotos privados.

Ruleset observado: `67393175-78e1-4b2f-88ed-dac83e36ad3f`, release `firebase.storage/chaski-rider.firebasestorage.app`. Prioridad: preparar permisos mínimos para evidencia por propietario/pedido y cerrar la regla general, verificando regresión de documentos/perfil. No sustituir por el archivo local sin añadir la ruta legítima de evidencias.

### P1 — Entrega sin geocerca efectiva

`LocalDemoOfferRepository.kt:86`: DELIVER perdió `OrderLocationRules.requireNear` respecto a la versión anterior. El ViewModel exige una posición disponible, pero no cercanía en esa rama. `OrdersScreen.kt:522` habilita por `isNearCustomer || isDemo`, y ambas rutas de navegación usan `isDemo = true`. Con foto, el flujo actual puede finalizar lejos del destino. Restaurar comprobación de dominio y eliminar el bypass en la ruta de trabajo.

### P1 — Evidencia ficticia o local aceptada como respaldo

`LocalDemoOfferRepository.kt:176` devuelve la URI local cuando la subida falla. No hay una cola de archivos que vuelva a subir esa foto. `:94` introduce una URL fija de ejemplo si no hay resultado; el ViewModel además convierte un URI nulo en el texto `"null"` (`OrdersViewModel.kt:147`). La aplicación no distingue foto capturada, subida pendiente y evidencia confirmada. No existe contrato de PIN ni obligatoriedad por pedido.

`OrdersViewModel` guarda una única foto/nota sin asociarlas a orderId/uid y no las limpia en RESET o aceptación. Un segundo pedido puede reutilizar la foto anterior; SavedStateHandle tampoco es un archivo durable de evidencia para reinstalación/borrado de caché. Capturas en cacheDir pueden desaparecer.

### P1 — Cierre con dos escritores y ganancia antes de persistir

`LocalDemoOfferRepository.kt:92` registra ganancias antes de guardar el pedido en Room. Si guardar falla o se repite DELIVER, se puede duplicar la ganancia: esa rama no rechaza un pedido ya entregado y `recordCompletedOrder` tampoco deduplica.

La escritura directa `:113` no espera confirmación y omite incremento de revisión, campos de fases previas y retirada de tracking. Las reglas actuales la rechazan en un registro existente por `revision > old.revision`. WorkManager es otro escritor, con un resultado diferente. Debe existir una única vía coordinada, con transacción local y sincronización idempotente.

La subida/direct write vuelve a consultar el usuario Firebase en mitad de una operación, en vez de conservar estrictamente el propietario inicial. Un cambio de sesión durante la subida puede dirigir intentos de escritura hacia otra cuenta. Validar el mismo uid antes/después de operaciones asíncronas.

### P1 — HU14 no conserva ganancias ni separa cuentas

`LocalEarningsRepository.kt:14`: MutableStateFlow sin Room ni fuente remota. Al reiniciar el proceso desaparecen los pedidos añadidos y se regeneran cinco ejemplos con fechas relativas al momento de inicio. El repositorio singleton no recibe uid: cambios de usuario comparten el historial en esa ejecución. No puede ser fuente económica del dashboard.

`AppContainer.kt:76–77`: divide total en 85 % base y 15 % propina, sustituyendo la propina original. Dos truncamientos también pueden perder un céntimo. No coincide con el precio ofrecido ni la decisión del cliente. `LocalDemoOfferRepository.kt:188` sigue usando S/0.80 por km de aproximación, S/1.20 por km de entrega, base y garantía; además calcula distancias geográficas, no viales. La tarifa nueva del laboratorio IA aún no está integrada.

### P1 — Pruebas de regresión no compilan

La tarea `testDebugUnitTest assembleDebug assembleDebugAndroidTest --offline` falló en compilación de pruebas. `OrdersViewModelTest.kt:46` y `DemoOrdersDeviceTest.kt:168` pasan LocationRepository como tercer argumento, ahora ocupado por SavedStateHandle. Los dobles de OfferRepository en `OrderSearchEngineTest.kt:26` y `OrdersViewModelTest.kt:36` conservan la firma anterior sin evidencia/nota.

Esto no demuestra que todas las funciones de la app fallen, pero impide aceptar las 80 pruebas antiguas como evidencia del commit actual. La app principal sí llegó a empaquetarse; no se instaló ese APK sobre el celular durante esta auditoría.

### P2 — Fechas, liquidaciones y dashboard incoherentes

- Hoy filtra últimas 24 h, semana últimos siete días y mes devuelve todo (`LocalEarningsRepository.kt:35–37`). No son periodos de calendario ni un rango personalizado; también se aceptarían fechas futuras.
- Liquidaciones: dos registros fijos, importes no derivados de entregas, fechas de pago recalculadas en cada consulta. La UI muestra `Pagado a cuenta final 4321` sin tener esa información (`EarningsScreen.kt:288`) y no presenta paidAt.
- El total de pantalla sí suma los registros visibles, pero esa coherencia interna no valida su procedencia ni la propina.
- `EarningsChartCard.kt:37` toma seis pedidos por comercio; no agrupa por horas/días ni informa que omite el resto. Faltan cancelaciones y tiempo promedio. Los ejemplos preinsertados impiden comprobar el estado real de cero actividad de una cuenta nueva.
- EarningsViewModel lanza consultas sin manejo de errores/cancelación y el repositorio no expone un flujo de cambios. Una implementación remota futura podría dejar el indicador cargando o presentar una respuesta de un filtro anterior.

### P1 — Reglas locales diferentes de producción

Firestore publicado: `aa77ee7f-4556-48cd-b417-b6d88df74496`; sí incluye evidencePhotoUrl/deliveryNote. El archivo local sigue excluyéndolos mediante hasOnly. Un despliegue local completo volvería a rechazar esos campos.

Las reglas publicadas permiten una URI `content://...` como evidencia y no exigen PIN/foto según un contrato del pedido. Añadir nombres a hasOnly no valida que haya un objeto real en Storage. Mantener una definición versionada y probada, sin debilitar acceso para conseguir que una subida funcione.

## Criterios del Excel

| Filas | Estado |
|---|---|
| 71 GPS destino | Regresión: se consulta ubicación, se omite validación de cercanía en dominio y se permite bypass demo. |
| 72 y 75 PIN/foto condicionales | Falta contrato requiresPin/requiresPhoto y validación final fuera de la UI. |
| 73 cámara/previsualización | Presente; falta prueba vigente por fallo de compilación de tests. |
| 74 foto solo de su pedido | No garantizado: URI global del ViewModel, sin reset por pedido y sin confirmación de subida. |
| 76 cierre/hora/tracking/ganancia | Hora/estado y worker existen; doble escritor y ganancia volátil impiden cierre coherente. |
| 77 disponibilidad posterior | Flujo previo conservado; pendiente regresión de cierre completa. |
| 78–89 incidencias/soporte | Funciones previas conservadas; no se ejecutó su regresión Android hoy. |
| 90–92 ganancia/desglose/propina | UI presente; persistencia ausente y propina incorrecta. |
| 93–94 fechas/totales | Filtros temporales incorrectos; suma de visibles presente sobre datos no fiables. |
| 95–96 liquidaciones | Ejemplos fijos, sin correspondencia ni persistencia de pagos. |
| 97–102 dashboard | Gráfico parcial; faltan agrupación diaria/semanal, métricas y estado vacío basado en cuenta real. |
| 103–109 offline | Base Room/WorkManager presente. Evidencia fuera de la cola; regresión de pruebas pendiente. |
| 110–119 IA demanda/asistente | Laboratorio independiente; faltan integración y validación con datos/proveedor reales. |
| 120–124 HU19 original | Sustituida por decisión del cliente. Actualizar backlog académico/Jira para evitar evaluar fotos como IA pendiente. |
| 125–131 estabilización/Play | No verificadas; pruebas Android bloqueadas y debug no acredita publicación. |

## Orden de corrección e implementación

1. Seguridad: eliminar acceso público general de Storage, preparando primero reglas específicas y pruebas por usuario/pedido/tipo/tamaño. Reconciliar las reglas locales con las vigentes.
2. Recuperar compilación de pruebas actualizando firmas y usando parámetros nombrados. Añadir regresiones que fallen con entrega lejana, foto de otro pedido y doble confirmación. No debilitar aserciones anteriores para que pasen.
3. HU11: contrato de requisitos de entrega; geocerca en dominio; PIN si aplica; foto ligada a uid/orderId; persistencia durable y cola de subida idempotente; quitar URL ficticia y escritura directa.
4. HU14: persistir cotización aceptada y ganancia por uid/orderId en la transacción de cierre; conservar propina original y desglose. No recalcular pagos históricos con la tarifa vigente. Datos antiguos sin cotización: mostrar no disponible, no inventar importes.
5. Liquidaciones: definir con el cliente periodo y quién confirma un pago simulado. Android solo consulta; no inventa transferencias ni cuentas. Las liquidaciones deben referenciar entregas para no pagar dos veces.
6. HU15: derivar métricas exclusivamente de los mismos registros persistidos; calendario America/Lima, rangos consistentes, cancelaciones, duración definida y agrupaciones por hora/día. Recalcular al cambiar periodo y probar cuenta vacía.
7. HU16: probar desde aceptación hasta entrega con/sin red, cierre de proceso, cambio de cuenta y subida interrumpida; verificar que se confirmen acciones y archivos sin duplicados.
8. Integrar IA después de estabilizar su fuente de datos y política de bonos. Definir proveedor de rutas/clima y acceso Gemini antes de consumir servicios externos. HU20 al cerrar regresión, release y publicación.

## Verificación ejecutada

- Revisión estática de código actualizado y contraste con Excel/captura.
- Intento de compilación/JVM/instrumentación: fallaron las compilaciones de pruebas descritas. No se ejecutaron pruebas del celular del commit actual.
- Lectura de reglas VIGENTES Firestore y Storage mediante sesión CLI autorizada; no modificación de permisos ni acceso a datos de repartidores.
- Tres comprobaciones locales con copia de reglas publicadas: rechazo de escritura directa sin revisión; aceptación de entrega sin campos PIN/foto; aceptación de URI de dispositivo como evidencia. Las tres pasaron, confirmando esas conductas actuales, no una certificación de HU11.
- Laboratorio IA: 25 pruebas aprobadas, con aviso de deprecación del cliente de pruebas HTTP. Sin llamadas reales a Gemini.

No corresponde marcar el sprint como cerrado. La siguiente prioridad es seguridad y entrega; ganancias/dashboard deben construirse sobre registros fiables y aislados por usuario.
