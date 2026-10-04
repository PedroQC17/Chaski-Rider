"use strict";
// Datos sintéticos. No representan rutas transitables ni pedidos de Chaski Food.
const points = {
  rider: { id: "rider", latitude: -12.055, longitude: -77.048 },
  store: { id: "store", latitude: -12.0464, longitude: -77.0428 },
  a: { id: "a", latitude: -12.037, longitude: -77.035 },
  b: { id: "b", latitude: -12.044, longitude: -77.023 },
  c: { id: "c", latitude: -12.055, longitude: -77.029 }
};
const stops = ["a", "b", "c"].map((id, i) => ({
  id, orderId: `demo-order-${i + 1}`, merchantId: "demo-store", point: points[id]
}));
const meters = {
  store: { a: 1600, b: 2500, c: 2300 },
  a: { b: 1800, c: 3300 },
  b: { a: 2100, c: 1700 },
  c: { a: 3400, b: 1900 }
};
module.exports = { points, stops, meters };
