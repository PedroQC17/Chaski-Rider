"use strict";
const { requiredDocuments } = require("./validation");

function fail(code, message) { const error = new Error(message); error.code = code; throw error; }
function reason(value, label) {
  if (typeof value !== "string" || !value.trim() || value.trim().length > 1000)
    fail("invalid-argument", label);
  return value.trim();
}
function assertEditable(rider) {
  if (rider && (!["INCOMPLETE", "NEEDS_CORRECTION"].includes(rider.status) || rider.isEnabled === false))
    fail("failed-precondition", "Este registro no admite cambios en su estado actual");
}
function submissionPatch(rider) {
  assertEditable(rider);
  const required = requiredDocuments(rider.vehicleType);
  if (required.some(type => rider.documentReviews?.[type]?.status === "OBSERVED"))
    fail("failed-precondition", "Reemplaza los documentos observados antes de volver a enviar");
  return {
    status: "PENDING_REVIEW", currentStep: 3, isAvailable: false, rejectionReason: "",
    submissionVersion: (rider.submissionVersion || 0) + 1,
    documentReviews: Object.fromEntries(required.map(type => [type, { status: "PENDING", reason: "" }]))
  };
}
function reviewPatch(rider, input) {
  if (!rider || rider.status !== "PENDING_REVIEW" || rider.isEnabled === false)
    fail("failed-precondition", "El registro no está pendiente o la cuenta no está habilitada");
  if (!Number.isSafeInteger(input.submissionVersion) || input.submissionVersion < 0)
    fail("invalid-argument", "Indica la versión del envío que estás revisando");
  if (input.submissionVersion !== (rider.submissionVersion || 0))
    fail("failed-precondition", "El registro cambió. Actualiza la información antes de revisar");
  if (!["APPROVED", "NEEDS_CORRECTION", "REJECTED"].includes(input.status))
    fail("invalid-argument", "Decisión de revisión no válida");
  const rejectionReason = input.status === "APPROVED" ? "" :
    reason(input.reason, "Indica el motivo que verá el repartidor");
  const required = requiredDocuments(rider.vehicleType);
  const observations = input.documentObservations || {};
  if (typeof observations !== "object" || Array.isArray(observations))
    fail("invalid-argument", "Observaciones de documentos no válidas");
  if (Object.keys(observations).some(type => !required.includes(type)))
    fail("invalid-argument", "Solo puedes observar documentos requeridos para este vehículo");
  if (input.status !== "NEEDS_CORRECTION" && Object.keys(observations).length)
    fail("invalid-argument", "Las observaciones corregibles solo corresponden a una solicitud de corrección");
  const documentReviews = Object.fromEntries(required.map(type => {
    if (Object.hasOwn(observations, type))
      return [type, { status: "OBSERVED", reason: reason(observations[type], "Indica qué debe corregirse en el documento") }];
    // An overall approval confirms all required documents. Other decisions do not imply approval.
    return [type, { status: input.status === "APPROVED" ? "APPROVED" : "PENDING", reason: "" }];
  }));
  return { status: input.status, rejectionReason, documentReviews, isAvailable: false };
}
module.exports = { assertEditable, submissionPatch, reviewPatch };
