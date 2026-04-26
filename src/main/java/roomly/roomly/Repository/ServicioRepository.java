package roomly.roomly.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomly.roomly.Model.*;
import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findByTenant(Tenant tenant);
    List<Servicio> findByProperty(Property property);
    List<Servicio> findByRoom(Room room);
    List<Servicio> findByPropertyAndTenant(Property property, Tenant tenant);
    List<Servicio> findByRoomAndTenant(Room room, Tenant tenant);
}
