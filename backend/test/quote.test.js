// HU07: cotización con base, extras, propina separada y garantía del monto aceptado.
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { quote } = require("../offers/route-quote");
const fixture = require("../offers/demo-fixture");

test("el total es la suma exacta de los conceptos visibles", () => {
  const q = quote([fixture.stops[0]]);
  assert.ok(q.baseCents > 0, "pago base presente");
  assert.ok(q.tipCents > 0, "propina presente como concepto separado");
  assert.equal(q.totalCents, q.baseCents + q.approachCents + q.deliveryCents + q.tipCents + q.guaranteeCents);
});

test("el monto total nunca disminuye bajo el ya aceptado", () => {
  const first = quote([fixture.stops[0]]);
  const second = quote([fixture.stops[0], fixture.stops[1]], first.totalCents);
  assert.ok(second.totalCents >= first.totalCents);
  assert.ok(second.additionalCents >= 0);
  const held = quote([fixture.stops[0]], first.totalCents + 500);
  assert.equal(held.totalCents, first.totalCents + 500, "la garantía cubre la diferencia");
  assert.ok(held.guaranteeCents > 0);
});

test("la propina se conserva íntegra en la cotización", () => {
  assert.equal(quote([fixture.stops[0]]).tipCents, fixture.TIP_CENTS);
  assert.equal(quote([fixture.stops[0], fixture.stops[2]]).tipCents, fixture.TIP_CENTS);
});
