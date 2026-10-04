"use strict";
// Datos sintéticos. No representan rutas transitables ni pedidos de Chaski Food.
// HU07: pago base fijo y propina conocida, ambos conceptos visibles por separado.
const BASE_CENTS = 400;
const TIP_CENTS = 250;
const points = {
  rider: { id: "rider", latitude: -12.055, longitude: -77.048, zone: "Zona de ejemplo" },
  store: { id: "store", name: "Tienda Demo Chaski", zone: "Centro de Lima", latitude: -12.0464, longitude: -77.0428 },
  a: { id: "a", latitude: -12.037, longitude: -77.035, zone: "Miraflores" },
  b: { id: "b", latitude: -12.044, longitude: -77.023, zone: "San Isidro" },
  c: { id: "c", latitude: -12.055, longitude: -77.029, zone: "Surquillo" }
};
const stops = ["a", "b", "c"].map((id, i) => ({
  id, orderId: `demo-order-${i + 1}`, merchantId: "demo-store", zone: points[id].zone,
  vehicleTypes: ["BICYCLE", "MOTORCYCLE"], point: points[id]
}));
const meters = {
  rider: { store: 900 },
  store: { a: 1600, b: 2500, c: 2300 },
  a: { b: 1800, c: 3300 },
  b: { a: 2100, c: 1700 },
  c: { a: 3400, b: 1900 }
};
// HU05: zonas de demanda de demostración (nivel y centro del área).
const demandZones = [
  { id: "z-centro", name: "Centro de Lima", level: "HIGH", latitude: -12.0464, longitude: -77.0428, radiusMeters: 1600 },
  { id: "z-miraflores", name: "Miraflores", level: "MEDIUM", latitude: -12.037, longitude: -77.035, radiusMeters: 1400 },
  { id: "z-san-isidro", name: "San Isidro", level: "LOW", latitude: -12.044, longitude: -77.023, radiusMeters: 1300 }
];
module.exports = { points, stops, meters, demandZones, BASE_CENTS, TIP_CENTS };
