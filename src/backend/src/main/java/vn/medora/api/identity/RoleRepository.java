package vn.medora.api.identity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository

    extends JpaRepository<Role,String>    {


}
