"use strict";
const MAX_BYTES = 10 * 1024 * 1024;
const MIME_TYPES = ["application/pdf", "image/jpeg", "image/png"];
const DOC_FIELDS = { dniFront: "dniFrontUrl", dniBack: "dniBackUrl", driverLicense: "driverLicenseUrl", soat: "soatUrl", bankStatement: "bankStatementUrl" };
function text(value, max = 150) {
  if (typeof value !== "string" || value.trim().length > max) throw Error("Información no válida");
  return value.trim();
}
function personal(input) {
  const name = text(input.name), lastName = text(input.lastName);
  const dni = text(input.dni, 8);
  let phone = text(input.phone, 30).replace(/[\s()-]/g, "");
  if (/^9[0-9]{8}$/.test(phone)) phone = "+51" + phone;
  if (!name || !lastName) throw Error("Completa tus nombres y apellidos");
  if (!/^[0-9]{8}$/.test(dni)) throw Error("El DNI debe tener 8 dígitos");
  if (!/^\+[1-9][0-9]{7,14}$/.test(phone)) throw Error("Ingresa un celular válido con código de país");
  if (input.termsAccepted !== true) throw Error("Debes aceptar los términos y la política de privacidad");
  return { name, lastName, dni, phone, termsAccepted: true };
}
function vehicle(value) {
  if (!["BICYCLE", "MOTORCYCLE"].includes(value)) throw Error("Selecciona bicicleta o motocicleta");
  return value;
}
function bank(input = {}) {
  const bankName = text(input.bankName), holderName = text(input.holderName);
  const accountNumber = text(input.accountNumber, 40), cci = text(input.cci, 20);
  if (!["BCP", "INTERBANK", "BBVA"].includes(bankName) || !holderName) throw Error("Completa el banco y el titular");
  if (!/^[0-9]{20}$/.test(cci)) throw Error("El CCI debe tener 20 dígitos");
  return { bankName, holderName, accountNumber, cci };
}
function requiredDocuments(type) {
  vehicle(type);
  return ["dniFront", "dniBack", "bankStatement"].concat(type === "MOTORCYCLE" ? ["driverLicense", "soat"] : []);
}
function validateMetadata(metadata) {
  if (!MIME_TYPES.includes(metadata.contentType) || Number(metadata.size) <= 0 || Number(metadata.size) > MAX_BYTES)
    throw Error("Selecciona un PDF, JPG o PNG de hasta 10 MB");
}
module.exports = { MAX_BYTES, MIME_TYPES, DOC_FIELDS, personal, vehicle, bank, requiredDocuments, validateMetadata };
