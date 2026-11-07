package vn.medora.api.attachment;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.medora.api.care.CareDtos.ReasonVersion;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequestMapping("/api/facilities/{facility}")
public class AttachmentController {
    private final AttachmentService service;
    public AttachmentController(AttachmentService service) { this.service=service; }
    @PostMapping(value="/encounters/{encounter}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@PathVariable UUID facility,@PathVariable UUID encounter,@RequestParam(required=false) UUID orderId,@RequestPart MultipartFile file) { return service.upload(facility,encounter,orderId,file); }
    @GetMapping("/encounters/{encounter}/attachments") public List<Map<String,Object>> list(@PathVariable UUID facility,@PathVariable UUID encounter,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(facility,encounter,page,size); }
    @GetMapping("/attachments/{id}/download") public ResponseEntity<byte[]> download(@PathVariable UUID facility,@PathVariable UUID id) {
        var file=service.download(facility,id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mediaType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.filename(),StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").body(file.bytes());
    }
    @PostMapping("/attachments/{id}/archive") public Map<String,Object> archive(@PathVariable UUID facility,@PathVariable UUID id,@Valid @RequestBody ReasonVersion r) { return service.archive(facility,id,r.version(),r.reason()); }
}
