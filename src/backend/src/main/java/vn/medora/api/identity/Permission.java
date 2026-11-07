package vn.medora.api.identity;

import jakarta.persistence.*;

@Entity
@Table(name="permission", schema ="medora")

public class Permission {
    @Id
    @Column(nullable = false, length = 80)
    private  String code;
    @Column(nullable = false, length = 200)
    private String description;
    protected Permission() {
    }
    public String getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }
}
