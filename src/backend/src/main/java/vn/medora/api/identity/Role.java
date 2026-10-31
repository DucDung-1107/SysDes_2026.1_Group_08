package vn.medora.api.identity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_role", schema = "medora")
public class Role {

    @Id
    @Column(nullable = false, length = 40)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission",
        schema = "medora",
        joinColumns = @JoinColumn(name = "role_code"),
        inverseJoinColumns = @JoinColumn(name = "permission_code")
    )
    private Set<Permission> permissions = new HashSet<>();


    protected Role() {
    }
    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }
}
