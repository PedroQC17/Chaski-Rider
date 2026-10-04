"use strict";
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const { createHash } = require("node:crypto");
const v = require("./validation");
initializeApp();
const db = getFirestore();
const reservation = (kind, value) => db.collection("riderUnique").doc(kind + "_" + createHash("sha256").update(value).digest("hex"));
const validated = (fn) => { try { return fn(); } catch (e) { throw new HttpsError("invalid-argument", e.message); } };
// Estados en los que se permite editar el perfil; submit solo desde INCOMPLETE/NEEDS_CORRECTION.
const EDITABLE_STATUSES = ["INCOMPLETE", "NEEDS_CORRECTION"];
const editable = (rider, action) => {
  if (!rider) return;
  const statuses = action === "submit" ? ["INCOMPLETE", "NEEDS_CORRECTION"] : EDITABLE_STATUSES;
  if (!statuses.includes(rider.status) || rider.isEnabled === false)
    throw new HttpsError("failed-precondition", "Este registro no admite cambios en su estado actual");
};

async function checkDocument(uid, type, path) {
  if (!v.DOC_FIELDS[type] || typeof path !== "string" ||
      !new RegExp("^riders/" + uid + "/documents/" + type + "/[a-f0-9-]{36}$").test(path))
    throw new HttpsError("invalid-argument", "Referencia de documento no válida");
  try {
    const file = getStorage().bucket().file(path);
    const [metadata] = await file.getMetadata();
    validated(() => v.validateMetadata(metadata));
    // Inspect the signature too; a client-controlled MIME string is insufficient.
    const [header] = await file.download({ start: 0, end: 7 });
    const valid = metadata.contentType === "application/pdf" ? header.subarray(0, 5).toString() === "%PDF-" :
      metadata.contentType === "image/png" ? header.equals(Buffer.from([137,80,78,71,13,10,26,10])) :
      header[0] === 255 && header[1] === 216 && header[2] === 255;
    if (!valid) throw new HttpsError("invalid-argument", "El contenido no corresponde a un PDF, JPG o PNG válido");
  } catch (e) {
    if (e instanceof HttpsError) throw e;
    throw new HttpsError("failed-precondition", "No se pudo comprobar el archivo. Vuelve a subirlo.");
  }
}


async function checkProfilePhoto(uid, path) {
  if (typeof path !== "string" || !new RegExp("^riders/" + uid + "/profilePhoto/[a-f0-9-]{36}$").test(path))
    throw new HttpsError("invalid-argument", "Referencia de foto no válida");
  try {
    const file = getStorage().bucket().file(path);
    const [metadata] = await file.getMetadata();
    if (metadata.contentType !== "image/jpeg" || Number(metadata.size) <= 0 || Number(metadata.size) > 5 * 1024 * 1024)
      throw new HttpsError("invalid-argument", "Foto no válida");
    const [header] = await file.download({ start: 0, end: 2 });
    if (header[0] !== 255 || header[1] !== 216 || header[2] !== 255)
      throw new HttpsError("invalid-argument", "Foto no válida");
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("failed-precondition", "No se pudo guardar la foto. Inténtalo nuevamente.");
  }
}

exports.riderRegistration = onCall({ region: "us-central1", maxInstances: 10, invoker: "public" }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Inicia sesión para continuar");
  const uid = request.auth.uid;
  const input = request.data || {};
  const ref = db.collection("riders").doc(uid);
  if (!["personal", "vehicle", "bank", "document", "submit", "availability", "profilePhoto"].includes(input.action))
    throw new HttpsError("invalid-argument", "Acción no válida");
  if (input.action === "document") await checkDocument(uid, input.docType, input.path);
  if (input.action === "profilePhoto") await checkProfilePhoto(uid, input.path);

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    const rider = snapshot.exists ? snapshot.data() : null;
    // Repeated submits are idempotent.
    if (input.action === "submit" && rider?.status === "PENDING_REVIEW") return;
    if (input.action === "profilePhoto") {
      if (!rider || rider.status !== "APPROVED" || rider.isEnabled === false)
        throw new HttpsError("failed-precondition", "Solo una cuenta aprobada puede añadir su foto");
      if (rider.profilePhotoPath) {
        if (rider.profilePhotoPath === input.path) return;
        throw new HttpsError("failed-precondition", "La foto de perfil ya fue registrada");
      }
      tx.update(ref, { profilePhotoPath: input.path, profilePhotoCreatedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
      return;
    }
    // HU04: la disponibilidad solo cambia para repartidores aprobados y habilitados.
    if (input.action === "availability") {
      if (!rider || rider.status !== "APPROVED" || rider.isEnabled === false)
        throw new HttpsError("failed-precondition", "Solo los repartidores habilitados pueden cambiar su disponibilidad");
      if (input.isAvailable === true && !rider.profilePhotoPath)
        throw new HttpsError("failed-precondition", "Añade tu foto de perfil para conectarte.");
      tx.set(ref, { ...rider, isAvailable: input.isAvailable === true, updatedAt: FieldValue.serverTimestamp() });
      return;
    }
    editable(rider, input.action);
    const base = rider || {
      id: uid, email: request.auth.token.email || "", status: "INCOMPLETE", currentStep: 1,
      vehicleType: "NONE", isEnabled: true, isAvailable: false, rejectionReason: ""
    };
    const patch = { updatedAt: FieldValue.serverTimestamp() };

    if (input.action === "personal") {
      const data = validated(() => v.personal(input));
      const dniRef = reservation("dni", data.dni), phoneRef = reservation("phone", data.phone);
      const [dniLock, phoneLock] = await Promise.all([tx.get(dniRef), tx.get(phoneRef)]);
      if (dniLock.exists && dniLock.data().uid !== uid) throw new HttpsError("already-exists", "Este DNI ya está siendo usado");
      if (phoneLock.exists && phoneLock.data().uid !== uid) throw new HttpsError("already-exists", "Este número ya está vinculado a una cuenta");
      // Include profiles created by the previous app before reservation indexes existed.
      const [oldDni, oldPhone] = await Promise.all([
        tx.get(db.collection("riders").where("dni", "==", data.dni)),
        tx.get(db.collection("riders").where("phone", "in", [data.phone, data.phone.startsWith("+51") ? data.phone.slice(3) : data.phone]))
      ]);
      if (oldDni.docs.some(d => d.id !== uid)) throw new HttpsError("already-exists", "Este DNI ya está siendo usado");
      if (oldPhone.docs.some(d => d.id !== uid)) throw new HttpsError("already-exists", "Este número ya está vinculado a una cuenta");
      const releases = [];
      for (const kind of ["dni", "phone"]) {
        if (base[kind] && base[kind] !== data[kind]) {
          const oldRef = reservation(kind, base[kind]), old = await tx.get(oldRef);
          if (old.exists && old.data().uid === uid) releases.push(oldRef);
        }
      }
      releases.forEach(r => tx.delete(r));
      tx.set(dniRef, { uid }); tx.set(phoneRef, { uid });
      Object.assign(patch, data, { currentStep: base.vehicleType !== "NONE" ? 3 : 2, phoneVerified: false,
        termsAcceptedAt: FieldValue.serverTimestamp() });
    } else {
      if (!rider) throw new HttpsError("failed-precondition", "Completa primero tus datos personales");
      if (input.action === "vehicle") {
        Object.assign(patch, { vehicleType: validated(() => v.vehicle(input.vehicleType)), currentStep: 3 });
      } else if (input.action === "bank") {
        patch.bankInfo = validated(() => v.bank(input.bankInfo));
      } else if (input.action === "document") {
        patch[v.DOC_FIELDS[input.docType]] = input.path;
      } else if (input.action === "submit") {
        validated(() => v.personal(base));
        validated(() => v.bank(base.bankInfo));
        const required = validated(() => v.requiredDocuments(base.vehicleType));
        const [dniLock, phoneLock] = await Promise.all([
          tx.get(reservation("dni", base.dni)), tx.get(reservation("phone", base.phone))
        ]);
        if (dniLock.data()?.uid !== uid || phoneLock.data()?.uid !== uid)
          throw new HttpsError("failed-precondition", "Vuelve al paso 1 para validar tu DNI y celular");
        await Promise.all(required.map(type => checkDocument(uid, type, base[v.DOC_FIELDS[type]])));
        Object.assign(patch, { status: "PENDING_REVIEW", currentStep: 3, isAvailable: false,
          rejectionReason: "", submittedAt: FieldValue.serverTimestamp() });
      }
    }
    tx.set(ref, { ...base, ...patch });
  });
  return { ok: true };
});

// Called only by a trusted administrative account with the riderReviewer custom claim.
// Assign the claim using Admin SDK in a trusted environment; never from the Android app.
exports.reviewRider = onCall({ region: "us-central1", maxInstances: 5, invoker: "public" }, async (request) => {
  if (request.auth?.token.riderReviewer !== true) throw new HttpsError("permission-denied", "Acceso restringido");
  const { uid, status, reason } = request.data || {};
  if (typeof uid !== "string" || !uid || uid.includes("/") || !["APPROVED", "NEEDS_CORRECTION"].includes(status))
    throw new HttpsError("invalid-argument", "Revisión no válida");
  const rejectionReason = status === "NEEDS_CORRECTION" ? validated(() => vText(reason)) : "";
  await db.runTransaction(async tx => {
    const ref = db.collection("riders").doc(uid), snap = await tx.get(ref);
    if (snap.data()?.status !== "PENDING_REVIEW") throw new HttpsError("failed-precondition", "El registro no está pendiente");
    tx.update(ref, { status, rejectionReason, reviewedAt: FieldValue.serverTimestamp(), reviewedBy: request.auth.uid });
  });
  return { ok: true };
});
function vText(value) {
  if (typeof value !== "string" || !value.trim() || value.length > 1000) throw Error("Indica el motivo de corrección");
  return value.trim();
}

exports.riderOfferDemo = require('./offers/demo-handler').riderOfferDemo;
