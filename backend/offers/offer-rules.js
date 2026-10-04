"use strict";
// HU06: reglas que deciden si un pedido puede ofrecerse a un repartidor.
// Zona/radio, compatibilidad de vehículo y pedidos activos de otros repartidores.
const fixture = require("./demo-fixture");
const RADIO_METERS = 6000;
const ASSIGN_REASON = "PROXIMITY";

function eligible(rider, stop, busyOrders = {}, riderStoreMeters = fixture.meters.rider.store) {
  if (!Number.isFinite(riderStoreMeters) || riderStoreMeters > RADIO_METERS) return "out_of_zone";
  if (Array.isArray(stop.vehicleTypes) && !stop.vehicleTypes.includes(rider.vehicleType || "NONE"))
    return "incompatible_vehicle";
  const busy = busyOrders[stop.orderId];
  if (busy && busy.uid !== rider.uid && (busy.status === "OFFERED" || busy.status === "ACCEPTED"))
    return "order_busy";
  return null;
}

module.exports = { eligible, RADIO_METERS, ASSIGN_REASON };
