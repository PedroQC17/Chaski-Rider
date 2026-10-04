"use strict";
const test = require("node:test");
const assert = require("node:assert");
const { eligible, RADIO_METERS } = require("../offers/offer-rules");

const riderBike = { uid: "rider-1", vehicleType: "BICYCLE" };
const stopA = { id: "a", orderId: "demo-order-1", vehicleTypes: ["BICYCLE", "MOTORCYCLE"] };

test("FUERA DE ZONA: si el local excede el radio no se ofrece", () => {
  assert.equal(eligible(riderBike, stopA, {}, RADIO_METERS + 1), "out_of_zone");
});

test("ZONA OK: distancia dentro del radio no bloquea", () => {
  assert.equal(eligible(riderBike, stopA, {}, RADIO_METERS), null);
});

test("VEHÍCULO INCOMPATIBLE: la oferta respeta el vehículo requerido", () => {
  const soloMoto = { id: "c", orderId: "demo-order-3", vehicleTypes: ["MOTORCYCLE"] };
  assert.equal(eligible(riderBike, soloMoto, {}), "incompatible_vehicle");
  assert.equal(eligible({ uid: "rider-2", vehicleType: "MOTORCYCLE" }, soloMoto, {}), null);
});

test("PEDIDO ACTIVO DE OTRO REPARTIDOR: no se ofrece", () => {
  const busy = { "demo-order-1": { uid: "otro", status: "ACCEPTED" } };
  assert.equal(eligible(riderBike, stopA, busy), "order_busy");
  assert.equal(eligible(riderBike, stopA, { "demo-order-1": { uid: "otro", status: "OFFERED" } }), "order_busy");
});

test("PEDIDO LIBRE: mismo repartidor o liberado no bloquea", () => {
  assert.equal(eligible(riderBike, stopA, { "demo-order-1": { uid: "rider-1", status: "ACCEPTED" } }), null);
  assert.equal(eligible(riderBike, stopA, { "demo-order-1": { uid: "otro", status: "RELEASED" } }), null);
  assert.equal(eligible(riderBike, stopA, {}), null);
});
