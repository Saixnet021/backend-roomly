package roomly.roomly.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.DTO.RoomDTO;
import roomly.roomly.Model.Property;
import roomly.roomly.Model.Room;
import roomly.roomly.Model.Tenant;
import roomly.roomly.Model.Usuario;
import roomly.roomly.Repository.PropertyRepository;
import roomly.roomly.Repository.RoomRepository;
import roomly.roomly.Repository.UsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PropertyRepository propertyRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Tenant getCurrentTenant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            Usuario user = usuarioRepository.findFirstByEmailIgnoreCase(email).orElse(null);
            if (user != null) {
                return user.getTenant();
            }
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getRoomsByProperty(@RequestParam Long propertyId) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null || !property.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }

        List<Room> rooms = roomRepository.findByProperty(property);
        List<RoomDTO> dtos = rooms.stream().map(r -> {
            RoomDTO dto = new RoomDTO();
            dto.setId(r.getId());
            dto.setRoomNumber(r.getRoomNumber());
            dto.setPrice(r.getPrice());
            dto.setStatus(r.getStatus());
            dto.setPropertyId(property.getId());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<RoomDTO> create(@RequestBody RoomDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Property property = propertyRepository.findById(dto.getPropertyId()).orElse(null);
        if (property == null || !property.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }

        Room r = new Room();
        r.setRoomNumber(dto.getRoomNumber());
        r.setPrice(dto.getPrice());
        r.setStatus(dto.getStatus() != null ? dto.getStatus() : "Disponible");
        r.setProperty(property);
        r.setTenant(tenant);

        Room saved = roomRepository.save(r);
        dto.setId(saved.getId());
        dto.setStatus(saved.getStatus());

        return ResponseEntity.status(201).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDTO> update(@PathVariable Long id, @RequestBody RoomDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Room r = roomRepository.findById(id).orElse(null);
        if (r == null || !r.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }

        r.setRoomNumber(dto.getRoomNumber());
        r.setPrice(dto.getPrice());
        r.setStatus(dto.getStatus());
        roomRepository.save(r);

        dto.setId(r.getId());
        dto.setPropertyId(r.getProperty().getId());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Room r = roomRepository.findById(id).orElse(null);
        if (r == null || !r.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }

        roomRepository.delete(r);
        return ResponseEntity.ok(java.util.Map.of("message", "Cuarto eliminado con éxito"));
    }
}
