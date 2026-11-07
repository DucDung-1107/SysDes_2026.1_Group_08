import { readFileSync, readdirSync } from "node:fs";
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { clinicalModules } from "../src/modules/clinical.ts";
import { receptionModules } from "../src/modules/reception.ts";
import { administrationModules } from "../src/modules/administration.ts";
import { fieldDefaults, serializeFields, validateFields } from "../src/lib/contracts.ts";
import type { ActionSpec, ModuleSpec } from "../src/lib/schema.ts";

function moduleById(id: string): ModuleSpec {
  const spec = clinicalModules.find((item) => item.id === id);
  assert.ok(spec, `Module ${id} must exist`);
  return spec;
}
function actionById(moduleId: string, actionId: string): ActionSpec {
  const action = moduleById(moduleId).actions?.find((item) => item.id === actionId);
  assert.ok(action, `Action ${actionId} must exist`);
  return action;
}
const normalizedPath = (path: string) => path.replace(/\{\w+\}/g, "{}");

it('all workspace reads and actions map to real backend HTTP routes', () => {
  const directory=new URL('../../backend/src/main/java/vn/medora/api/',import.meta.url);
  const implemented=new Set<string>();
  for(const file of readdirSync(directory,{recursive:true}).filter(name=>String(name).endsWith('Controller.java'))) {
    const source=readFileSync(new URL(String(file).replaceAll('\\','/'),directory),'utf8');
    const base=source.match(/@RequestMapping\("([^"]+)"\)/)?.[1]||'';
    for(const annotation of source.matchAll(/@(Get|Post|Put|Delete)Mapping(?:\(([^)]*)\))?/g)) {
      const routes=annotation[2]?[...annotation[2].matchAll(/"([^"]+)"/g)].map(match=>match[1]).filter(route=>route.startsWith('/')):[''];
      for(const route of routes)implemented.add(`${annotation[1].toUpperCase()} ${normalizedPath(base+route)}`);
    }
  }
  const all=[...receptionModules,...clinicalModules,...administrationModules];
  assert.equal(new Set(all.map(spec=>spec.id)).size,all.length);
  for(const spec of all) {
    const check=(method:string,path:string)=>{
      const absolute=path.startsWith('/@admin/')?`/api/admin/facilities/{facility}/${path.slice(8)}`:`/api/facilities/{facility}${path}`;
      assert.ok(implemented.has(`${method} ${normalizedPath(absolute)}`),`${spec.id}: ${method} ${absolute}`);
    };
    if(spec.endpoint)check('GET',spec.endpoint);
    if(spec.detailPath)check('GET',spec.detailPath);
    for(const view of spec.views||[])if(view.path)check('GET',view.path);
    for(const action of [...spec.actions||[],...(spec.views||[]).flatMap(view=>view.actions||[])])check(action.method==='DOWNLOAD'?'GET':action.method==='UPLOAD'?'POST':action.method||'POST',action.path);
  }
});

describe("clinical frontend contracts", () => {
  it("uses HTTP methods and paths present in the actual clinic backend controllers", () => {
    const controllers = [
      "clinical/ClinicalController.java", "care/PrescriptionController.java", "care/ProcedureController.java",
      "care/MedicationCatalogController.java", "attachment/AttachmentController.java", "scheduling/SchedulingController.java", "governance/GovernanceController.java",
    ];
    const implemented = new Set<string>();
    for (const filename of controllers) {
      const source = readFileSync(new URL(`../../backend/src/main/java/vn/medora/api/${filename}`, import.meta.url), "utf8");
      const annotations = source.matchAll(/@(Get|Post|Put|Delete)Mapping\(\s*(?:value\s*=\s*)?"([^"]+)"/g);
      for (const annotation of annotations) implemented.add(`${annotation[1].toUpperCase()} ${normalizedPath(annotation[2])}`);
    }
    function assertRoute(method: string, path: string, context: string) {
      assert.ok(implemented.has(`${method} ${normalizedPath(path)}`), `${context}: ${method} ${path} is not declared by a controller`);
    }
    for (const spec of clinicalModules) {
      if (spec.endpoint) assertRoute("GET", spec.endpoint, spec.id);
      if (spec.detailPath) assertRoute("GET", spec.detailPath, `${spec.id} detail`);
      for (const action of [...spec.actions ?? [], ...(spec.views ?? []).flatMap((view) => view.actions ?? [])]) {
        const method = action.method === "DOWNLOAD" ? "GET" : action.method === "UPLOAD" ? "POST" : action.method ?? "POST";
        assertRoute(method, action.path, `${spec.id}/${action.id}`);
      }
      for (const view of spec.views ?? []) if (view.path) assertRoute("GET", view.path, `${spec.id}/${view.id}`);
    }
  });

  it("keeps incomplete SOAP sections as non-null draft strings for the backend DTO", () => {
    const action = actionById("encounters", "note-create");
    const defaults = fieldDefaults(action.fields ?? []);
    const serialized = serializeFields(action.fields ?? [], defaults);
    assert.deepEqual(validateFields(action.fields ?? [], defaults), {});
    assert.deepEqual(action.body?.(serialized, {}), {
      subjective: "", objective: "", assessment: "", plan: "", version: null,
    });
  });

  it("allows a confirmed no-allergy profile to send empty, non-null structured sets", () => {
    const action = actionById("medication-profile", "profile-save");
    const values = {
      ...fieldDefaults(action.fields ?? []),
      encounterId: "00000000-0000-0000-0000-000000000001",
      allergyStatus: "KNOWN_NONE",
    };
    assert.deepEqual(validateFields(action.fields ?? [], values), {});
    const serialized = serializeFields(action.fields ?? [], values);
    assert.deepEqual(action.body?.(serialized, {}), {
      allergyStatus: "KNOWN_NONE", allergyCodes: [], conditionCodes: [], currentMedicationIds: [], version: null,
    });
  });

  it("serializes nested prescription rows without losing fractional quantity or sending a route ID in the body", () => {
    const action = actionById("prescriptions", "rx-create");
    const values = {
      encounterId: "00000000-0000-0000-0000-000000000001",
      items: [{
        medicationId: "00000000-0000-0000-0000-000000000002",
        dose: "Nội dung kiểm thử", route: "Nội dung kiểm thử", frequency: "Nội dung kiểm thử",
        durationDays: "2", quantity: "0.125", instructions: "Dữ liệu giả lập chỉ dùng kiểm thử hợp đồng",
      }],
    };
    assert.deepEqual(validateFields(action.fields ?? [], values), {});
    const serialized = serializeFields(action.fields ?? [], values);
    const body = action.body?.(serialized, {}) as { items: { quantity: number; durationDays: number }[] };
    assert.deepEqual(Object.keys(body), ["items"]);
    assert.equal(body.items[0].quantity, 0.125);
    assert.equal(body.items[0].durationDays, 2);
  });

  it("supports an empty warning acknowledgement set when the server reports no warnings", () => {
    const action = actionById("prescriptions", "rx-issue");
    const values = {
      ...fieldDefaults(action.fields ?? []), version: 0,
      pharmacistId: "00000000-0000-0000-0000-000000000003",
    };
    assert.deepEqual(validateFields(action.fields ?? [], values), {});
    const serialized = serializeFields(action.fields ?? [], values);
    assert.deepEqual(serialized.acknowledgedWarnings, []);
    assert.equal(action.idempotent, true);
  });
});
