"use strict";
const fixture = require("./demo-fixture");
function permutations(items) {
  if (!items.length) return [[]];
  return items.flatMap((item, i) => permutations(items.filter((_, j) => i !== j)).map(rest => [item, ...rest]));
}
// La matriz proviene del proveedor de rutas; en esta demo es explícitamente sintética.
function quote(stops, previousTotalCents = 0) {
  if (!stops.length || stops.length > 3 || new Set(stops.map(s => s.id)).size !== stops.length ||
      stops.some(s => s.merchantId !== "demo-store")) throw new Error("invalid_batch");
  const candidates = permutations(stops).map(order => {
    let from = "store", distance = 0;
    for (const stop of order) { distance += fixture.meters[from][stop.id]; from = stop.id; }
    return { order, distance };
  }).sort((a, b) => a.distance - b.distance || a.order.map(s => s.id).join("").localeCompare(b.order.map(s => s.id).join("")));
  const best = candidates[0];
  const approachMeters = 1600;
  const approachCents = Math.round(approachMeters / 10);
  const deliveryCents = Math.round(best.distance / 10);
  // HU07: total = base + extras (acercamiento y ruta) + propina conocida + garantía.
  // La garantía absorbe la diferencia para que el total nunca baje del monto aceptado.
  const baseCents = fixture.BASE_CENTS;
  const tipCents = fixture.TIP_CENTS;
  const visibleCents = baseCents + approachCents + deliveryCents + tipCents;
  const guaranteeCents = Math.max(0, previousTotalCents - visibleCents);
  const totalCents = visibleCents + guaranteeCents;
  return { stops: best.order, approachMeters, deliveryMeters: best.distance,
    durationSeconds: Math.round((approachMeters + best.distance) / 4),
    baseCents, approachCents, deliveryCents, tipCents, guaranteeCents, totalCents,
    additionalCents: Math.max(0, totalCents - previousTotalCents), currency: "PEN",
    path: [fixture.points.rider, fixture.points.store, ...best.order.map(s => s.point)] };
}
module.exports = { quote };
