package vn.medora.api.attachment;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.medora.api.common.*;
import java.nio.*;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;
import static vn.medora.api.common.SqlRows.*;
import static vn.medora.api.common.ClinicAccess.*;
import static vn.medora.api.scheduling.SchedulingService.expected;

@Service @Transactional
public class AttachmentService {
    private final Store db; private final ClinicAccess access; private final AuditService audit; private final BlobStorage storage;
    public AttachmentService(Store db,ClinicAccess access,AuditService audit,BlobStorage storage) { this.db=db; this.access=access; this.audit=audit; this.storage=storage; }
    private void scope(UUID facility,UUID encounter,UUID order,boolean write) {
        access.require(facility,write?"attachment.write":"attachment.read");
        if(access.permits(facility,"procedure.execute")) {
            if(order==null) { throw new AccessDeniedException("ORDER_ATTACHMENT_REQUIRED"); }
            var assigned=db.one("select * from medora.procedure_order where id=? and facility_id=? and encounter_id=?",order,facility,encounter);
            if(!access.actor().equals(assigned.get("staffId"))) { throw new AccessDeniedException("PROCEDURE_NOT_ASSIGNED"); }
            if(write) { state(assigned,"SCHEDULED","IN_PROGRESS"); }
        } else {
            var visit=access.encounter(facility,encounter,"clinical.read",write);
            if(write) { access.open(visit); }
            if(order!=null) { db.one("select id from medora.procedure_order where id=? and facility_id=? and encounter_id=?",order,facility,encounter); }
        }
    }
    private String hash(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch(Exception exception) { throw new IllegalStateException(exception); }
    }
    private String media(byte[] bytes,String declared) {
        if("application/pdf".equals(declared)&&bytes.length>=5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")) { return declared; }
        if("image/png".equals(declared)&&bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) { return declared; }
        if("image/jpeg".equals(declared)&&bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255) { return declared; }
        if("text/plain".equals(declared)) {
            try { String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
                if(text.indexOf(0)<0 && !text.stripLeading().startsWith("<")) { return declared; }
            } catch(CharacterCodingException ignored) { }
        }
        throw bad("UNSUPPORTED_FILE_CONTENT");
    }
    public Map<String,Object> upload(UUID facility,UUID encounter,UUID order,MultipartFile file) {
        access.require(facility,"attachment.write"); access.lock(facility); scope(facility,encounter,order,true);
        if(file.isEmpty()||file.getSize()>10*1024*1024) { throw bad("FILE_SIZE_OUT_OF_RANGE"); }
        String filename=Objects.toString(file.getOriginalFilename(),"attachment").replace('\\','_').replace('/','_').replaceAll("[\\p{Cntrl}]","_");
        if(filename.length()>200) { filename=filename.substring(filename.length()-200); }
        UUID id=UUID.randomUUID(); String key=id+".blob";
        try {
            byte[] data=file.getBytes(); String type=media(data,file.getContentType());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) { storage.removeUncommitted(key); } }
            });
            storage.put(key,data,type);
            db.update("""
                insert into medora.attachment(id,facility_id,encounter_id,order_id,filename,media_type,byte_size,sha256,storage_key,uploaded_by)
                values(?,?,?,?,?,?,?,?,?,?)
                """,id,facility,encounter,order,filename,type,data.length,hash(data),key,access.actor());
        } catch(java.io.IOException|software.amazon.awssdk.core.exception.SdkException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"PRIVATE_STORAGE_UNAVAILABLE");
        }
        audit.record(access.actor(),facility,"ATTACHMENT_UPLOADED","ATTACHMENT",id);
        return publicView(db.one("select * from medora.attachment where id=?",id));
    }
    public List<Map<String,Object>> list(UUID facility,UUID encounter,int page,int size) {
        access.require(facility,"attachment.read"); page(page,size,"id");
        if(!access.permits(facility,"procedure.execute")) { scope(facility,encounter,null,false); }
        else if(!db.exists("select exists(select 1 from medora.procedure_order where facility_id=? and encounter_id=? and staff_id=?)",facility,encounter,access.actor())) {
            throw new AccessDeniedException("PROCEDURE_NOT_ASSIGNED");
        }
        audit.record(access.actor(),facility,"ATTACHMENTS_READ","ENCOUNTER",encounter);
        return db.list("""
            select a.* from medora.attachment a where a.facility_id=? and a.encounter_id=? and not a.archived
            and (? or exists(select 1 from medora.procedure_order o where o.id=a.order_id and o.staff_id=?))
            order by a.uploaded_at desc,a.id limit ? offset ?
            """,facility,encounter,!access.permits(facility,"procedure.execute"),access.actor(),size,(long)page*size).stream().map(this::publicView).toList();
    }
    public record Download(byte[] bytes,String mediaType,String filename) { }
    public Download download(UUID facility,UUID id) {
        access.require(facility,"attachment.read");
        var row=db.one("select * from medora.attachment where id=? and facility_id=? and not archived",id,facility);
        scope(facility,uuid(row,"encounterId"),uuid(row,"orderId"),false);
        try {
            byte[] bytes=storage.get((String)row.get("storageKey"));
            if(bytes.length!=number(row,"byteSize")||!hash(bytes).equals(row.get("sha256"))) { throw new java.io.IOException("Object integrity mismatch"); }
            audit.record(access.actor(),facility,"ATTACHMENT_DOWNLOADED","ATTACHMENT",id);
            return new Download(bytes,(String)row.get("mediaType"),(String)row.get("filename"));
        } catch(java.io.IOException|software.amazon.awssdk.core.exception.SdkException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"PRIVATE_STORAGE_UNAVAILABLE");
        }
    }
    public Map<String,Object> archive(UUID facility,UUID id,Long version,String reason) {
        access.require(facility,"attachment.write"); access.lock(facility); var row=db.one("select * from medora.attachment where id=? and facility_id=?",id,facility);
        scope(facility,uuid(row,"encounterId"),uuid(row,"orderId"),true); expected(row,version); reason(reason);
        db.update("update medora.attachment set archived=true,archive_reason=?,version=version+1 where id=?",reason,id);
        audit.record(access.actor(),facility,"ATTACHMENT_ARCHIVED","ATTACHMENT",id); return publicView(db.one("select * from medora.attachment where id=?",id));
    }
    private Map<String,Object> publicView(Map<String,Object> row) { var result=new LinkedHashMap<>(row); result.remove("storageKey"); return result; }
}
