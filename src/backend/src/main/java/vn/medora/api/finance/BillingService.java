package vn.medora.api.finance;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import java.math.BigDecimal;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ClinicAccess.*;
import static vn.medora.api.scheduling.SchedulingService.expected;
import static vn.medora.api.finance.FinanceDtos.*;

@Service @Transactional
public class BillingService {
    private final Store db; private final ClinicAccess access; private final AuditService audit; private final RequestDeduplication dedup;
    public BillingService(Store db,ClinicAccess access,AuditService audit,RequestDeduplication dedup) {
        this.db=db; this.access=access; this.audit=audit; this.dedup=dedup;
    }
    private BigDecimal money(Map<String,Object> row,String field) { return (BigDecimal)row.get(field); }
    private Map<String,Object> invoice(UUID facility,UUID id) {
        return db.one("select * from medora.invoice where id=? and facility_id=? for update",id,facility);
    }
    private BigDecimal paid(UUID id) {
        return money(db.one("""
            select coalesce((select sum(amount) from medora.payment where invoice_id=?),0)
            -coalesce((select sum(r.amount) from medora.refund r join medora.payment p on p.id=r.payment_id where p.invoice_id=? and r.status='COMPLETED'),0) as net
            """,id,id),"net");
    }
    private Map<String,Object> balance(Map<String,Object> row) {
        var result=new LinkedHashMap<>(row); BigDecimal total=money(row,"subtotal").subtract(money(row,"discount"));
        BigDecimal net=paid(uuid(row,"id")); result.put("total",total); result.put("netPaid",net); result.put("balance",total.subtract(net)); return result;
    }
    private void refresh(UUID id) {
        var row=db.one("select * from medora.invoice where id=?",id); BigDecimal net=paid(id),total=money(row,"subtotal").subtract(money(row,"discount"));
        String status=net.signum()==0?"ISSUED":net.compareTo(total)==0?"PAID":"PARTIALLY_PAID";
        db.update("update medora.invoice set status=?,version=version+1 where id=?",status,id);
    }
    public List<Map<String,Object>> prices(UUID facility,UUID service) {
        access.require(facility,"price.manage");
        return db.list("select * from medora.service_price where facility_id=? and service_id=? order by effective_from desc limit 100",facility,service);
    }
    public Map<String,Object> price(UUID facility,PriceInput r) {
        UUID actor=access.require(facility,"price.manage"); access.lock(facility);
        db.one("select id from medora.clinic_service where id=? and facility_id=?",r.serviceId(),facility);
        if(r.effectiveTo()!=null&&!r.effectiveTo().isAfter(r.effectiveFrom())) { throw bad("INVALID_PRICE_PERIOD"); }
        UUID id=UUID.randomUUID(); db.update("insert into medora.service_price(id,facility_id,service_id,amount,currency,effective_from,effective_to,created_by) values(?,?,?,?,?,?,?,?)",
            id,facility,r.serviceId(),r.amount(),r.currency(),r.effectiveFrom(),r.effectiveTo(),actor);
        audit.record(actor,facility,"SERVICE_PRICE_ADDED","SERVICE_PRICE",id);
        return db.one("select * from medora.service_price where id=?",id);
    }
    public Map<String,Object> endPrice(UUID facility,UUID id,PriceEnd r) {
        UUID actor=access.require(facility,"price.manage"); access.lock(facility);
        var row=db.one("select * from medora.service_price where id=? and facility_id=? for update",id,facility);
        if(row.get("effectiveTo")!=null||!r.effectiveTo().isAfter(java.time.Instant.now())
            ||!r.effectiveTo().isAfter(instant(row,"effectiveFrom"))) { throw bad("PRICE_CAN_ONLY_END_IN_FUTURE"); }
        db.update("update medora.service_price set effective_to=? where id=?",r.effectiveTo(),id);
        audit.record(actor,facility,"SERVICE_PRICE_PERIOD_ENDED","SERVICE_PRICE",id);
        return db.one("select * from medora.service_price where id=?",id);
    }
    public Map<String,Object> create(UUID facility,UUID encounter,String key) {
        UUID actor=access.require(facility,"invoice.write"); access.lock(facility);
        var fingerprint=Map.of("encounterId",encounter); var old=dedup.previous(facility,actor,"INVOICE_CREATE",key,fingerprint);
        if(old.isPresent()) { return old.get(); }
        var visit=db.one("select * from medora.encounter where id=? and facility_id=? for update",encounter,facility); state(visit,"COMPLETED");
        if(db.exists("select exists(select 1 from medora.invoice where encounter_id=?)",encounter)) { throw conflict("INVOICE_ALREADY_EXISTS"); }
        List<Map<String,Object>> sources=new ArrayList<>(); UUID service=uuid(visit,"consultationServiceId");
        if(visit.get("appointmentId")!=null) { service=uuid(db.one("select service_id from medora.appointment where id=?",visit.get("appointmentId")),"serviceId"); }
        if(service!=null) { sources.add(Map.of("sourceType","CONSULTATION","sourceId",encounter,"serviceId",service,"at",visit.get("createdAt"))); }
        for(var order:db.list("select * from medora.procedure_order where encounter_id=? and status='COMPLETED'",encounter)) {
            sources.add(Map.of("sourceType","PROCEDURE","sourceId",order.get("id"),"serviceId",order.get("serviceId"),"at",order.get("completedAt")));
        }
        if(sources.isEmpty()) { throw conflict("NO_ELIGIBLE_BILLABLE_ITEMS"); }
        List<Map<String,Object>> lines=new ArrayList<>(); BigDecimal subtotal=BigDecimal.ZERO;
        for(var source:sources) {
            var price=db.optional("""
                select p.*,s.name from medora.service_price p join medora.clinic_service s on s.id=p.service_id
                where p.facility_id=? and p.service_id=? and p.effective_from<=? and (p.effective_to is null or p.effective_to>?)
                """,facility,source.get("serviceId"),source.get("at"),source.get("at")).orElseThrow(()->conflict("EFFECTIVE_PRICE_NOT_CONFIGURED"));
            var line=new LinkedHashMap<>(source); line.put("priceId",price.get("id")); line.put("amount",price.get("amount")); line.put("description",price.get("name"));
            lines.add(line); subtotal=subtotal.add(money(price,"amount"));
        }
        UUID id=UUID.randomUUID(); db.update("insert into medora.invoice(id,facility_id,encounter_id,subtotal,created_by) values(?,?,?,?,?)",id,facility,encounter,subtotal,actor);
        for(var line:lines) {
            db.update("insert into medora.invoice_item(id,invoice_id,service_id,price_id,source_type,source_id,description,unit_price) values(?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),id,line.get("serviceId"),line.get("priceId"),line.get("sourceType"),line.get("sourceId"),line.get("description"),line.get("amount"));
        }
        audit.record(actor,facility,"INVOICE_CREATED","INVOICE",id);
        return dedup.save(facility,actor,"INVOICE_CREATE",key,fingerprint,details(facility,id));
    }
    private Map<String,Object> details(UUID facility,UUID id) {
        var result=balance(invoice(facility,id)); result.put("items",db.list("select * from medora.invoice_item where invoice_id=? order by id",id));
        result.put("payments",db.list("select * from medora.payment where invoice_id=? order by recorded_at,id",id));
        result.put("refunds",db.list("select r.* from medora.refund r join medora.payment p on p.id=r.payment_id where p.invoice_id=? order by r.requested_at,r.id",id)); return result;
    }
    public Map<String,Object> get(UUID facility,UUID id) {
        UUID actor=access.require(facility,"invoice.read"); var result=details(facility,id);
        audit.record(actor,facility,"INVOICE_READ","INVOICE",id); return result;
    }
    public List<Map<String,Object>> list(UUID facility,int page,int size) {
        UUID actor=access.require(facility,"invoice.read"); page(page,size,"id");
        audit.record(actor,facility,"INVOICE_LIST_READ","FACILITY",facility);
        return db.list("select * from medora.invoice where facility_id=? order by created_at desc,id limit ? offset ?",facility,size,(long)page*size).stream().map(this::balance).toList();
    }
    public Map<String,Object> discount(UUID facility,UUID id,DiscountInput r) {
        UUID actor=access.require(facility,"invoice.discount"); access.lock(facility); var row=invoice(facility,id); state(row,"DRAFT"); expected(row,r.version());
        if(r.amount().compareTo(money(row,"subtotal"))>=0) { throw bad("DISCOUNT_EXCEEDS_TOTAL"); }
        db.update("update medora.invoice set discount=?,discount_reason=?,version=version+1 where id=?",r.amount(),r.reason(),id);
        audit.record(actor,facility,"INVOICE_DISCOUNT_APPLIED","INVOICE",id,null,r.reason()); return balance(invoice(facility,id));
    }
    public Map<String,Object> issue(UUID facility,UUID id,Long version) {
        UUID actor=access.require(facility,"invoice.write"); access.lock(facility); var row=invoice(facility,id); state(row,"DRAFT"); expected(row,version);
        db.update("update medora.invoice set status='ISSUED',issued_by=?,issued_at=current_timestamp,version=version+1 where id=?",actor,id);
        audit.record(actor,facility,"INVOICE_ISSUED","INVOICE",id); return balance(invoice(facility,id));
    }
    public Map<String,Object> payment(UUID facility,UUID id,String key,PaymentInput r) {
        UUID actor=access.require(facility,"payment.write"); access.lock(facility);
        var fingerprint=Map.of("invoiceId",id,"input",r); var old=dedup.previous(facility,actor,"PAYMENT",key,fingerprint); if(old.isPresent()) { return old.get(); }
        var row=invoice(facility,id); state(row,"ISSUED","PARTIALLY_PAID");
        if(!Boolean.TRUE.equals(r.receivedVerified())) { throw bad("PAYMENT_RECEIPT_REQUIRES_VERIFICATION"); }
        BigDecimal balance=money(row,"subtotal").subtract(money(row,"discount")).subtract(paid(id));
        if(r.amount().compareTo(balance)>0) { throw conflict("PAYMENT_EXCEEDS_BALANCE"); }
        UUID payment=UUID.randomUUID(); db.update("insert into medora.payment(id,facility_id,invoice_id,amount,currency,method,receipt_number,recorded_by) values(?,?,?,?,?,?,?,?)",
            payment,facility,id,r.amount(),r.currency(),r.method(),"RCPT-"+payment,actor);
        refresh(id); audit.record(actor,facility,"PAYMENT_RECORDED","PAYMENT",payment,id,null);
        var result=new LinkedHashMap<>(db.one("select * from medora.payment where id=?",payment)); result.put("invoice",balance(invoice(facility,id)));
        return dedup.save(facility,actor,"PAYMENT",key,fingerprint,result);
    }
    public Map<String,Object> receipt(UUID facility,UUID id) {
        UUID actor=access.require(facility,"invoice.read"); var payment=db.one("select * from medora.payment where id=? and facility_id=?",id,facility);
        audit.record(actor,facility,"RECEIPT_EXPORTED","PAYMENT",id); return payment;
    }
    private Map<String,Object> paymentForRefund(UUID facility,UUID payment) {
        return db.one("select * from medora.payment where id=? and facility_id=?",payment,facility);
    }
    private BigDecimal reservedRefunds(UUID payment) {
        return money(db.one("select coalesce(sum(amount),0) as amount from medora.refund where payment_id=? and status in ('PENDING_APPROVAL','APPROVED','COMPLETED')",payment),"amount");
    }
    public Map<String,Object> requestRefund(UUID facility,String key,RefundInput r) {
        UUID actor=access.require(facility,"refund.request"); access.lock(facility);
        var old=dedup.previous(facility,actor,"REFUND_REQUEST",key,r); if(old.isPresent()) { return old.get(); }
        var payment=paymentForRefund(facility,r.paymentId()); var invoice=invoice(facility,uuid(payment,"invoiceId"));
        if(r.amount().compareTo(money(payment,"amount").subtract(reservedRefunds(r.paymentId())))>0) { throw conflict("REFUND_EXCEEDS_AVAILABLE_PAYMENT"); }
        var policy=db.optional("select * from medora.clinic_policy where facility_id=?",facility);
        BigDecimal limit=policy.map(p->money(p,"autoRefundLimit")).orElse(BigDecimal.ZERO);
        boolean auto=r.amount().compareTo(limit)<=0; UUID id=UUID.randomUUID();
        db.update("insert into medora.refund(id,facility_id,payment_id,amount,reason,status,requested_by) values(?,?,?,?,?,?,?)",
            id,facility,r.paymentId(),r.amount(),r.reason(),auto?"APPROVED":"PENDING_APPROVAL",actor);
        refundEvent(facility,id,actor,"REQUESTED",r.reason());
        if(auto) { finish(facility,id,uuid(invoice,"id"),actor); }
        var result=refundResult(facility,id,auto); return dedup.save(facility,actor,"REFUND_REQUEST",key,r,result);
    }
    private void finish(UUID facility,UUID id,UUID invoice,UUID actor) {
        db.update("update medora.refund set status='COMPLETED',completed_at=current_timestamp,completed_by=?,version=version+1 where id=?",actor,id);
        refresh(invoice); refundEvent(facility,id,actor,"COMPLETED",null);
    }
    private Map<String,Object> refundResult(UUID facility,UUID id,boolean completed) {
        var row=db.one("select * from medora.refund where id=? and facility_id=?",id,facility);
        var result=new LinkedHashMap<>(row);
        if(completed) { var payment=paymentForRefund(facility,uuid(row,"paymentId")); result.put("invoice",balance(invoice(facility,uuid(payment,"invoiceId")))); }
        return result;
    }
    public Map<String,Object> approve(UUID facility,UUID id,String key,ApprovalInput r) {
        UUID actor=access.require(facility,"refund.approve"); access.lock(facility);
        var fingerprint=Map.of("refundId",id,"input",r); var old=dedup.previous(facility,actor,"REFUND_APPROVE",key,fingerprint); if(old.isPresent()) { return old.get(); }
        var row=db.one("select * from medora.refund where id=? and facility_id=? for update",id,facility); state(row,"PENDING_APPROVAL"); expected(row,r.version());
        if(actor.equals(row.get("requestedBy"))) { throw new AccessDeniedException("SELF_APPROVAL_NOT_ALLOWED"); }
        db.update("update medora.refund set status=?,approved_by=?,approved_at=current_timestamp,version=version+1 where id=?",
            r.action().equals("APPROVE")?"APPROVED":"REJECTED",actor,id);
        refundEvent(facility,id,actor,r.action(),r.reason()); return dedup.save(facility,actor,"REFUND_APPROVE",key,fingerprint,refundResult(facility,id,false));
    }
    public Map<String,Object> execute(UUID facility,UUID id,String key,Long version) {
        UUID actor=access.require(facility,"refund.request"); access.lock(facility);
        var fingerprint=Map.of("refundId",id,"version",version); var old=dedup.previous(facility,actor,"REFUND_EXECUTE",key,fingerprint); if(old.isPresent()) { return old.get(); }
        var row=db.one("select * from medora.refund where id=? and facility_id=? for update",id,facility); state(row,"APPROVED"); expected(row,version);
        if(row.get("approvedBy")==null || row.get("approvedBy").equals(row.get("requestedBy"))) { throw conflict("SEPARATE_APPROVAL_REQUIRED"); }
        var payment=paymentForRefund(facility,uuid(row,"paymentId")); var invoice=invoice(facility,uuid(payment,"invoiceId"));
        if(reservedRefunds(uuid(row,"paymentId")).compareTo(money(payment,"amount"))>0) { throw conflict("REFUND_EXCEEDS_AVAILABLE_PAYMENT"); }
        finish(facility,id,uuid(invoice,"id"),actor);
        return dedup.save(facility,actor,"REFUND_EXECUTE",key,fingerprint,refundResult(facility,id,true));
    }
    public Map<String,Object> refund(UUID facility,UUID id) {
        UUID actor=access.require(facility,"invoice.read"); var row=refundResult(facility,id,false);
        audit.record(actor,facility,"REFUND_READ","REFUND",id);
        if(row.get("status").equals("COMPLETED")) { return refundResult(facility,id,true); } return row;
    }
    private void refundEvent(UUID facility,UUID id,UUID actor,String action,String reason) {
        db.update("insert into medora.refund_event(id,refund_id,actor_id,action,reason) values(?,?,?,?,?)",UUID.randomUUID(),id,actor,action,reason);
        audit.record(actor,facility,"REFUND_"+action,"REFUND",id);
    }
}
