# HU20 — Estabilización y publicación

## Estado actual

La app compila en release. El artefacto generado sin credenciales de firma es:

```
app/build/outputs/apk/release/app-release-unsigned.apk
```

Para publicar hace falta firmar el APK/AAB con la keystore del equipo.

## Configuración local

La clave de Maps y la firma se leen desde `local.properties` o desde variables de entorno. `local.properties` no se versiona.

```properties
MAPS_API_KEY=...
RELEASE_STORE_FILE=C:\\ruta\\chaski-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

También se pueden definir como variables de entorno con los mismos nombres.

## Crear la keystore de release

Solo una vez por equipo. No versionar la keystore ni las contraseñas.

```powershell
keytool -genkeypair -v `
  -keystore chaski-release.jks `
  -alias chaski `
  -keyalg RSA -keysize 2048 -validity 10000
```

## Compilar para publicar

```powershell
.\gradlew.bat :app:clean :app:bundleRelease :app:assembleRelease
```

Artefactos esperados:

- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/release/app-release.apk`

Si las cuatro propiedades de firma no están definidas, Gradle genera un APK/AAB sin firmar; no debe subirse a la tienda.

## Checklist antes de publicar

- [ ] `MAPS_API_KEY` configurada y restringida a la aplicación.
- [ ] `app/google-services.json` corresponde al proyecto Firebase de producción.
- [ ] Keystore de release configurada y respaldada fuera del repositorio.
- [ ] `versionCode` incrementado y `versionName` actualizado.
- [ ] `firestore.rules` y las callables del backend desplegadas.
- [ ] Reglas de Firebase probadas con un usuario real.
- [ ] Pruebas unitarias e instrumentadas ejecutadas.
- [ ] Prueba manual: login, conexión, oferta, recojo, entrega, PIN, foto, ganancias.
- [ ] Verificar que no se publiquen datos de prueba ni contenido demo.
- [ ] Revisar permisos declarados: ubicación, cámara, teléfono y notificaciones.
- [ ] Verificar que `allowBackup=false` siga siendo la configuración esperada.
- [ ] Revisar el reporte de `lintVitalRelease`.

## Limitaciones conocidas

- R8/minify está desactivado para evitar cambios de comportamiento sin una prueba completa en dispositivo. Si se activa, validar Gson, Firebase y los modelos persistidos con las reglas de `app/proguard-rules.pro`.
- La firma no se puede completar desde este repositorio sin la keystore y sus contraseñas.
- La validación de calidad de evidencia es local y heurística; el modelo de IA del backend queda para la integración posterior.
