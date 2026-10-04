<!--
  Fuente desplegada de las Cloud Functions de Chaski Rider (proyecto chaski-rider).
  Copia exacta descargada del bucket gcf-v2-sources el 04/10/2026.
  NO editar sin coordinar con Angel: producción es la fuente de verdad.
-->
# Backend — Cloud Functions (copia de producción)

## Origen

Descargado desde `gcf-v2-sources-634578215837` (project number de `chaski-rider`):

- Consola: https://console.cloud.google.com/storage/browser/gcf-v2-sources-634578215837?project=chaski-rider
- Un zip por función: `riderRegistration_function-source.zip`, `reviewRider_function-source.zip`, `riderOfferDemo_function-source.zip`.
- Esta copia corresponde al zip de `riderOfferDemo` (el más reciente: incluye `offers/`). El código de `riderRegistration` desplegado es idéntico salvo la última línea de exportación de offers.

## Funciones

| Función | Tipo | Región | Uso |
|---|---|---|---|
| `riderRegistration` | onCall | us-central1 | Acciones del registro: `personal`, `vehicle`, `bank`, `document`, `submit`, `availability`, `profilePhoto` |
| `reviewRider` | onCall | us-central1 | Aprobación/rechazo (claim `riderReviewer`), solo admin |
| `riderOfferDemo` | onRequest | us-central1 | Demo de pedidos HU07 (`offers/`) |

## Pruebas locales

```bash
npm test          # node --test test/*.test.js  (validación pura, sin dependencias)
```

## Despliegue

> ⚠️ Coordinar con Angel antes de desplegar. Producción ya tiene `availability` y `profilePhoto`.

```bash
firebase login
firebase use chaski-rider
firebase deploy --only functions:riderRegistration,functions:reviewRider
```

Usar siempre `--only` con las funciones necesarias para no tocar las demás.

## Historia de esta carpeta

- 28/09/2025: parches locales (acción `availability`, `editable()`) — ya superados.
- 04/10/2026: reemplazada por la fuente real de producción (incluye `availability`,
  `profilePhoto`, reservas DNI/teléfono, `invoker: "public"`).
