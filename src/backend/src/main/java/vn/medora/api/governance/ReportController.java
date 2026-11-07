package vn.medora.api.governance;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) { this.service=service; }
    @GetMapping({"/dashboard","/reports/operations"}) public Map<String,Object> dashboard(@PathVariable UUID facility,@RequestParam Instant from,@RequestParam Instant to) { return service.operations(facility,from,to); }
    @GetMapping("/reports/revenue") public Map<String,Object> revenue(@PathVariable UUID facility,@RequestParam Instant from,@RequestParam Instant to) { return service.revenue(facility,from,to,false); }
    @GetMapping("/reports/revenue.csv") public ResponseEntity<String> revenueCsv(@PathVariable UUID facility,@RequestParam Instant from,@RequestParam Instant to) {
        return csv(service.csv(List.of(service.revenue(facility,from,to,true)),List.of("from","to","currency","paymentsReceived","refundsCompleted","netCash")),"medora-revenue.csv");
    }
    @GetMapping("/reports/invoices.csv") public ResponseEntity<String> invoices(@PathVariable UUID facility,@RequestParam Instant from,@RequestParam Instant to,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="100") int size) {
        return csv(service.csv(service.invoices(facility,from,to,page,size),List.of("id","status","currency","total","netPaid")),"medora-invoices.csv");
    }
    private ResponseEntity<String> csv(String body,String name) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").header(HttpHeaders.CACHE_CONTROL,"no-store").body(body);
    }
}
