package roomly.roomly.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomly.roomly.Model.Inquilino;
import roomly.roomly.Model.Tenant;
import java.util.List;

public interface InquilinoRepository extends JpaRepository<Inquilino, Long> {
    List<Inquilino> findByTenant(Tenant tenant);
    java.util.Optional<Inquilino> findFirstByEmailIgnoreCase(String email);
}
