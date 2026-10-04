const { test } = require("node:test");
const assert = require("node:assert/strict");
const v = require("../validation");
const person = { name: "Ana", lastName: "Perez", dni: "00123456", phone: "987654321", termsAccepted: true };
test("normalizes phone without losing leading DNI zeros", () => {
  const result = v.personal(person);
  assert.equal(result.phone, "+51987654321");
  assert.equal(result.dni, "00123456");
});
test("rejects invalid identity data and missing acceptance", () => {
  for (const input of [{ dni: "123" }, { phone: "123" }, { termsAccepted: false }, { name: "" }])
    assert.throws(() => v.personal({ ...person, ...input }));
});
test("motorcycles require additional documents", () => {
  assert.deepEqual(v.requiredDocuments("BICYCLE"), ["dniFront", "dniBack", "bankStatement"]);
  assert.equal(v.requiredDocuments("MOTORCYCLE").length, 5);
  assert.throws(() => v.requiredDocuments("CAR"));
});
test("bank requires account or a valid CCI", () => {
  // Whitelist real: BCP/INTERBANK/BBVA (igual que la app). El dato "Bank" estaba desactualizado.
  assert.throws(() => v.bank({ bankName: "BCP", holderName: "Ana", accountNumber: "", cci: "" }));
  assert.throws(() => v.bank({ bankName: "BCP", holderName: "Ana", accountNumber: "", cci: "123" }));
  assert.equal(v.bank({ bankName: "BCP", holderName: "Ana", accountNumber: "", cci: "00123456789012345678" }).cci.length, 20);
});
test("rejects empty, oversized and unsupported uploads", () => {
  v.validateMetadata({ contentType: "application/pdf", size: v.MAX_BYTES });
  for (const metadata of [{contentType:"text/plain",size:20}, {contentType:"image/png",size:0}, {contentType:"image/jpeg",size:v.MAX_BYTES+1}])
    assert.throws(() => v.validateMetadata(metadata));
});
