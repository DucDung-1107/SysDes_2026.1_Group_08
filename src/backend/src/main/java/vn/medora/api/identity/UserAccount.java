package vn.medora.api.identity;
import jakarta.persistence.*;
import java.util.UUID;
import java.util.Locale;
import java.time.Instant;






@Entity
@Table(name="user_account",schema = "medora")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false,unique = true,length = 64)
    private String username;
    @Column(nullable = false,length = 160, name = "display_name")
    private String displayName;
    @Column(nullable = false,length = 255, name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=16)
    private AccountStatus status = AccountStatus.ACTIVE;
    @Column(nullable=false, name = "auth_version")
    private long authVersion = 0;
    @Version
    @Column(nullable = false)
    private Long version;

    protected UserAccount(){

    }
    public UserAccount(String username, String displayname, String passwordHash){
        String normalizedUsername = username.strip().toLowerCase(Locale.ROOT);
        if (!normalizedUsername.matches("^[a-z0-9][a-z0-9._-]{2,63}$")){
            throw new IllegalArgumentException("Invalid username");
        }
        this.username=normalizedUsername;
        this.displayName=displayname;
        this.passwordHash=passwordHash;
    }
    @Column(name="created_at",nullable =false,updatable = false)
    private Instant createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt=Instant.now();
    }
    public UUID getId(){
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getDisplayName() {
        return displayName;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public AccountStatus getStatus() {
        return status;
    }
    public long getAuthVersion() {
        return authVersion;
    }
    public Long getVersion(){
        return version;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void updateProfile(String displayName, AccountStatus newStatus) {
    this.displayName = displayName.strip();

    if (this.status != newStatus) {
        this.status = newStatus;
        invalidateSessions();
        }
    }

    public void resetPassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        invalidateSessions();
    }

    public void invalidateSessions() {
        this.authVersion++;
    }

}
