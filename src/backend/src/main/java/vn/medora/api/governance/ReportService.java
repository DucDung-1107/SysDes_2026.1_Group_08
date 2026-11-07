package vn.medora.api.governance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.*;
import java.time.Instant;
import java.util.*;

@Service @Transactional
public class ReportService {
    private final Store db; private final ClinicAccess access; private final AuditService audit;
    public ReportService(Store db,ClinicAccess access,AuditService audit) { this.db=db; this.access=access; this.audit=audit; }
    public Map<String,Object> operations(UUID facility,Instant from,Instant to) {
        UUID actor=access.require(facility,"report.read"); GovernanceService.range(from,to);
        boolean unrestricted=access.permits(facility,"invoice.read");
        var result=db.one("""
            select count(*) as encounters,count(*) filter(where status='COMPLETED') as completed,
                count(*) filter(where status='WAITING') as waiting,count(*) filter(where status='AWAITING_SERVICE') as awaiting_service
            from medora.encounter where facility_id=? and created_at>=? and created_at<? and (? or doctor_id=? or nurse_id=?)
            """,facility,from,to,unrestricted,actor,actor);
        audit.record(actor,facility,"OPERATIONS_REPORT_READ","FACILITY",facility);
        var view=new LinkedHashMap<>(result); view.put("from",from); view.put("to",to); view.put("scope",unrestricted?"FACILITY":"ASSIGNED_ENCOUNTERS"); return view;
    }
    public Map<String,Object> revenue(UUID facility,Instant from,Instant to,boolean export) {
        UUID actor=access.require(facility,export?"report.export":"report.read"); access.require(facility,"invoice.read"); GovernanceService.range(from,to);
        var result=db.one("""
            select coalesce((select sum(amount) from medora.payment where facility_id=? and recorded_at>=? and recorded_at<?),0) as payments_received,
                coalesce((select sum(amount) from medora.refund where facility_id=? and status='COMPLETED' and completed_at>=? and completed_at<?),0) as refunds_completed
            """,facility,from,to,facility,from,to);
        var view=new LinkedHashMap<>(result);
        view.put("netCash",((java.math.BigDecimal)result.get("paymentsReceived")).subtract((java.math.BigDecimal)result.get("refundsCompleted")));
        view.put("currency","VND"); view.put("from",from); view.put("to",to);
        audit.record(actor,facility,export?"REVENUE_REPORT_EXPORTED":"REVENUE_REPORT_READ","FACILITY",facility); return view;
    }
    public List<Map<String,Object>> invoices(UUID facility,Instant from,Instant to,int page,int size) {
        UUID actor=access.require(facility,"report.export"); access.require(facility,"invoice.read"); GovernanceService.range(from,to); ApiSupport.page(page,size,"id");
        var result=db.list("""
            select i.id,i.status,i.currency,i.subtotal-i.discount as total,
                coalesce((select sum(p.amount) from medora.payment p where p.invoice_id=i.id),0)
                -coalesce((select sum(r.amount) from medora.refund r join medora.payment p on p.id=r.payment_id where p.invoice_id=i.id and r.status='COMPLETED'),0) as net_paid
            from medora.invoice i where i.facility_id=? and i.created_at>=? and i.created_at<? order by i.created_at,i.id limit ? offset ?
            """,facility,from,to,size,(long)page*size);
        audit.record(actor,facility,"INVOICE_REPORT_EXPORTED","FACILITY",facility); return result;
    }
    private String cell(Object value) {
        String text=Objects.toString(value,"");
        if(!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0))>=0) { text="'"+text; }
        return "\""+text.replace("\"","\"\"")+"\"";
    }
    public String csv(List<Map<String,Object>> rows,List<String> columns) {
        var result=new StringBuilder(String.join(",",columns)).append("\r\n");
        for(var row:rows) { result.append(columns.stream().map(key->cell(row.get(key))).collect(java.util.stream.Collectors.joining(","))).append("\r\n"); } return result.toString();
    }
}
