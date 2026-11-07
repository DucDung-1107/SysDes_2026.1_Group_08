package vn.medora.api.identity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "user_role_assignment",
        schema = "medora",
        uniqueConstraints = @UniqueConstraint(
            name = "uq_user_role_assignment",
            columnNames = {"user_account_id","facility_id","role_code"}
        )
    )
public class UserRoleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_account_id",nullable = false)
    private UserAccount account;
    @ManyToOne(fetch = FetchType.LAZY,optional=false)
    @JoinColumn(name="role_code",nullable = false)
    private Role role;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="facility_id",nullable=false)
    private Facility facility;

    protected UserRoleAssignment(){

    }
    public UserRoleAssignment(UserAccount account,Role role,Facility facility){
        this.account = account;
        this.role = role;
        this.facility=facility;
    }
    public UUID getId() {
    return id;
    }
    public UserAccount getAccount() {
        return account;
    }
    public Role getRole() {
        return role;
    }
    public Facility getFacility() {
        return facility;
    }

}
