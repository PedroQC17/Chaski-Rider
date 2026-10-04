"use strict";
const { onRequest } = require("firebase-functions/v2/https");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");
const { randomUUID } = require("node:crypto");
const { quote } = require("./route-quote");
const rules = require("./offer-rules");
const fixture = require("./demo-fixture");
const empty = () => ({ revision: 0, pickedUp: false, batch: null, offer: null, decisions: {}, requests: {} });
exports.riderOfferDemo = onRequest({ region: "us-central1", maxInstances: 2, minInstances: 0,
  invoker: "public", timeoutSeconds: 30 }, async (req, res) => {
  res.set("Cache-Control", "no-store");
  if (req.method !== "POST") return res.status(405).json({ code: "method_not_allowed" });
  let uid;
  try {
    const token = /^Bearer (.+)$/.exec(req.headers.authorization || "")?.[1];
    if (!token) throw new Error("missing_token");
    uid = (await getAuth().verifyIdToken(token, true)).uid;
  } catch (_) { return res.status(401).json({ code: "unauthenticated" }); }
  const { action, offerId, requestId } = req.body || {};
  if (!["state", "offer", "accept", "reject", "pickup", "reset"].includes(action) ||
      (action !== "state" && (typeof requestId !== "string" || !/^[a-f0-9-]{36}$/.test(requestId))) ||
      (["accept", "reject"].includes(action) && (typeof offerId !== "string" || offerId.length > 80)))
    return res.status(400).json({ code: "invalid_request" });
  try {
    const db = getFirestore(), ref = db.collection("riderOfferDemos").doc(uid);
    const result = await db.runTransaction(async tx => {
      const [profile, doc] = await Promise.all([tx.get(db.collection("riders").doc(uid)), tx.get(ref)]);
      const rider = profile.data();
      if (!rider || rider.status !== "APPROVED" || rider.isEnabled === false || !rider.profilePhotoPath)
        return { status: 403, body: { code: "rider_unavailable" } };
      let state = doc.data() || empty();
      state.requests ||= {};
      const signature = `${action}:${offerId || ""}`;
      if (state.requests[requestId] && state.requests[requestId] !== signature)
        return { status: 409, body: { code: "request_conflict" } };
      const operation = state.requests[requestId] ? "state" : action;
      const now = Date.now();
      // Registro compartido de pedidos: quién tiene cada pedido activo (HU06).
      const orderDocs = await Promise.all(fixture.stops.map(s =>
        tx.get(db.collection("riderOfferDemoOrders").doc(s.orderId))));
      const busy = {};
      fixture.stops.forEach((s, i) => { if (orderDocs[i].exists) busy[s.orderId] = orderDocs[i].data(); });
      const orderWrites = [];
      const markOrder = (orderId, status) => { if (orderId) orderWrites.push({ orderId, status }); };
      let changed = false, code = null;
      if (state.offer && state.offer.expiresAt <= now) {
        state.decisions[state.offer.id] = "expired";
        if (busy[state.offer.orderId]?.uid === uid && busy[state.offer.orderId]?.status === "OFFERED")
          markOrder(state.offer.orderId, "RELEASED");
        state.offer = null; changed = true;
      }
      if (operation === "reset") {
        (state.batch?.stops || []).forEach(s => markOrder(s.orderId, "RELEASED"));
        if (busy[state.offer?.orderId]?.uid === uid && busy[state.offer?.orderId]?.status === "OFFERED")
          markOrder(state.offer.orderId, "RELEASED");
        state = empty(); changed = true;
      }
      if (operation === "offer" && !state.offer) {
        // HU04: sin disponibilidad activa no se generan ofertas nuevas.
        if (rider.isAvailable !== true)
          return { status: 403, body: { code: "rider_unavailable" } };
        const accepted = state.batch?.stops || [];
        if (state.pickedUp || accepted.length >= 3) code = "batch_closed";
        else {
          // HU06: siguiente pedido que cumple zona, vehículo y no esté activo para otro repartidor.
          const next = fixture.stops.find(s => !accepted.some(a => a.id === s.id) &&
            rules.eligible({ uid, vehicleType: rider.vehicleType }, s, busy) === null);
          if (!next) code = "no_eligible_order";
          else {
            state.offer = { id: randomUUID(), revision: state.revision, expiresAt: now + 45000,
              orderId: next.orderId, reason: rules.ASSIGN_REASON, reasonMeters: fixture.meters.rider.store,
              quote: quote([...accepted, next], state.batch?.totalCents || 0) };
            markOrder(next.orderId, "OFFERED");
            changed = true;
          }
        }
      }
      if (operation === "accept" || operation === "reject") {
        const decided = state.decisions[offerId];
        if (decided === (action === "accept" ? "accepted" : "rejected")) { /* Reintento idempotente. */ }
        else if (!state.offer || state.offer.id !== offerId || state.offer.revision !== state.revision || state.pickedUp)
          code = decided === "expired" ? "offer_expired" : "offer_conflict";
        else {
          if (action === "accept") {
            state.batch = state.offer.quote; state.revision++;
            // HU05/HU06: al aceptar el pedido queda como ACCEPTADO para este repartidor y no se ofrece a otros.
            markOrder(state.offer.orderId, "ACCEPTED");
          } else markOrder(state.offer.orderId, "RELEASED");
          state.decisions[offerId] = action === "accept" ? "accepted" : "rejected";
          state.offer = null; changed = true;
        }
      }
      if (operation === "pickup") {
        if (!state.batch || state.offer) code = "offer_conflict";
        else if (!state.pickedUp) { state.pickedUp = true; state.revision++; changed = true; }
      }
      // Acotar el historial de esta herramienta de demostración.
      state.decisions = Object.fromEntries(Object.entries(state.decisions).slice(-30));
      if (operation !== "state" && !code) {
        state.requests[requestId] = signature;
        state.requests = Object.fromEntries(Object.entries(state.requests).slice(-50));
        changed = true;
      }
      // Nunca tocar pedidos activos de otro repartidor.
      for (const w of orderWrites) {
        if (busy[w.orderId] && busy[w.orderId].uid !== uid) continue;
        tx.set(db.collection("riderOfferDemoOrders").doc(w.orderId),
          { uid, status: w.status, updatedAt: now });
      }
      if (changed) tx.set(ref, state);
      // HU05: zonas de demanda con su instante de actualización.
      const demandZones = fixture.demandZones.map(z => ({ ...z, updatedAt: now }));
      return { status: code ? 409 : 200, body: { code, serverTime: now, revision: state.revision,
        pickedUp: state.pickedUp, batch: state.batch, offer: state.offer, merchant: fixture.points.store,
        demandZones } };
    });
    return res.status(result.status).json(result.body);
  } catch (e) {
    console.error("riderOfferDemo", e.code || e.name);
    return res.status(500).json({ code: "unavailable" });
  }
});
