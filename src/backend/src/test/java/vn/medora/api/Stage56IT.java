package vn.medora.api;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;
import vn.medora.api.governance.NotificationDispatcher;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class Stage56IT extends IntegrationSupport {
    UUID pharmacist,technician,cashier,finance,manager,registrar,auditor;
    MockHttpSession pharmacySession,technicianSession,cashierSession,financeSession,managerSession,registrarSession,auditorSession;
    @Autowired NotificationDispatcher dispatcher;
    @BeforeEach void extendedRoles() throws Exception {
        pharmacist=account("pharm","PHARMACIST"); technician=account("tech","TECHNICIAN"); cashier=account("cash","CASHIER");
        finance=account("finance","FINANCE"); manager=account("manager","CLINIC_MANAGER"); registrar=account("registrar","AUTHORIZED_REGISTRAR"); auditor=account("auditor","AUDITOR");
        pharmacySession=login("pharm."+suffix); technicianSession=login("tech."+suffix); cashierSession=login("cash."+suffix);
        financeSession=login("finance."+suffix); managerSession=login("manager."+suffix); registrarSession=login("registrar."+suffix); auditorSession=login("auditor."+suffix);
    }
    UUID visit(String name) throws Exception { UUID id=walkIn(patient(name)); start(id); return id; }
    UUID medicine(String code,String ingredient) throws Exception {
        return id(ok(request(adminSession,post(url("/medications")),Map.of("code",code,"name","Synthetic "+code,"ingredientCode",ingredient,"unit","tablet","enabled",true)),200));
    }
    Map<String,Object> rxItem(UUID medication) {
        return Map.of("medicationId",medication,"dose","Synthetic dose","route","Synthetic route","frequency","Synthetic frequency","durationDays",2,"quantity",4,"instructions","Synthetic instructions; no medical advice");
    }
    UUID prescription(UUID encounter,UUID medication) throws Exception {
        return id(ok(request(doctorSession,post(url("/encounters/"+encounter+"/prescriptions")),Map.of("items",List.of(rxItem(medication)))),200));
    }
    void policyAndProfile(UUID encounter,String allergyStatus,List<String> codes) throws Exception {
        ok(request(managerSession,put(url("/medication-policy")),Map.of("source","Synthetic reviewed teaching rules","dataVersion","test-1","enabled",true)),200);
        ok(request(nurseSession,put(url("/encounters/"+encounter+"/medication-profile")),Map.of("allergyStatus",allergyStatus,"allergyCodes",codes,"conditionCodes",List.of(),"currentMedicationIds",List.of())),200);
    }
    Map<String,Object> issueInput(long version,List<String> warnings) {
        return new LinkedHashMap<>(Map.of("version",version,"pharmacistId",pharmacist,"acknowledgedWarnings",warnings));
    }
    void noteAndVitals(UUID encounter) throws Exception {
        String base="/encounters/"+encounter;
        ok(request(doctorSession,post(url(base+"/diagnoses")),Map.of("code","DEMO-001","symptoms","Synthetic symptoms")),201);
        JsonNode note=ok(request(doctorSession,post(url(base+"/notes")),Map.of("subjective","Synthetic symptoms","objective","Synthetic examination","assessment","Teaching record","plan","Synthetic plan")),201);
        ok(request(doctorSession,post(url(base+"/notes/"+id(note)+"/sign")),Map.of("version",0)),200);
        ok(request(nurseSession,post(url(base+"/vitals")),Map.of("pulseBpm",80,"measuredAt",Instant.now().minusSeconds(1).toString(),"verified",true)),201);
    }
    JsonNode complete(UUID encounter) throws Exception {
        JsonNode current=ok(request(doctorSession,get(url("/encounters/"+encounter)),null),200);
        JsonNode awaiting=ok(request(doctorSession,post(url("/encounters/"+encounter+"/submit-for-closure")),Map.of("version",current.get("version").asLong())),200);
        return ok(request(doctorSession,post(url("/encounters/"+encounter+"/complete")),Map.of("version",awaiting.get("version").asLong(),"summary","Synthetic closure","instructions","Synthetic instructions","followUp","No follow-up in this teaching example")),200);
    }
    record InvoiceFixture(UUID encounter,UUID invoice,UUID service) { }
    InvoiceFixture issuedInvoice() throws Exception {
        UUID encounter=visit("Finance "+suffix); var resources=resources();
        ok(request(doctorSession,put(url("/encounters/"+encounter+"/consultation-service")),Map.of("serviceId",resources.service())),200);
        ok(request(managerSession,post(url("/service-prices")),Map.of("serviceId",resources.service(),"amount",100000,"currency","VND","effectiveFrom",Instant.now().minusSeconds(86400).toString())),200);
        noteAndVitals(encounter); complete(encounter);
        JsonNode draft=ok(request(cashierSession,post(url("/encounters/"+encounter+"/invoice")).header("Idempotency-Key","invoice-create-1"),null),200);
        UUID invoice=id(draft);
        ok(request(cashierSession,post(url("/invoices/"+invoice+"/issue")),Map.of("version",0)),200);
        return new InvoiceFixture(encounter,invoice,resources.service());
    }
    Map<String,Object> payment(int amount) { return Map.of("amount",amount,"currency","VND","method","CASH","receivedVerified",true); }

    @Test void medicationPolicyAndKnownAllergyInputsAreRequiredAndIssueDoesNotDispense() throws Exception {
        UUID encounter=visit("Rx "+suffix),medication=medicine("MED1","ING1"),rx=prescription(encounter,medication);
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","issue-no-policy"),issueInput(0,List.of())),409);
        policyAndProfile(encounter,"KNOWN_NONE",List.of());
        JsonNode issued=ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","issue-success-1"),issueInput(0,List.of())),200);
        assertThat(issued.get("status").asText()).isEqualTo("ISSUED");
        assertThat(jdbc.queryForObject("select count(*) from medora.medication_event where prescription_id=?",Integer.class,rx)).isZero();
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","issue-success-1"),issueInput(0,List.of())),200);
        assertThatThrownBy(()->jdbc.update("update medora.prescription_item set dose='tampered' where prescription_id=?",rx)).hasMessageContaining("immutable");
        assertThatThrownBy(()->jdbc.update("update medora.prescription set safety_source='tampered' where id=?",rx)).hasMessageContaining("immutable");
        UUID item=UUID.fromString(issued.get("items").get(0).get("id").asText());
        var event=Map.of("itemId",item,"kind","DISPENSED","quantity",2,"performedAt",Instant.now().toString());
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/medicine-events")).header("Idempotency-Key","dispense-bad-role"),event),403);
        JsonNode first=ok(request(pharmacySession,post(url("/prescriptions/"+rx+"/medicine-events")).header("Idempotency-Key","dispense-first-1"),event),200);
        JsonNode retry=ok(request(pharmacySession,post(url("/prescriptions/"+rx+"/medicine-events")).header("Idempotency-Key","dispense-first-1"),event),200);
        assertThat(id(first)).isEqualTo(id(retry));
        ok(request(pharmacySession,post(url("/prescriptions/"+rx+"/medicine-events")).header("Idempotency-Key","dispense-too-much"),Map.of("itemId",item,"kind","DISPENSED","quantity",3,"performedAt",Instant.now().toString())),409);
        ok(request(adminSession,get(url("/prescriptions/"+rx)),null),403);
    }
    @Test void allergyBlocksAndConfiguredOverridesRequireReasonAndAcknowledgement() throws Exception {
        UUID encounter=visit("Allergy "+suffix),medication=medicine("MED1","ING1"),rx=prescription(encounter,medication);
        policyAndProfile(encounter,"RECORDED",List.of("ING1"));
        var body=issueInput(0,List.of("ALLERGY:ING1")); body.put("overrideReason","Attempt forbidden allergy override");
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","allergy-attempt1"),body),409);
        ok(request(nurseSession,put(url("/encounters/"+encounter+"/medication-profile")),Map.of("version",0,"allergyStatus","KNOWN_NONE","allergyCodes",List.of(),"conditionCodes",List.of("COND1"),"currentMedicationIds",List.of())),200);
        JsonNode rule=ok(request(managerSession,post(url("/medication-rules")),Map.of("medicationId",medication,"kind","CONTRAINDICATION","conditionCode","COND1","severity","BLOCK","overridable",true,"message","Synthetic overridable warning","source","Synthetic rule source","enabled",true)),200);
        String warning=rule.get("id").asText();
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","override-no-reason"),issueInput(0,List.of(warning))),400);
        var valid=issueInput(0,List.of(warning)); valid.put("overrideReason","Synthetic approved override decision");
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","override-valid-1"),valid),200);
        assertThat(jdbc.queryForObject("select status from medora.prescription where id=?",String.class,rx)).isEqualTo("ISSUED");
    }
    @Test void amendmentsKeepOldPrescriptionAndClosureRejectsOutstandingDraft() throws Exception {
        UUID encounter=visit("AmendRx "+suffix),medication=medicine("MED1","ING1"),rx=prescription(encounter,medication);
        policyAndProfile(encounter,"KNOWN_NONE",List.of());
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","rx-original-issue"),issueInput(0,List.of())),200);
        UUID amended=id(ok(request(doctorSession,post(url("/prescriptions/"+rx+"/amendments")),Map.of("version",1,"reason","Synthetic correction")),200));
        noteAndVitals(encounter);
        JsonNode current=ok(request(doctorSession,get(url("/encounters/"+encounter)),null),200);
        ok(request(doctorSession,post(url("/encounters/"+encounter+"/submit-for-closure")),Map.of("version",current.get("version").asLong())),409);
        ok(request(doctorSession,post(url("/prescriptions/"+amended+"/issue")).header("Idempotency-Key","rx-amended-issue"),issueInput(0,List.of())),200);
        assertThat(jdbc.queryForObject("select status from medora.prescription where id=?",String.class,rx)).isEqualTo("SUPERSEDED");
        assertThat(jdbc.queryForObject("select count(*) from medora.prescription_item where prescription_id=?",Integer.class,rx)).isEqualTo(1);
        complete(encounter);
    }
    @Test void procedureAllocationExecutionReviewAndClosureUseSeparateEvidence() throws Exception {
        UUID encounter=visit("Procedure "+suffix); var r=resources();
        ok(request(adminSession,post(url("/provider-services")),Map.of("providerId",technician,"serviceId",r.service())),204);
        ok(request(adminSession,post(url("/staff-schedules")),Map.of("providerId",technician,"kind","WORK","startsAt",Instant.now().minusSeconds(3600).toString(),"endsAt",Instant.now().plusSeconds(28800).toString())),201);
        UUID order=id(ok(request(doctorSession,post(url("/encounters/"+encounter+"/orders")),Map.of("serviceId",r.service(),"reason","Synthetic lab request","priority","ROUTINE","preparation","Synthetic preparation","required",true,"kind","LAB")),200));
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking where procedure_order_id=?",Integer.class,order)).isZero();
        ok(request(doctorSession,put(url("/orders/"+order+"/allocation")),Map.of("version",0,"staffId",technician,"roomId",r.room(),"startsAt",Instant.now().minusSeconds(1).toString(),"reason","Synthetic allocation")),200);
        ok(request(otherDoctorSession,post(url("/orders/"+order+"/actions")),Map.of("action","START","version",1)),403);
        ok(request(technicianSession,post(url("/orders/"+order+"/actions")),Map.of("action","START","version",1)),200);
        ok(request(technicianSession,post(url("/orders/"+order+"/actions")),Map.of("action","COMPLETE","version",2)),400);
        ok(request(technicianSession,post(url("/orders/"+order+"/actions")),Map.of("action","COMPLETE","version",2,"result","Synthetic lab outcome")),200);
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking where procedure_order_id=? and active",Integer.class,order)).isZero();
        assertThatThrownBy(()->jdbc.update("update medora.procedure_order set result='tampered' where id=?",order)).hasMessageContaining("immutable");
        ok(request(doctorSession,post(url("/orders/"+order+"/review")),Map.of("version",3,"reason","Synthetic result reviewed")),200);
        noteAndVitals(encounter); complete(encounter);
    }
    @Test void privateAttachmentsValidateContentAndScopeAndArchiveWithoutDeletingEvidence() throws Exception {
        UUID encounter=visit("Attachment "+suffix); byte[] content="Synthetic private result".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file=new MockMultipartFile("file","result.txt","text/plain",content);
        JsonNode saved=ok(request(doctorSession,multipart(url("/encounters/"+encounter+"/attachments")).file(file),null),200);
        UUID attachment=id(saved); assertThat(saved.has("storageKey")).isFalse();
        var download=mvc.perform(get(url("/attachments/"+attachment+"/download")).session(doctorSession)).andReturn().getResponse();
        assertThat(download.getStatus()).isEqualTo(200); assertThat(download.getContentAsByteArray()).isEqualTo(content);
        assertThat(download.getHeader("Content-Disposition")).contains("attachment;");
        ok(request(adminSession,get(url("/attachments/"+attachment+"/download")),null),403);
        ok(request(doctorSession,multipart(url("/encounters/"+encounter+"/attachments")).file(new MockMultipartFile("file","evil.pdf","application/pdf","<script>bad</script>".getBytes())),null),400);
        ok(request(doctorSession,post(url("/attachments/"+attachment+"/archive")),Map.of("version",0,"reason","Superseded synthetic upload")),200);
        ok(request(doctorSession,get(url("/attachments/"+attachment+"/download")),null),404);
        assertThat(jdbc.queryForObject("select archived from medora.attachment where id=?",Boolean.class,attachment)).isTrue();
    }
    @Test void paymentRetriesAndPartialSettlementAreAppendOnlyAndScoped() throws Exception {
        var fixture=issuedInvoice(); UUID invoice=fixture.invoice();
        JsonNode first=ok(request(cashierSession,post(url("/invoices/"+invoice+"/payments")).header("Idempotency-Key","payment-first-key"),payment(40000)),200);
        JsonNode retry=ok(request(cashierSession,post(url("/invoices/"+invoice+"/payments")).header("Idempotency-Key","payment-first-key"),payment(40000)),200);
        assertThat(id(first)).isEqualTo(id(retry)); assertThat(first.get("invoice").get("balance").asInt()).isEqualTo(60000);
        ok(request(cashierSession,post(url("/invoices/"+invoice+"/payments")).header("Idempotency-Key","payment-first-key"),payment(50000)),409);
        ok(request(cashierSession,post(url("/invoices/"+invoice+"/payments")).header("Idempotency-Key","payment-over-key"),payment(60001)),409);
        ok(request(doctorSession,get(url("/invoices/"+invoice)),null),403);
        ok(request(cashierSession,post(url("/invoices/"+invoice+"/payments")).header("Idempotency-Key","payment-second-key"),payment(60000)),200);
        assertThat(jdbc.queryForObject("select status from medora.invoice where id=?",String.class,invoice)).isEqualTo("PAID");
        assertThatThrownBy(()->jdbc.update("delete from medora.payment where id=?",id(first))).hasMessageContaining("append-only");
        assertThatThrownBy(()->jdbc.update("update medora.invoice set subtotal=1 where id=?",invoice)).hasMessageContaining("immutable");
    }
    @Test void refundsKeepPendingSeparateRequireOtherApproverAndRetryExecutionOnce() throws Exception {
        var fixture=issuedInvoice();
        JsonNode payment=ok(request(cashierSession,post(url("/invoices/"+fixture.invoice()+"/payments")).header("Idempotency-Key","refund-payment-key"),payment(100000)),200);
        var input=Map.of("paymentId",id(payment),"amount",30000,"reason","Synthetic refund");
        ok(request(cashierSession,post(url("/refunds")).header("Idempotency-Key","refund-wrong-role"),input),403);
        JsonNode pending=ok(request(financeSession,post(url("/refunds")).header("Idempotency-Key","refund-request-key"),input),200); UUID refund=id(pending);
        assertThat(pending.get("status").asText()).isEqualTo("PENDING_APPROVAL"); assertThat(pending.has("invoice")).isFalse();
        assertThat(jdbc.queryForObject("select status from medora.invoice where id=?",String.class,fixture.invoice())).isEqualTo("PAID");
        var approval=Map.of("version",0,"action","APPROVE","reason","Independent synthetic approval");
        ok(request(financeSession,post(url("/refunds/"+refund+"/approval")).header("Idempotency-Key","self-approve-key"),approval),403);
        ok(request(managerSession,post(url("/refunds/"+refund+"/approval")).header("Idempotency-Key","refund-approve-key"),approval),200);
        JsonNode completed=ok(request(financeSession,post(url("/refunds/"+refund+"/execute")).header("Idempotency-Key","refund-execute-key"),Map.of("version",1)),200);
        assertThat(completed.get("invoice").get("balance").asInt()).isEqualTo(30000);
        ok(request(financeSession,post(url("/refunds/"+refund+"/execute")).header("Idempotency-Key","refund-execute-key"),Map.of("version",1)),200);
        assertThat(jdbc.queryForObject("select count(*) from medora.refund where payment_id=? and status='COMPLETED'",Integer.class,id(payment))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select amount from medora.payment where id=?",Integer.class,id(payment))).isEqualTo(100000);
        assertThatThrownBy(()->jdbc.update("delete from medora.refund where id=?",refund)).hasMessageContaining("immutable");
    }
    @Test void concurrentPaymentsCannotOverpayAndPriceSnapshotsStayFixed() throws Exception {
        var fixture=issuedInvoice(); MockHttpSession second=login("cash."+suffix);
        JsonNode token1=json.readTree(mvc.perform(get("/api/auth/csrf").session(cashierSession)).andReturn().getResponse().getContentAsString());
        JsonNode token2=json.readTree(mvc.perform(get("/api/auth/csrf").session(second)).andReturn().getResponse().getContentAsString());
        var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results=new ArrayList<>();
            for(int i=0;i<2;i++) { final int index=i; results.add(executor.submit(()->{
                gate.await(); var token=index==0?token1:token2;
                return mvc.perform(post(url("/invoices/"+fixture.invoice()+"/payments")).session(index==0?cashierSession:second)
                    .header(token.get("headerName").asText(),token.get("token").asText()).header("Idempotency-Key","parallel-payment-"+index)
                    .contentType("application/json").content(json.writeValueAsString(payment(70000)))).andReturn().getResponse().getStatus();
            })); }
            gate.countDown(); assertThat(List.of(results.get(0).get(30,TimeUnit.SECONDS),results.get(1).get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("select unit_price from medora.invoice_item where invoice_id=?",Integer.class,fixture.invoice())).isEqualTo(100000);
        assertThatThrownBy(()->jdbc.update("update medora.service_price set amount=200000 where service_id=?",fixture.service())).hasMessageContaining("immutable");
    }
    @Test void reportsReconcilePaymentsRefundsAndExportsAuditWithoutGrantingClinicalAccess() throws Exception {
        var fixture=issuedInvoice(); ok(request(cashierSession,post(url("/invoices/"+fixture.invoice()+"/payments")).header("Idempotency-Key","report-payment-1"),payment(25000)),200);
        String from=Instant.now().minusSeconds(86400).toString(),to=Instant.now().plusSeconds(60).toString();
        JsonNode revenue=ok(request(managerSession,get(url("/reports/revenue")).param("from",from).param("to",to),null),200);
        assertThat(revenue.get("netCash").asInt()).isEqualTo(25000);
        ok(request(doctorSession,get(url("/reports/revenue")).param("from",from).param("to",to),null),403);
        var csv=mvc.perform(get(url("/reports/revenue.csv")).session(managerSession).param("from",from).param("to",to)).andReturn().getResponse();
        assertThat(csv.getStatus()).isEqualTo(200); assertThat(csv.getContentAsString()).contains("netCash","25000");
        JsonNode audits=ok(request(auditorSession,get(url("/audit-events")).param("from",from).param("to",to).param("action","REVENUE_REPORT_EXPORTED"),null),200);
        assertThat(audits.size()).isEqualTo(1);
        ok(request(auditorSession,post(url("/medications")),Map.of("code","NO","name","Forbidden","ingredientCode","NO","unit","tablet","enabled",true)),403);
    }
    @Test void mergeRequiresRegistrarRetainsSourcesAndLinksLongitudinalHistory() throws Exception {
        UUID source=patient("Merge Source "+suffix),target=patient("Merge Target "+suffix);
        UUID old=walkIn(source); start(old); noteAndVitals(old); complete(old);
        JsonNode merge=ok(request(receptionistSession,post(url("/merge-requests")).header("Idempotency-Key","merge-request-key"),Map.of("sourceId",source,"targetId",target,"reason","Synthetic verified duplicate identity")),200);
        var decision=Map.of("version",0,"action","APPROVE","identityVerified",true,"reason","Registrar verified both synthetic identities");
        ok(request(receptionistSession,post(url("/merge-requests/"+id(merge)+"/decision")).header("Idempotency-Key","merge-rec-denied"),decision),403);
        ok(request(registrarSession,post(url("/merge-requests/"+id(merge)+"/decision")).header("Idempotency-Key","merge-registrar-key"),decision),200);
        assertThat(jdbc.queryForObject("select merged_into_id from medora.patient where id=?",UUID.class,source)).isEqualTo(target);
        assertThat(jdbc.queryForObject("select patient_id from medora.encounter where id=?",UUID.class,old)).isEqualTo(source);
        ok(request(receptionistSession,post(url("/check-ins")),Map.of("patientId",source,"doctorId",doctor,"identityVerified",true)),409);
        UUID current=walkIn(target);
        JsonNode timeline=ok(request(doctorSession,get(url("/encounters/"+current+"/record")),null),200);
        assertThat(timeline.size()).isEqualTo(2);
        ok(request(doctorSession,get(url("/encounters/"+current+"/record/"+old)),null),200);
    }
    @Test void inAppOutboxRetriesAndDoesNotAlterAppointmentAndRecipientIsPrivate() throws Exception {
        UUID p=patient("Notify "+suffix); var r=resources();
        UUID appointment=id(ok(request(receptionistSession,post(url("/appointments")).header("Idempotency-Key","notify-booking-key"),booking(p,r.room(),r.service(),r.at())),201));
        JsonNode template=ok(request(adminSession,post(url("/notification-templates")),Map.of("code","APPT","body","Appointment {{appointmentId}} at {{startsAt}}","enabled",true)),200);
        JsonNode notification=ok(request(receptionistSession,post(url("/notifications")).header("Idempotency-Key","notify-request-key"),Map.of("templateId",id(template),"appointmentId",appointment,"recipientId",doctor)),200);
        ok(request(adminSession,put(url("/notification-templates/"+id(template))),Map.of("version",0,"code","APPT","body","Appointment {{appointmentId}}","enabled",false)),200);
        dispatcher.dispatch();
        assertThat(jdbc.queryForObject("select status from medora.notification where id=?",String.class,id(notification))).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select status from medora.appointment where id=?",String.class,appointment)).isEqualTo("BOOKED");
        ok(request(adminSession,put(url("/notification-templates/"+id(template))),Map.of("version",1,"code","APPT","body","Appointment {{appointmentId}}","enabled",true)),200);
        jdbc.update("update medora.notification set next_attempt_at=current_timestamp where id=?",id(notification)); dispatcher.dispatch();
        assertThat(ok(request(doctorSession,get(url("/inbox")),null),200).size()).isEqualTo(1);
        assertThat(ok(request(otherDoctorSession,get(url("/inbox")),null),200).size()).isZero();
    }
    @Test void optionalDischargeApprovalAndExplicitSessionRevocationAreEnforced() throws Exception {
        UUID encounter=visit("Discharge "+suffix); noteAndVitals(encounter);
        JsonNode policy=ok(request(managerSession,get(url("/clinic-policy")),null),200);
        ok(request(managerSession,put(url("/clinic-policy")),Map.of("version",policy.get("version").asLong(),"autoRefundLimit",0,"dischargeApprovalRequired",true)),200);
        JsonNode current=ok(request(doctorSession,get(url("/encounters/"+encounter)),null),200);
        ok(request(doctorSession,post(url("/encounters/"+encounter+"/submit-for-closure")),Map.of("version",current.get("version").asLong())),409);
        UUID request=id(ok(request(doctorSession,post(url("/encounters/"+encounter+"/discharge-requests")),Map.of("reason","Synthetic discharge request")),200));
        ok(request(managerSession,post(url("/discharge-requests/"+request+"/decision")),Map.of("version",0,"action","APPROVE","reason","Synthetic approval")),200);
        complete(encounter);
        ok(request(adminSession,post("/api/admin/facilities/"+facility+"/accounts/"+doctor+"/revoke-sessions"),Map.of("version",0)),200);
        assertThat(mvc.perform(get("/api/auth/me").session(doctorSession)).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
    @Test void pendingRefundReservationsPreventConcurrentOverRefundAndRejectionReleasesThem() throws Exception {
        var fixture=issuedInvoice(); UUID payment=id(ok(request(cashierSession,post(url("/invoices/"+fixture.invoice()+"/payments")).header("Idempotency-Key","reserve-pay-key"),payment(100000)),200));
        MockHttpSession second=login("finance."+suffix);
        JsonNode token1=json.readTree(mvc.perform(get("/api/auth/csrf").session(financeSession)).andReturn().getResponse().getContentAsString());
        JsonNode token2=json.readTree(mvc.perform(get("/api/auth/csrf").session(second)).andReturn().getResponse().getContentAsString());
        var input=Map.of("paymentId",payment,"amount",70000,"reason","Synthetic concurrent refund"); var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results=new ArrayList<>();
            for(int i=0;i<2;i++) { final int index=i; results.add(executor.submit(()->{
                gate.await();var token=index==0?token1:token2;
                return mvc.perform(post(url("/refunds")).session(index==0?financeSession:second).header(token.get("headerName").asText(),token.get("token").asText())
                    .header("Idempotency-Key","parallel-refund-"+index).contentType("application/json").content(json.writeValueAsString(input))).andReturn().getResponse().getStatus();
            })); }
            gate.countDown();assertThat(List.of(results.get(0).get(30,TimeUnit.SECONDS),results.get(1).get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        UUID pending=jdbc.queryForObject("select id from medora.refund where payment_id=?",UUID.class,payment);
        ok(request(managerSession,post(url("/refunds/"+pending+"/approval")).header("Idempotency-Key","reject-reserved-refund"),Map.of("version",0,"action","REJECT","reason","Synthetic rejection")),200);
        ok(request(financeSession,post(url("/refunds")).header("Idempotency-Key","request-after-reject"),Map.of("paymentId",payment,"amount",100000,"reason","Reservation released")),200);
        assertThat(jdbc.queryForObject("select status from medora.invoice where id=?",String.class,fixture.invoice())).isEqualTo("PAID");
    }
    @Test void serviceResourceConflictsLeavePendingOrdersUnallocatedAndCancellationReleasesSlot() throws Exception {
        UUID encounter=visit("Service conflict "+suffix); var r=resources();
        ok(request(adminSession,post(url("/provider-services")),Map.of("providerId",technician,"serviceId",r.service())),204);
        ok(request(adminSession,post(url("/staff-schedules")),Map.of("providerId",technician,"kind","WORK","startsAt",Instant.now().minusSeconds(3600).toString(),"endsAt",Instant.now().plusSeconds(28800).toString())),201);
        var orderInput=Map.of("serviceId",r.service(),"reason","Synthetic service","priority","ROUTINE","preparation","","required",true,"kind","PROCEDURE");
        UUID first=id(ok(request(doctorSession,post(url("/encounters/"+encounter+"/orders")),orderInput),200));
        UUID second=id(ok(request(doctorSession,post(url("/encounters/"+encounter+"/orders")),orderInput),200));
        var slot=Map.of("version",0,"staffId",technician,"roomId",r.room(),"startsAt",Instant.now().minusSeconds(1).toString(),"reason","Synthetic booking");
        ok(request(doctorSession,put(url("/orders/"+first+"/allocation")),slot),200);
        ok(request(doctorSession,put(url("/orders/"+second+"/allocation")),slot),409);
        assertThat(jdbc.queryForObject("select status from medora.procedure_order where id=?",String.class,second)).isEqualTo("PENDING_ALLOCATION");
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking where procedure_order_id=?",Integer.class,second)).isZero();
        ok(request(doctorSession,post(url("/orders/"+first+"/actions")),Map.of("version",1,"action","CANCEL","reason","No longer clinically requested in test")),200);
        ok(request(doctorSession,put(url("/orders/"+second+"/allocation")),slot),200);
    }
    @Test void futurePriceChangesPreserveHistoricalInvoicesAndAutoRefundUsesExplicitPolicy() throws Exception {
        var fixture=issuedInvoice();
        UUID price=jdbc.queryForObject("select id from medora.service_price where service_id=?",UUID.class,fixture.service());
        Instant change=Instant.now().plusSeconds(3600);
        ok(request(managerSession,post(url("/service-prices/"+price+"/end")),Map.of("effectiveTo",change.toString())),200);
        ok(request(managerSession,post(url("/service-prices")),Map.of("serviceId",fixture.service(),"amount",200000,"currency","VND","effectiveFrom",change.toString())),200);
        assertThat(jdbc.queryForObject("select unit_price from medora.invoice_item where invoice_id=?",Integer.class,fixture.invoice())).isEqualTo(100000);
        JsonNode policy=ok(request(managerSession,get(url("/clinic-policy")),null),200);
        ok(request(managerSession,put(url("/clinic-policy")),Map.of("version",policy.get("version").asLong(),"autoRefundLimit",10000,"dischargeApprovalRequired",false)),200);
        UUID payment=id(ok(request(cashierSession,post(url("/invoices/"+fixture.invoice()+"/payments")).header("Idempotency-Key","auto-pay-key-1"),payment(100000)),200));
        JsonNode refund=ok(request(financeSession,post(url("/refunds")).header("Idempotency-Key","auto-refund-key-1"),Map.of("paymentId",payment,"amount",5000,"reason","Configured auto limit test")),200);
        assertThat(refund.get("status").asText()).isEqualTo("COMPLETED"); assertThat(refund.get("invoice").get("balance").asInt()).isEqualTo(5000);
    }

    @Test void schedulingCatalogUpdatesAndListsPreserveVersionsAndSavedFields() throws Exception {
        var r=resources();
        var room=ok(request(adminSession,put(url("/rooms/"+r.room())),Map.of("code","R1","name","Updated room","roomType","CONSULTATION","equipment","ECG","enabled",true,"version",0)),200);
        assertThat(room.get("name").asText()).isEqualTo("Updated room");
        ok(request(adminSession,put(url("/rooms/"+r.room())),Map.of("code","R1","name","Stale","roomType","CONSULTATION","equipment","","enabled",true,"version",0)),409);
        var service=ok(request(adminSession,put(url("/services/"+r.service())),Map.of("code","S1","name","Updated service","durationMinutes",20,"bufferMinutes",10,"enabled",true,"version",0)),200);
        ok(request(managerSession,put(url("/services/"+r.service()+"/requirements")),Map.of("requiredRoomType","CONSULTATION","requiredEquipment","ECG","version",service.get("version").asLong())),200);
        assertThat(ok(request(doctorSession,get(url("/rooms")),null),200).get(0).get("equipment").asText()).isEqualTo("ECG");
        assertThat(ok(request(doctorSession,get(url("/services")),null),200).get(0).get("requiredRoomType").asText()).isEqualTo("CONSULTATION");
        assertThat(ok(request(adminSession,get(url("/staff-schedules")).param("from",Instant.now().minusSeconds(60).toString()).param("to",Instant.now().plusSeconds(86400).toString()),null),200).isArray()).isTrue();
        UUID p=patient("Booking list "+suffix);
        UUID appt=id(ok(request(receptionistSession,post(url("/appointments")).header("Idempotency-Key","list-book-key"),booking(p,r.room(),r.service(),r.at())),201));
        assertThat(ok(request(receptionistSession,get(url("/appointments")).param("from",r.at().minusSeconds(3600).toString()).param("to",r.at().plusSeconds(3600).toString()),null),200).toString()).contains(appt.toString());
        assertThat(ok(request(receptionistSession,get(url("/patients")).param("q","Booking list"),null),200).get("items").toString()).contains(p.toString());
        assertThat(ok(request(receptionistSession,get(url("/patients/"+p)),null),200).get("fullName").asText()).isEqualTo("Booking list "+suffix);
    }

    @Test void assignedClinicalReadViewsAndReportsRejectUnassignedReaders() throws Exception {
        UUID encounter=visit("Read views "+suffix);noteAndVitals(encounter);
        ok(request(nurseSession,post(url("/encounters/"+encounter+"/history")),Map.of("kind","HISTORY","content","Synthetic history read test")),201);
        for(String part:List.of("history","vitals","diagnoses","notes","orders","prescriptions","attachments")) {
            assertThat(ok(request(doctorSession,get(url("/encounters/"+encounter+"/"+part)),null),200).isArray()).as(part).isTrue();
            ok(request(otherDoctorSession,get(url("/encounters/"+encounter+"/"+part)),null),403);
        }
        assertThat(ok(request(doctorSession,get(url("/diagnosis-catalog")),null),200).isArray()).isTrue();
        var from=Instant.now().minusSeconds(3600).toString();var to=Instant.now().plusSeconds(3600).toString();
        assertThat(ok(request(doctorSession,get(url("/dashboard")).param("from",from).param("to",to),null),200).get("encounters").asInt()).isEqualTo(1);
        assertThat(ok(request(doctorSession,get(url("/reports/operations")).param("from",from).param("to",to),null),200).get("scope").asText()).isEqualTo("ASSIGNED_ENCOUNTERS");
        UUID p=UUID.fromString(ok(request(doctorSession,get(url("/encounters/"+encounter)),null),200).get("patientId").asText());
        assertThat(ok(request(auditorSession,get(url("/patients/"+p+"/audit-events")),null),200).isArray()).isTrue();
        assertThat(ok(request(receptionistSession,get(url("/merge-requests")),null),200).isArray()).isTrue();
        ok(request(auditorSession,get(url("/encounters/"+encounter+"/notes")),null),403);
    }

    @Test void catalogEditsAndWarningsRequireCurrentVersionAndExplicitAcknowledgement() throws Exception {
        UUID encounter=visit("Warnings "+suffix),one=medicine("WARN1","ING-W1"),two=medicine("WARN2","ING-W2");
        var medBody=Map.of("code","WARN1","name","Updated synthetic medicine","ingredientCode","ING-W1","unit","tablet","enabled",true,"version",0);
        assertThat(ok(request(managerSession,put(url("/medications/"+one)),medBody),200).get("version").asLong()).isEqualTo(1);
        ok(request(managerSession,put(url("/medications/"+one)),medBody),409);
        policyAndProfile(encounter,"KNOWN_NONE",List.of());
        ok(request(nurseSession,put(url("/encounters/"+encounter+"/medication-profile")),Map.of("allergyStatus","KNOWN_NONE","allergyCodes",List.of(),"conditionCodes",List.of(),"currentMedicationIds",List.of(two),"version",0)),200);
        var body=new LinkedHashMap<String,Object>(Map.of("medicationId",one,"otherMedicationId",two,"kind","INTERACTION","severity","WARN","overridable",false,"message","Synthetic warning","source","Synthetic source","enabled",true));
        var rule=ok(request(managerSession,post(url("/medication-rules")),body),200);body.put("version",0);body.put("message","Updated warning");
        ok(request(managerSession,put(url("/medication-rules/"+id(rule))),body),200);
        ok(request(managerSession,put(url("/medication-rules/"+id(rule))),body),409);
        assertThat(ok(request(doctorSession,get(url("/medications")).param("q","WARN1"),null),200).get(0).get("id").asText()).isEqualTo(one.toString());
        assertThat(ok(request(managerSession,get(url("/medication-rules")),null),200).size()).isEqualTo(1);
        assertThat(ok(request(managerSession,get(url("/medication-policy")),null),200).get("enabled").asBoolean()).isTrue();
        assertThat(ok(request(nurseSession,get(url("/encounters/"+encounter+"/medication-profile")),null),200).get("version").asInt()).isEqualTo(1);
        UUID rx=prescription(encounter,one);var safety=ok(request(doctorSession,get(url("/prescriptions/"+rx+"/safety")),null),200);
        assertThat(safety.get("warnings").size()).isEqualTo(1);
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","warn-no-ack-key"),issueInput(0,List.of())),409);
        ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","warn-ack-key"),issueInput(0,List.of(safety.get("warnings").get(0).get("id").asText()))),200);
        assertThat(ok(request(pharmacySession,get(url("/pharmacy-queue")),null),200).toString()).contains(rx.toString());
        assertThat(ok(request(pharmacySession,get(url("/prescriptions/"+rx)),null),200).get("items").size()).isEqualTo(1);
        assertThat(ok(request(doctorSession,get(url("/prescriptions/"+rx+"/export")),null),200).get("id").asText()).isEqualTo(rx.toString());
    }

    @Test void prescriptionDraftEditAndCancellationRetainItemsAndPreventDispensing() throws Exception {
        UUID encounter=visit("Rx lifecycle "+suffix),med=medicine("LIFE1","ING-L1"),rx=prescription(encounter,med);
        policyAndProfile(encounter,"KNOWN_NONE",List.of());
        var changed=ok(request(doctorSession,put(url("/prescriptions/"+rx)),Map.of("version",0,"items",List.of(rxItem(med)))),200);
        ok(request(doctorSession,put(url("/prescriptions/"+rx)),Map.of("version",0,"items",List.of(rxItem(med)))),409);
        var issued=ok(request(doctorSession,post(url("/prescriptions/"+rx+"/issue")).header("Idempotency-Key","cancel-issue-key"),issueInput(changed.get("version").asLong(),List.of())),200);
        var canceled=ok(request(doctorSession,post(url("/prescriptions/"+rx+"/cancel")),Map.of("version",issued.get("version").asLong(),"reason","Synthetic cancellation")),200);
        assertThat(canceled.get("status").asText()).isEqualTo("CANCELED");assertThat(canceled.get("items").size()).isEqualTo(1);
        ok(request(pharmacySession,post(url("/prescriptions/"+rx+"/medicine-events")).header("Idempotency-Key","cancel-no-dispense"),Map.of("itemId",issued.get("items").get(0).get("id").asText(),"kind","DISPENSED","quantity",1,"performedAt",issued.get("issuedAt").asText())),409);
    }

    @Test void procedureQueueAndDetailRequireTheCorrectStaffAssignment() throws Exception {
        UUID encounter=visit("Procedure reads "+suffix);var r=resources();
        ok(request(adminSession,post(url("/provider-services")),Map.of("providerId",technician,"serviceId",r.service())),204);
        ok(request(adminSession,post(url("/staff-schedules")),Map.of("providerId",technician,"kind","WORK","startsAt",Instant.now().minusSeconds(3600).toString(),"endsAt",Instant.now().plusSeconds(28800).toString())),201);
        UUID order=id(ok(request(doctorSession,post(url("/encounters/"+encounter+"/orders")),Map.of("serviceId",r.service(),"reason","Synthetic lab","priority","ROUTINE","preparation","","required",true,"kind","LAB")),200));
        assertThat(ok(request(technicianSession,get(url("/procedure-queue")),null),200).size()).isZero();
        ok(request(doctorSession,put(url("/orders/"+order+"/allocation")),Map.of("staffId",technician,"roomId",r.room(),"startsAt",Instant.now().minusSeconds(1).toString(),"version",0,"reason","Synthetic scheduling")),200);
        assertThat(ok(request(technicianSession,get(url("/procedure-queue")),null),200).get(0).get("id").asText()).isEqualTo(order.toString());
        assertThat(ok(request(technicianSession,get(url("/orders/"+order)),null),200).get("staffId").asText()).isEqualTo(technician.toString());
        ok(request(otherDoctorSession,get(url("/orders/"+order)),null),403);
    }

    @Test void discountedInvoiceReadReceiptRefundAndCsvMatchTheLedger() throws Exception {
        UUID encounter=visit("Finance reads "+suffix);var r=resources();
        ok(request(doctorSession,put(url("/encounters/"+encounter+"/consultation-service")),Map.of("serviceId",r.service())),200);
        ok(request(managerSession,post(url("/service-prices")),Map.of("serviceId",r.service(),"amount",100000,"currency","VND","effectiveFrom",Instant.now().minusSeconds(86400).toString())),200);
        assertThat(ok(request(managerSession,get(url("/service-prices")).param("serviceId",r.service().toString()),null),200).get(0).get("amount").asInt()).isEqualTo(100000);
        noteAndVitals(encounter);complete(encounter);
        UUID invoice=id(ok(request(cashierSession,post(url("/encounters/"+encounter+"/invoice")).header("Idempotency-Key","read-invoice-key"),null),200));
        var discounted=ok(request(financeSession,post(url("/invoices/"+invoice+"/discount")),Map.of("amount",10000,"reason","Synthetic discount","version",0)),200);
        assertThat(discounted.get("total").asInt()).isEqualTo(90000);
        ok(request(cashierSession,post(url("/invoices/"+invoice+"/issue")),Map.of("version",discounted.get("version").asLong())),200);
        UUID pay=id(ok(request(cashierSession,post(url("/invoices/"+invoice+"/payments")).header("Idempotency-Key","read-payment-key"),payment(90000)),200));
        assertThat(ok(request(cashierSession,get(url("/invoices")),null),200).get(0).get("id").asText()).isEqualTo(invoice.toString());
        var details=ok(request(cashierSession,get(url("/invoices/"+invoice)),null),200);
        assertThat(details.get("balance").asInt()).isZero();assertThat(details.get("payments").size()).isEqualTo(1);
        assertThat(ok(request(cashierSession,get(url("/payments/"+pay+"/receipt")),null),200).get("amount").asInt()).isEqualTo(90000);
        var refund=ok(request(financeSession,post(url("/refunds")).header("Idempotency-Key","read-refund-key"),Map.of("paymentId",pay,"amount",10000,"reason","Synthetic refund")),200);
        var pending=ok(request(financeSession,get(url("/refunds/"+id(refund))),null),200);
        assertThat(pending.get("status").asText()).isEqualTo("PENDING_APPROVAL");assertThat(pending.has("invoice")).isFalse();
        var csv=mvc.perform(get(url("/reports/invoices.csv")).session(managerSession).param("from",Instant.now().minusSeconds(3600).toString()).param("to",Instant.now().plusSeconds(3600).toString())).andReturn().getResponse();
        assertThat(csv.getStatus()).isEqualTo(200);assertThat(csv.getContentType()).contains("text/csv");assertThat(csv.getContentAsString()).contains(invoice.toString());
        ok(request(doctorSession,get(url("/payments/"+pay+"/receipt")),null),403);
    }

    @Test void deliveredInboxReadIsPrivateAndDoesNotChangeItsFirstReadTime() throws Exception {
        UUID p=patient("Inbox "+suffix);var r=resources();
        UUID appt=id(ok(request(receptionistSession,post(url("/appointments")).header("Idempotency-Key","inbox-book-key"),booking(p,r.room(),r.service(),r.at())),201));
        var template=ok(request(managerSession,post(url("/notification-templates")),Map.of("code","INBOX_TEST","body","Synthetic {{appointmentId}}","enabled",true)),200);
        assertThat(ok(request(receptionistSession,get(url("/notification-templates")),null),200).size()).isEqualTo(1);
        UUID notification=id(ok(request(receptionistSession,post(url("/notifications")).header("Idempotency-Key","inbox-send-key"),Map.of("templateId",id(template),"appointmentId",appt,"recipientId",doctor)),200));
        dispatcher.dispatch();
        assertThat(ok(request(receptionistSession,get(url("/notifications")),null),200).get(0).get("status").asText()).isEqualTo("DELIVERED");
        var read=ok(request(doctorSession,post(url("/inbox/"+notification+"/read")),null),200);
        assertThat(read.get("readAt").isNull()).isFalse();
        assertThat(ok(request(doctorSession,post(url("/inbox/"+notification+"/read")),null),200).get("readAt")).isEqualTo(read.get("readAt"));
        ok(request(otherDoctorSession,post(url("/inbox/"+notification+"/read")),null),404);
    }

    @Test void abandonedAmendmentAndReturnedClosurePreserveOriginalSignedText() throws Exception {
        UUID encounter=visit("Return closure "+suffix);noteAndVitals(encounter);
        var signed=ok(request(doctorSession,get(url("/encounters/"+encounter+"/notes")),null),200).get(0);
        var amendment=ok(request(doctorSession,post(url("/encounters/"+encounter+"/notes/"+id(signed)+"/amendments")),Map.of("version",signed.get("version").asLong(),"reason","Synthetic draft")),201);
        ok(request(doctorSession,post(url("/encounters/"+encounter+"/notes/"+id(amendment)+"/abandon")),Map.of("version",0)),204);
        var current=ok(request(doctorSession,get(url("/encounters/"+encounter)),null),200);
        var assigned=ok(request(receptionistSession,put(url("/encounters/"+encounter+"/nurse")),Map.of("nurseId",nurse,"version",current.get("version").asLong())),200);
        var awaiting=ok(request(doctorSession,post(url("/encounters/"+encounter+"/submit-for-closure")),Map.of("version",assigned.get("version").asLong())),200);
        assertThat(ok(request(doctorSession,post(url("/encounters/"+encounter+"/return-for-correction")),Map.of("version",awaiting.get("version").asLong())),200).get("status").asText()).isEqualTo("IN_CONSULTATION");
        assertThat(complete(encounter).get("status").asText()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select subjective from medora.clinical_note where id=?",String.class,id(signed))).isEqualTo(signed.get("subjective").asText());
        ok(request(managerSession,put(url("/diagnosis-catalog")),Map.of("code","UAT.01","name","Synthetic custom diagnosis","enabled",true)),200);
    }

}
