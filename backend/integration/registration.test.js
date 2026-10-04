const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const { initializeApp, deleteApp } = require("firebase/app");
const { getAuth, connectAuthEmulator, createUserWithEmailAndPassword } = require("firebase/auth");
const { getFunctions, connectFunctionsEmulator, httpsCallable } = require("firebase/functions");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, getDoc, setDoc, collection, getDocs } = require("firebase/firestore");
const { ref, uploadBytes, getBytes } = require("firebase/storage");
const { randomUUID } = require("node:crypto");
const projectId = "demo-chaski-rider";
let env;
const apps = [];
before(async () => {
  env = await initializeTestEnvironment({ projectId,
    firestore: { host: "127.0.0.1", port: 8080, rules: fs.readFileSync(require("node:path").join(__dirname, "../../firestore.rules"), "utf8") },
    storage: { host: "127.0.0.1", port: 9199, rules: fs.readFileSync(require("node:path").join(__dirname, "../../storage.rules"), "utf8") } });
});
after(async () => { if (env) await env.cleanup(); await Promise.all(apps.map(deleteApp)); });
async function user() {
  const app = initializeApp({ apiKey: "demo-api-key", projectId, storageBucket: projectId + ".appspot.com" }, randomUUID());
  apps.push(app);
  const auth = getAuth(app);
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  const credential = await createUserWithEmailAndPassword(auth, randomUUID() + "@example.test", "Password123!");
  const functions = getFunctions(app);
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
  return { uid: credential.user.uid, call: httpsCallable(functions, "riderRegistration"), review: httpsCallable(functions, "reviewRider") };
}
const personal = (dni, phone) => ({ action: "personal", name: "Ana", lastName: "Perez", dni, phone, termsAccepted: true });
test("simultaneous registrations reserve a DNI only once", async () => {
  const a = await user(), b = await user();
  const results = await Promise.allSettled([
    a.call(personal("00111111", "+51911111111")), b.call(personal("00111111", "+51922222222"))
  ]);
  assert.equal(results.filter(r => r.status === "fulfilled").length, 1);
  assert.equal(results.find(r => r.status === "rejected").reason.code, "functions/already-exists");
});
test("phone uniqueness, own edits, and release of replaced reservations", async () => {
  const a = await user(), b = await user();
  await a.call(personal("00222222", "+51933333333"));
  await a.call(personal("00222222", "+51 933-333-333"));
  await assert.rejects(b.call(personal("00333333", "+51933333333")), /número ya/);
  await a.call(personal("00222222", "+51944444444"));
  await b.call(personal("00333333", "+51933333333"));
});
test("owners cannot approve themselves, list riders or read another rider", async () => {
  const a = await user(), b = await user();
  await a.call(personal("00444444", "+51955555555"));
  const db = env.authenticatedContext(a.uid).firestore();
  await assertSucceeds(getDoc(doc(db, "riders", a.uid)));
  await assertFails(getDoc(doc(db, "riders", b.uid)));
  await assertFails(getDocs(collection(db, "riders")));
  await assertFails(setDoc(doc(db, "riders", a.uid), { status: "APPROVED" }, { merge: true }));
  await assert.rejects(a.review({ uid: a.uid, status: "APPROVED" }), /Acceso restringido/);
});
test("document requirements and private immutable uploads are enforced", async () => {
  const a = await user(), b = await user();
  await a.call(personal("00555555", "+51966666666"));
  await a.call({ action: "vehicle", vehicleType: "MOTORCYCLE" });
  await a.call({ action: "bank", bankInfo: { bankName: "Bank", holderName: "Ana", accountNumber: "12345", cci: "" } });
  await assert.rejects(a.call({ action: "submit" }));
  const ownStorage = env.authenticatedContext(a.uid).storage();
  const otherStorage = env.authenticatedContext(b.uid).storage();
  const pdf = new TextEncoder().encode("%PDF-1.7\n test document");
  for (const type of ["dniFront", "dniBack", "bankStatement", "driverLicense", "soat"]) {
    const path = "riders/" + a.uid + "/documents/" + type + "/" + randomUUID();
    await assertSucceeds(uploadBytes(ref(ownStorage, path), pdf, { contentType: "application/pdf" }));
    await assertFails(uploadBytes(ref(ownStorage, path), pdf, { contentType: "application/pdf" }));
    await assertFails(getBytes(ref(otherStorage, path)));
    await a.call({ action: "document", docType: type, path });
  }
  await a.call({ action: "submit" });
  await a.call({ action: "submit" }); // Retry must not send twice or fail.
  await assert.rejects(a.call({ action: "vehicle", vehicleType: "BICYCLE" }));
  const final = await getDoc(doc(env.authenticatedContext(a.uid).firestore(), "riders", a.uid));
  assert.equal(final.data().status, "PENDING_REVIEW");
  await assertFails(uploadBytes(ref(ownStorage, "riders/" + a.uid + "/documents/soat/" + randomUUID()), pdf, { contentType: "application/pdf" }));
});
test("forged PDF contents and unsupported uploads are rejected", async () => {
  const a = await user();
  await a.call(personal("00666666", "+51977777777"));
  const storage = env.authenticatedContext(a.uid).storage();
  const path = "riders/" + a.uid + "/documents/dniFront/" + randomUUID();
  await assertFails(uploadBytes(ref(storage, path), new Uint8Array([1]), { contentType: "text/plain" }));
  await uploadBytes(ref(storage, path), new Uint8Array([1,2,3,4,5,6,7,8]), { contentType: "application/pdf" });
  await assert.rejects(a.call({ action: "document", docType: "dniFront", path }), /contenido no corresponde/);
});
