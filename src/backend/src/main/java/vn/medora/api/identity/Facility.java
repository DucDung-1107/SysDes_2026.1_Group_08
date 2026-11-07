package vn.medora.api.identity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="facility",schema = "medora")
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,unique = true,length = 32)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;
    protected Facility() {

    }

    public Facility(String code, String name) {
        this.code =code;
        this.name =name;
    }
    public UUID getId() {
        return id;
    }
    public String getCode() {
        return code;
    }
        public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
