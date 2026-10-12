const { test } = require("node:test");
const assert = require("node:assert/strict");
const { assertEditable, submissionPatch, reviewPatch } = require("../registration-review");
const pending = { status: "PENDING_REVIEW", vehicleType: "BICYCLE", isEnabled: true, submissionVersion: 2 };
const decision = { status: "APPROVED", submissionVersion: 2 };
test("only incomplete and correctable registrations can change", () => {
  assertEditable(null);
  for (const status of ["INCOMPLETE", "NEEDS_CORRECTION"]) assertEditable({ status, isEnabled: true });
  for (const status of ["APPROVED", "PENDING_REVIEW", "REJECTED", "UNKNOWN"])
    assert.throws(() => assertEditable({ status }), { code: "failed-precondition" });
  assert.throws(() => assertEditable({ status: "INCOMPLETE", isEnabled: false }));
});
test("rejection requires a reason and is terminal for rider edits and submissions", () => {
  const patch = reviewPatch(pending, { ...decision, status: "REJECTED", reason: "Identidad no válida" });
  assert.equal(patch.status, "REJECTED");
  assert.equal(patch.rejectionReason, "Identidad no válida");
  assert.equal(patch.isAvailable, false);
  const rider = { ...pending, ...patch };
  assert.throws(() => assertEditable(rider));
  assert.throws(() => submissionPatch(rider));
  assert.throws(() => reviewPatch(rider, decision));
  for (const reason of [undefined, "", " ", "x".repeat(1001)])
    assert.throws(() => reviewPatch(pending, { ...decision, status: "REJECTED", reason }), { code: "invalid-argument" });
});
test("decisions cannot apply to an old submission, disabled account or non-pending state", () => {
  assert.throws(() => reviewPatch(pending, { ...decision, submissionVersion: 1 }), { code: "failed-precondition" });
  for (const submissionVersion of [undefined, -1, 1.5, "2"])
    assert.throws(() => reviewPatch(pending, { ...decision, submissionVersion }), { code: "invalid-argument" });
  assert.throws(() => reviewPatch({ ...pending, isEnabled: false }, decision));
  assert.throws(() => reviewPatch({ ...pending, status: "INCOMPLETE" }, decision));
});
test("legacy pending registration without version can be reviewed with explicit version zero", () => {
  const patch = reviewPatch({ ...pending, submissionVersion: undefined }, { ...decision, submissionVersion: 0 });
  assert.equal(patch.status, "APPROVED");
});
test("approval confirms all required documents and clears old observations", () => {
  const patch = reviewPatch({ ...pending, rejectionReason: "old" }, decision);
  assert.equal(patch.rejectionReason, "");
  assert.deepEqual(Object.keys(patch.documentReviews), ["dniFront", "dniBack", "bankStatement"]);
  assert.ok(Object.values(patch.documentReviews).every(v => v.status === "APPROVED"));
});
test("correctable observation identifies the document and retains general instructions", () => {
  const patch = reviewPatch(pending, { ...decision, status: "NEEDS_CORRECTION", reason: "Reemplaza la foto borrosa",
    documentObservations: { dniFront: "No se puede leer el número" } });
  assert.equal(patch.documentReviews.dniFront.status, "OBSERVED");
  assert.equal(patch.documentReviews.dniFront.reason, "No se puede leer el número");
  assert.equal(patch.documentReviews.dniBack.status, "PENDING");
  assertEditable({ ...pending, ...patch });
  assert.throws(() => submissionPatch({ ...pending, ...patch }), /Reemplaza los documentos/);
});
test("after observed document replacement a new submission starts a new review round", () => {
  const patch = submissionPatch({ ...pending, status: "NEEDS_CORRECTION", rejectionReason: "old",
    documentReviews: { dniFront: { status: "PENDING", reason: "" } } });
  assert.equal(patch.submissionVersion, 3);
  assert.equal(patch.status, "PENDING_REVIEW");
  assert.equal(patch.rejectionReason, "");
  assert.ok(Object.values(patch.documentReviews).every(v => v.status === "PENDING"));
  assert.throws(() => reviewPatch({ ...pending, ...patch }, decision), /registro cambió/);
});
test("document observations reject unrelated types, empty reasons and conflicting decisions", () => {
  for (const documentObservations of [{ soat: "not required" }, { arbitrary: "x" }, { dniFront: "" }, []])
    assert.throws(() => reviewPatch(pending, { ...decision, status: "NEEDS_CORRECTION", reason: "Corrige", documentObservations }));
  assert.throws(() => reviewPatch(pending, { ...decision, documentObservations: { dniFront: "x" } }));
  assert.throws(() => reviewPatch(pending, { ...decision, status: "BANNED" }));
});
test("motorcycle review includes driving licence and SOAT", () => {
  const patch = reviewPatch({ ...pending, vehicleType: "MOTORCYCLE" }, decision);
  assert.equal(Object.keys(patch.documentReviews).length, 5);
  assert.equal(patch.documentReviews.soat.status, "APPROVED");
});
