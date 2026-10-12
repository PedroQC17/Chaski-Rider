# Chaski Rider: mapa técnico y revisión del backlog

Revisión del 11 de octubre de 2026 (America/Lima).

**Actualización del 12 de octubre:** se implementó la primera mejora del registro: estado común de los tres pasos, borradores por usuario en Room y recuperación de capturas pendientes. El detalle actualizado está en [REGISTRO_PERSISTENCIA.md](REGISTRO_PERSISTENCIA.md). El análisis que sigue conserva la situación inicial; sus observaciones sobre ausencia de Room y los antiguos PersonalRegistrationViewModel/UiState ya están superadas por esa entrega. Soporte, estados administrativos y las demás brechas continúan pendientes.

Este documento conserva el contexto de la revisión para futuras conversaciones. Describe el código local observado; no certifica lo desplegado en Firebase ni sustituye pruebas en dispositivo. No se modificó lógica de aplicación, reglas ni backend durante esta revisión.

## Fuentes y alcance

- Android: `C:/Users/Angel/Desktop/Chaski_Rider`.
- Backend separado: `C:/Users/Angel/Desktop/Chaski_Rider_Backend`.
- Backlog: `C:/Users/Angel/Downloads/ChaskiRiders-backlogpropuesta (2).xlsx`, hojas `PLANIFICACION` (filas 1–131) y `REQUISITOS` (filas 1–15). Se leyeron ambas completas; no contienen comentarios de celda.
- Se revisaron los 106 archivos Kotlin de producción, pruebas Android, JavaScript de ambas carpetas backend, reglas de seguridad, configuración y documentación. Se excluyen dependencias, código generado, binarios y secretos locales.
- El texto aportado del chat anterior sirve de referencia histórica. Sus diez sprints reconstruidos NO son la planificación del Excel.
- Los comentarios y documentos del repositorio contienen afirmaciones históricas contradictorias. Las conclusiones siguientes proceden del código actual y de las pruebas especificadas.

## Planificación oficial

| Sprint | Historias | Semanas | Lectura del estado local |
|---|---|---|---|
| 1 | HU01–HU04 | 2–3 | Base implementada, con brechas en documentos, automóvil y notificaciones de ofertas. |
| 2 | HU05–HU07 | 4–5 | Demostración y componentes implementados; no existe despacho integrado con Food. Hay errores de consistencia. |
| 3 | HU08–HU10 | 6–7 | Parcial/prototipo. No reúne todos los criterios para declararlo terminado. |
| 4 | HU11–HU13 | 8–9 | Hay un botón de entrega básica; no evidencia un flujo completo de PIN, evidencia, incidencias y soporte. |
| 5 | HU14–HU16 | 10–11 | Pendientes ganancias persistidas, dashboard, Room y WorkManager. |
| 6 | HU17–HU20 | 12–13 | Sin implementación de las tres funciones de IA. Publicación en Play no verificada. |

## Mapa para trabajar en el código

Las rutas Android siguientes parten de `app/src/main/java/com/example/chaskirider/`.

| Área | Archivos principales | Responsabilidad |
|---|---|---|
| Arranque | `MainActivity.kt` | Inicializa dependencias y procesa el extra de notificación demo. |
| Dependencias | `di/AppContainer.kt` | Service locator manual, repositorios y sesión compartida. No usa Hilt. |
| Navegación | `ui/navigation/AppNavigation.kt`, `Screen.kt`, `WorkSidebar.kt` | Rutas, permisos de documentos, creación de ViewModels, gating por registro/foto/disponibilidad y drawer. |
| Autenticación | `ui/screens/auth/AuthViewModel.kt`, `AuthUiState.kt`, `AccessScreen.kt`, `EmailLoginScreen.kt`, `SessionLoadingScreen.kt`, `RegistrationStatusScreen.kt`, `PasswordSetupDialog.kt` | Sesión, acceso, recuperación, contraseña y estado de habilitación. |
| Google | `ui/platform/GoogleSignInClient.kt` | Credential Manager; entrega el token al flujo de autenticación. |
| Registro personal | `ui/screens/onboarding/PersonalRegistrationViewModel.kt`, `PersonalRegistrationUiState.kt`, `OnboardingStep1Screen.kt` | Formulario personal, validaciones, vinculación de contraseña y guardado. |
| Vehículo y banco | `ui/screens/onboarding/OnboardingStep2Screen.kt`, `OnboardingStep3Screen.kt`, `BankSelector.kt`; `ui/screens/profile/RiderProfileViewModel.kt`, `RiderProfileUiState.kt` | Selección, formulario bancario y envío del registro. |
| Perfil | `ui/screens/profile/Profile*Screen.kt`, `components/ProfileCards.kt`, `ProfileDetailsLayout.kt` | Consulta de información y aviso de solicitudes de cambio aún no implementadas. |
| Documentos | `ui/screens/profile/documents/DocumentsViewModel.kt`, `DocumentsUiState.kt`, `DocumentPreviewUiState.kt`, `DocumentPreviewHost.kt` | Captura, subida, apertura y paginación/zoom de vista previa. |
| Foto | `ui/screens/profile/photo/ProfilePhotoViewModel.kt`, `ProfilePhotoUiState.kt`, `ProfilePhotoRoute.kt`, `ProfilePhotoScreen.kt` | Foto obligatoria tras aprobación; SavedStateHandle para las URI de captura/preview. |
| Inicio | `ui/screens/home/HomeViewModel.kt`, `HomeUiState.kt`, `HomeRoute.kt`, `HomeScreen.kt` | Disponibilidad confirmada por servidor, permisos, ubicación puntual y demanda. |
| Mapa de trabajo | `ui/screens/home/components/WorkMap.kt`, `ConnectionPanel.kt`, `AvailabilitySlider.kt` | Mapa, círculos de demanda y control de conexión. |
| Pedidos | `ui/screens/orders/OrdersViewModel.kt`, `OrdersUiState.kt`, `OrdersScreen.kt`, `OrdersMap.kt`, `OrderQuoteCard.kt` | Oferta, aceptación/rechazo, llegada, espera, recojo y entrega. Comparten ViewModel entre repositorio remoto y demo. |
| Chat | `ui/screens/chat/ChatViewModel.kt`, `ChatUiState.kt`, `ChatDialog.kt` | Mensajes locales, canales por etapa y dictado mediante actividad externa. |
| Notificaciones | `ui/screens/notifications/NotificationsViewModel.kt`, `NotificationsUiState.kt`, `NotificationsScreen.kt` | Lista y contador de no leídas. |
| Contratos | `domain/repository/*.kt`, `domain/orders/OfferSnapshot.kt` | Interfaces de autenticación, perfil, sesión, documentos, foto, ubicación, notificaciones y ofertas. |
| Modelos y reglas | `domain/model/*.kt`, `domain/orders/DeliveryQuote.kt`, `domain/chat/ChatMessage.kt`, `domain/location/LocationUtils.kt` | Perfil, documentos, vehículos, validaciones, dinero en céntimos y geometría. |
| Textos | `domain/text/TextKey.kt`, `TextProvider.kt`, `data/text/AndroidTextProvider.kt`, `app/src/main/res/values/strings.xml` | Catálogo y traducción de errores conocidos. Hay textos nuevos incrustados en pedidos/chat. |
| Acceso a perfil | `data/repository/AuthRepositoryImpl.kt`, `RiderProfileRepositoryImpl.kt`, `data/remote/RiderProfileRemoteDataSource.kt`, `FirebaseResult.kt` | Firebase Auth, lecturas de perfil, callable functions y traducción de fallos. |
| Sesión | `data/session/RiderSessionStore.kt` | StateFlow en memoria, compartido entre ViewModels. Se reconstruye leyendo Auth/perfil. |
| Archivos | `data/documents/RiderDocumentDataSource.kt`, `DocumentPreviewRepositoryImpl.kt`, `data/repository/DocumentRepositoryImpl.kt` | Storage, archivos temporales, límites y renderizado PDF fuera del hilo principal. |
| Fotos | `data/profile/ProfilePhotoRepositoryImpl.kt`, `PortraitImageProcessor.kt` | Carga/guardado y procesamiento JPEG, tamaño y orientación. |
| Ubicación | `data/location/DeviceLocationRepository.kt` | getCurrentLocation y fallback a lastLocation; no servicio de tracking. |
| Ofertas | `data/orders/OfferApi.kt`, `RealOfferRepository.kt`, `LocalDemoOfferRepository.kt`, `OfferNotifier.kt` | HTTP Retrofit, mapeo DTO, demo local y avisos Android. El remoto llama a riderOfferDemo. |
| FCM | `data/notifications/ChaskiFirebaseMessagingService.kt`, `NotificationsInitializer.kt`, `RiderTopicSubscription.kt`, `NotificationsStore.kt` | Recepción, topics y almacenamiento en memoria; repositorio en `data/repository/NotificationsRepositoryImpl.kt`. |
| UI común | `ui/components/*.kt`, `ui/theme/*.kt` | Encabezados, diálogos, contraseña, iconos y tema Material 3. |
| Android/configuración | `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/*.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml` | Permisos, FileProvider, dependencias y configuración de builds. |

## Dos versiones del backend

1. `Chaski_Rider_Backend/functions/`: el README lo identifica como fuente de desarrollo. Tiene `index.js`, `validation.js` y `offers/{demo-handler,route-quote,demo-fixture}.js`. Sus reglas están en la raíz del repositorio. Su handler de pedidos admite únicamente state, offer, accept, reject, pickup y reset.
2. `Chaski_Rider/backend/`: el README dice que fue una copia de producción del 4 de octubre. Actualmente contiene más funciones: reglas de asignación, zonas, llegada, espera, entrega y locks compartidos. No se comprobó si esa versión coincide con producción hoy.
3. El `firebase.json` de Android apunta a `backend`; el del repositorio separado apunta a `functions` con codebase `rider-registration`. Hay dos entradas de despliegue distintas.
4. `validation.js` coincide entre copias. `index.js` no muestra diferencias textuales con git diff; la diferencia de hash observada es de representación del archivo. Los tres archivos compartidos de `offers` sí tienen diferencias funcionales. `offer-rules.js` y sus pruebas solo están en Android/backend.
5. El backend separado ya tenía cambios sin commit en DEPLOYMENT_STATUS.md, functions/index.js, functions/validation.js y storage.rules, y archivos sin seguimiento CAMBIOS_PENDIENTES.md, CONTRATO_PEDIDOS.md y functions/offers. Se conservaron.

Antes de implementar pedidos hay que reconciliar estas fuentes y dejar una ruta de mantenimiento inequívoca. No se debe desplegar una copia antigua sobre una más avanzada por asumir que el nombre del repositorio indica su actualidad.

`index.js` implementa registro transaccional, reservas de DNI/teléfono, validación de propiedad y firma de archivos, foto de perfil única y revisión administrativa por claim. Las reglas prohíben escrituras directas a perfiles y acceso ajeno; los documentos se crean con UUID inmutable. Esto es una base útil que conviene conservar.

## MVVM y conservación del registro

El proyecto ya tiene el patrón Screen → ViewModel → Repository → datos, con UiState expuesto como StateFlow. La UI no realiza llamadas directas a Firebase/Retrofit. Hay excepciones de acoplamiento menores: OrdersViewModel toma por defecto LocationRepository de AppContainer y RiderUser lleva anotaciones Firestore. No justifican rehacer toda la arquitectura.

| Información | Dónde vive ahora | Recuperación observada |
|---|---|---|
| Cuenta autenticada | Firebase Auth | El código consulta currentUser al arrancar. |
| Datos personales ya guardados, vehículo y referencias de documentos | Firestore/Storage | Se vuelven a leer al iniciar, con red. |
| Campos aún sin guardar del paso 1 | PersonalRegistrationUiState en ViewModel | Sobreviven a recreación de Activity con el mismo ViewModel; no a muerte del proceso. |
| Vehículo seleccionado y banco todavía sin enviar | rememberSaveable en pasos 2 y 3 | Restauración de estado guardado cuando Android conserva la tarea; no persistencia duradera. |
| Foto de perfil pendiente | SavedStateHandle + archivo de caché | Hay mecanismo de restauración de URI; requiere que el archivo siga disponible. |
| Documento tomado con cámara, todavía sin confirmar | pendingCapture y DocumentsUiState en memoria | La muerte del proceso puede perder la asociación URI/tipo. |
| Pedido de la demo local | Respuestas fabricadas por acción | STATE devuelve vacío incluso sin matar el proceso; refrescar pierde el progreso. |
| Chat y notificaciones | StateFlow en memoria | Se pierden al terminar el proceso; el chat tampoco tiene repositorio remoto. |

`UiState` describe la pantalla; `ViewModel` coordina eventos y mantiene estado durante su vida; ninguno constituye por sí mismo almacenamiento persistente. SavedStateHandle y rememberSaveable ayudan a restaurar estado pequeño tras recreación/muerte del proceso iniciada por el sistema, pero no sustituyen una base de datos para cierre voluntario, nueva apertura o reinicio.

La lectura del perfil usa `Source.SERVER`: una sesión Auth conservada no significa que se pueda abrir el registro sin internet. AuthViewModel deja la pantalla de recuperación/error si no puede cargarlo.

Propuesta para una implementación posterior: mantener Screen + UiState + ViewModel, elevar los formularios de vehículo/banco al estado de presentación y persistir un borrador por UID detrás de un repositorio. Guardar campos necesarios, paso y referencias duraderas de archivos; conservar contraseñas solo en memoria. Separar borrador editable de perfil confirmado. Limpiar/aislar datos al cambiar de cuenta y reconciliar con Firebase sin sobrescribir un borrador reciente con una respuesta vieja. Room tiene una justificación concreta porque HU16 exige SQLite y una cola de acciones; no hace falta añadir capas o ViewModels por cada componente visual.

Referencia técnica: https://developer.android.com/topic/libraries/architecture/saving-states

## Contraste de HU01–HU10

| HU / filas PLANIFICACION | Evidencia favorable | Brecha para cumplir todos los criterios |
|---|---|---|
| HU01 / 7–12 | Capas, repositorios, Compose Material 3, StateFlow, coroutines y Retrofit; compilación y tests JVM correctos. | El manejo de errores de pedidos altera estados en lugar de representar fallo; falta probar recuperación y conectividad en dispositivo. |
| HU02 / 13–19 | Auth Google, acceso correo/contraseña, vinculación de contraseña, cierre, unicidad de DNI/teléfono y gating. | La creación de cuenta parte de Google; el correo permite acceder/vincular contraseña, no hay formulario createUserWithEmailAndPassword en la app. HU02 no exige expresamente ese método de alta. Recuperación sin red limitada. |
| HU03 / 20–26 | Registro por pasos, documentos privados, cámara y preview, edición en NEEDS_CORRECTION. | Fila 21 incluye automóvil; UI y servidor lo rechazan. Fila 24 exige Pendiente/Aprobado/Observado por documento; solo hay estado global de revisión y estados de subida. |
| HU04 / 27–32 | Permisos de ubicación al conectar, estado remoto confirmado, FCM receptor y notificación local de oferta. | No hay emisor/despachador FCM de ofertas en el código revisado. OfferNotifier abre demo incluso desde la instancia remota. La copia antigua del handler no verifica disponibilidad al ofrecer. |
| HU05 / 33–37 | Círculos, selección, timestamp, refresco y vacío implementados. | Los datos son fixtures en un backend; en el otro no se devuelven. Se pueden presentar zonas sintéticas en el mapa de trabajo sin señalarlas como demo. Si falla la primera carga no aparece el botón de refrescar, condicionado al timestamp. |
| HU06 / 38–44 | Botones, TTL remoto de 45 s, transacciones y reglas puras probadas en la copia ampliada. | No hay asignación real Food ni recepción automática demostrada. Radio calculado con dato fijo; problema de locks al reasignar pedidos liberados. |
| HU07 / 45–51 | Componentes del desglose y garantía; cotizador ampliado con tests. | Demo local: conceptos suman S/23.00 y total indica S/11.50. Cotizador del backend separado no incluye base/propina. Recorridos y tiempos sintéticos. |
| HU08 / 52–57 | Botones de llegada/espera, distancia, círculo de 100 m y compensación parcial. | El mapa usa el primer punto de la ruta como repartidor, no riderLocation. No hay recálculo vial. Llegada no bloqueada por geocerca; el handler ampliado recibe coordenadas pero no las valida ni guarda. |
| HU09 / 58–63 | pickedUpAt/deliveredAt en handler ampliado, sondeo GPS cada 5 s en ViewModel y parada del bucle al entregar. | No hay envío continuo de posición, servicio foreground, recuperación duradera ni recálculo al cliente. El sondeo es local. No hay flujo de cancelación. |
| HU10 / 64–70 | Dos canales, remitente/hora, UI de lectura por etapa y dictado que permite revisar antes de enviar. | Mensajes sembrados/locales, sin servidor. Usa primer orderId del lote o demo_order; envío no valida etapa en ViewModel. El dictado usa RecognizerIntent externo, no SpeechRecognizer embebido; confirmar equivalencia académica si se exige esa API exacta. |

## Hallazgos que conviene corregir primero

1. **Errores que se convierten en éxito.** `OrdersViewModel.execute` llama a `fallbackSimulateLocalAction` ante cualquier excepción en llegada, espera, recojo, entrega y reset, también con el repositorio remoto. Puede ocurrir con 400/403/409, no solo falta de red. La pantalla afirma hechos que el servidor rechazó. `clearError` también simula llegada si hay lote sin arrivedAt; hoy el botón que lo llama está rotulado como salto de demo. Eliminar esa posibilidad en el flujo remoto y representar estados pendientes/confirmados/fallidos explícitos.
2. **Geocerca sin enforcement.** `OrdersScreen` calcula canConfirmArrival pero no lo utiliza en enabled. `backend/offers/demo-handler.js` extrae latitude/longitude y luego no las usa; arrive_merchant solo asigna una hora. La validación debe vivir también en el servidor y registrar ubicación/hora. Sin posición válida no se puede afirmar llegada.
3. **Tracking nominal.** `syncTrackingLifecycle` solo ejecuta getCurrentLocation periódicamente; el repositorio no publica esas posiciones. La ubicación se envía únicamente al solicitar llegada y entrega y el handler actual no la persiste. No satisface HU09 ni la tecnología Foreground Service del Excel.
4. **Pérdida de registro y capturas pendientes.** Paso 1 sin SavedStateHandle ni borrador; cámara de documentos sin restauración. Guardar solo al pulsar Continuar conserva únicamente lo confirmado. El objetivo de no perder lo escrito exige definir y persistir el borrador.
5. **La demo no conserva estado.** LocalDemoOfferRepository reconstruye snapshots y devuelve vacío con STATE. Volver de otra Activity (incluido dictado) o reanudar OrdersRoute desencadena refresh. El problema existe aun sin cierre de proceso. También reinventa horas al avanzar y ofrece TTL 60 s frente a barra de 45 s.
6. **Dinero incoherente en demo local.** 500 + 200 + 800 + 150 + 650 = 2300 céntimos; totalCents está fijado en 1150. La UI muestra todos esos conceptos. Mantener una sola cotización coherente y probar la suma; no recalcular un precio confirmado en la UI.
7. **Locks de asignación incompletos en backend ampliado.** eligible permite un pedido RELEASED de otro UID, pero el bucle de escrituras omite todo documento cuyo UID sea distinto, aunque esté RELEASED. Se guarda la oferta en el estado individual sin reservar el lock compartido. accept tampoco exige que el lock pertenezca al aceptante. Esto abre una ruta a ofertas/aceptaciones concurrentes del mismo pedido. Los tests existentes cubren eligible aislado, no la transacción completa. Además, un OFFERED de un usuario que no vuelva a consultar puede permanecer bloqueado: no hay vencimiento independiente del lock.
8. **Rutas y ubicación visual.** OrdersMap recibe snapshot pero no riderLocation. isOffRoute compara con vértices, no distancia a segmentos, y su resultado no dispara recálculo. Puede considerar fuera de ruta un punto en medio de un segmento largo. El indicador de distancia usa línea recta; no equivale a distancia vial restante.
9. **Chat solo aparente.** La UI dice tiempo real, pero no se transmite ningún mensaje. Vincular por pedido/canal, validar permisos y etapas en servidor y repositorio, y representar envío/confirmación/error. Los canales visualmente deshabilitados no sustituyen controles de negocio.
10. **Notificaciones.** OfferNotifier siempre escribe EXTRA_DEMO=true; apertura debug conduce a demo local. Los observadores solo llaman update cuando hay offer: al pasar a null no cancelan inmediatamente el aviso. No hay restauración dirigida por ID de oferta. El menú de demo tampoco está limitado a DEBUG aunque algunos documentos digan que sí.

Otros ajustes posteriores: conservar requestId de una operación a través de reintentos (RealOfferRepository genera uno nuevo en cada llamada); no capturar silenciosamente CancellationException mediante runCatching en refreshZones; cancelar/aislar respuestas de cargas al cambiar de cuenta; evitar logs de coordenadas en producción; detener el reloj de espera al recoger; mover textos incrustados a recursos. No hace falta convertir estas mejoras en una reescritura total.

## Requisitos del curso

Fuente: hoja REQUISITOS, filas 4–15.

| Requisito | Evaluación local |
|---|---|
| Autenticación | Implementada; pendiente validación integral en dispositivo en esta revisión. |
| Tres procesos de negocio | Delivery parcial; gestión económica e incidencias/soporte todavía no completas. |
| Firebase | Auth, Firestore, Storage, Functions y receptor FCM presentes. Chat/soporte sincronizados pendientes. |
| Material Design | Material 3 presente. |
| MVVM + Clean Code | Base presente; consistencia de formularios y persistencia por completar. |
| Coroutines + Retrofit | Presentes en acceso a datos/ofertas. |
| WorkManager | Ausente; planificado HU16. |
| SQLite | Room/SQLite ausentes; planificado HU16. |
| Dashboard | No implementado; HU15. |
| Tres recursos móviles | Ubicación y cámara implementadas; dictado por app externa presente, falta validación en dispositivo. |
| Tres funciones de IA | HU17, HU18 y HU19 sin implementar. Dictado no demuestra esas tres historias. |
| Google Play Store | No se verificó distribución. El build debug y minSdk/targetSdk no prueban publicación release. |

Los requisitos de sprints futuros no son defectos por estar todavía pendientes. Deben planificarse sin declarar cumplido lo que solo tiene un botón o una dependencia.

## Validación realizada

- `./gradlew.bat :app:testDebugUnitTest --offline --console=plain`: BUILD SUCCESSFUL; 9 tests, 0 fallos. Se usaron dependencias locales y tareas de compilación existentes.
- `node --test backend/test/offer-rules.test.js backend/test/quote.test.js backend/test/validation.test.js`: 13/13 correctos.
- En backend separado, `node --test functions/test/validation.test.js`: 4/5 correctos. Falla el caso bancario: espera aceptar bankName=Bank, fuera de BCP/INTERBANK/BBVA. Es un dato de prueba desactualizado respecto a la validación.
- Revisión estática de pruebas instrumentadas: bicycleDoesNotRequireMotorcycleDocuments espera habilitar Finalizar registro con CCI vacío; el formulario exige 20 dígitos. No se ejecutó esa prueba.
- Las pruebas de integración de ambas copias también contienen Bank y CCI vacío. En la copia dentro de Android faltan los archivos de reglas en las rutas que esas pruebas esperan; en el backend separado sí existen.
- No se ejecutaron tests instrumentados ni emuladores Firebase, ni se crearon usuarios o pedidos remotos. No se desplegó ni se verificó la versión actualmente publicada.
- Los tests correctos cubren validaciones/cotización, no prueban process death, tracking, chat, geocerca o concurrencia transaccional completa.

## Orden de trabajo recomendado

1. Resolver qué backend se mantiene y comparar con la versión desplegada antes de publicar cambios.
2. Estabilizar registro: estado coherente por pantalla, borrador por UID, recuperación de cámara y pruebas de rotación/muerte de proceso/reapertura.
3. Corregir éxitos ficticios, contrato de estados, geocerca y cotización. Mantener la demo explícita y aislada.
4. Cerrar HU08–HU10 con rutas, seguimiento autorizado/recuperable y chat persistido. Probar lo que ve el cliente/establecimiento, no solo lo que muestra el repartidor.
5. Resolver diferencias de alcance del sprint 1 (automóvil, revisión por documento) contra el backlog que se entregará a la profesora.
6. Continuar sprints 4–6. Aprovechar la persistencia como base de HU16, sin adelantar funcionalidades que aún no se necesiten.

Criterio para futuros cambios: cada archivo o abstracción nueva debe responder a un requisito concreto o a un fallo reproducible. Conservar las capas y componentes que ya funcionan; evitar crear infraestructura por anticipación.
