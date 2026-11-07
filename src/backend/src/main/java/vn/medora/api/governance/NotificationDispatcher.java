package vn.medora.api.governance;

import org.springframework.scheduling.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.medora.api.common.Store;

@Component
public class NotificationDispatcher {
    private final Store db;
    public NotificationDispatcher(Store db) { this.db=db; }
    @Configuration @EnableScheduling static class Scheduling { }
    @Scheduled(fixedDelayString="${medora.notifications.poll-ms:5000}") @Transactional
    public void dispatch() {
        for(var row:db.list("select * from medora.notification where status in ('PENDING','FAILED') and attempts<5 and next_attempt_at<=current_timestamp order by created_at limit 50 for update skip locked")) {
            boolean enabled=db.exists("""
                select exists(select 1 from medora.notification_template t join medora.facility f on f.id=t.facility_id
                join medora.user_account u on u.id=? where t.id=? and t.enabled and f.enabled and u.status='ACTIVE'
                and exists(select 1 from medora.user_role_assignment a where a.facility_id=f.id and a.user_account_id=u.id))
                """,row.get("recipientId"),row.get("templateId"));
            if(enabled) { db.update("update medora.notification set status='DELIVERED',delivered_at=current_timestamp,attempts=attempts+1,last_error=null where id=?",row.get("id")); }
            else { db.update("update medora.notification set status='FAILED',attempts=attempts+1,last_error='CHANNEL_OR_RECIPIENT_DISABLED',next_attempt_at=current_timestamp+interval '30 seconds'*(attempts+1) where id=?",row.get("id")); }
        }
    }
}
