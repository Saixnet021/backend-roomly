package roomly.roomly.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomly.roomly.Model.Property;
import roomly.roomly.Model.Tenant;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByTenant(Tenant tenant);
}
