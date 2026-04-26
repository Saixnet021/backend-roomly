package roomly.roomly.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomly.roomly.Model.Room;
import roomly.roomly.Model.Property;
import roomly.roomly.Model.Tenant;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByTenant(Tenant tenant);
    List<Room> findByProperty(Property property);
}
