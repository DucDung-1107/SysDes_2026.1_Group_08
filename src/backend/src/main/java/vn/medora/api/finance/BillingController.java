package vn.medora.api.finance;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vn.medora.api.common.VersionRequest;
import java.util.*;
import static vn.medora.api.finance.FinanceDtos.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class BillingController {
    private final BillingService service;
    public BillingController(BillingService service) { this.service=service; }
    @GetMapping("/service-prices") public List<Map<String,Object>> prices(@PathVariable UUID facility,@RequestParam UUID serviceId) { return service.prices(facility,serviceId); }
    @PostMapping("/service-prices") public Map<String,Object> price(@PathVariable UUID facility,@Valid @RequestBody PriceInput r) { return service.price(facility,r); }
    @PostMapping("/service-prices/{id}/end") public Map<String,Object> endPrice(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody PriceEnd r) { return service.endPrice(facility,id,r); }
    @PostMapping("/encounters/{encounter}/invoice") public Map<String,Object> create(@PathVariable UUID facility,@PathVariable UUID encounter,@RequestHeader(value="Idempotency-Key",required=false) String key) { return service.create(facility,encounter,key); }
    @GetMapping("/invoices") public List<Map<String,Object>> list(@PathVariable UUID facility,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(facility,page,size); }
    @GetMapping("/invoices/{id}") public Map<String,Object> get(@PathVariable UUID facility,@PathVariable UUID id) { return service.get(facility,id); }
    @PostMapping("/invoices/{id}/discount") public Map<String,Object> discount(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody DiscountInput r) { return service.discount(facility,id,r); }
    @PostMapping("/invoices/{id}/issue") public Map<String,Object> issue(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody VersionRequest r) { return service.issue(facility,id,r.version()); }
    @PostMapping("/invoices/{id}/payments") public Map<String,Object> payment(@PathVariable UUID facility,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody PaymentInput r) { return service.payment(facility,id,key,r); }
    @GetMapping("/payments/{id}/receipt") public Map<String,Object> receipt(@PathVariable UUID facility,@PathVariable UUID id) { return service.receipt(facility,id); }
    @PostMapping("/refunds") public Map<String,Object> requestRefund(@PathVariable UUID facility,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody RefundInput r) { return service.requestRefund(facility,key,r); }
    @GetMapping("/refunds/{id}") public Map<String,Object> refund(@PathVariable UUID facility,@PathVariable UUID id) { return service.refund(facility,id); }
    @PostMapping("/refunds/{id}/approval") public Map<String,Object> approve(@PathVariable UUID facility,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody ApprovalInput r) { return service.approve(facility,id,key,r); }
    @PostMapping("/refunds/{id}/execute") public Map<String,Object> execute(@PathVariable UUID facility,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody VersionRequest r) { return service.execute(facility,id,key,r.version()); }
}
