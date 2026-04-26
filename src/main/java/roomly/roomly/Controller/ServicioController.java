package roomly.roomly.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.DTO.ServicioDTO;
import roomly.roomly.Model.*;
import roomly.roomly.Repository.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "http://localhost:4200")
public class ServicioController {

    @Autowired private ServicioRepository servicioRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Tenant getCurrentTenant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return usuarioRepository.findFirstByEmailIgnoreCase(ud.getUsername())
                .map(Usuario::getTenant).orElse(null);
        }
        return null;
    }

    private ServicioDTO toDTO(Servicio s) {
        ServicioDTO dto = new ServicioDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setCost(s.getCost());
        dto.setTipo(s.getTipo());
        if (s.getProperty() != null) {
            dto.setPropertyId(s.getProperty().getId());
            dto.setPropertyName(s.getProperty().getName());
        }
        if (s.getRoom() != null) {
            dto.setRoomId(s.getRoom().getId());
            dto.setRoomNumber(s.getRoom().getRoomNumber());
        }
        return dto;
    }

    // GET /api/servicios?propertyId=1 | GET /api/servicios?roomId=3 | GET /api/servicios (todos)
    @GetMapping
    public ResponseEntity<List<ServicioDTO>> getAll(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        List<Servicio> list;

        if (roomId != null) {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null) return ResponseEntity.status(404).build();
            list = servicioRepository.findByRoomAndTenant(room, tenant);
        } else if (propertyId != null) {
            Property prop = propertyRepository.findById(propertyId).orElse(null);
            if (prop == null || !prop.getTenant().getId().equals(tenant.getId()))
                return ResponseEntity.status(404).build();
            list = servicioRepository.findByPropertyAndTenant(prop, tenant);
        } else {
            list = servicioRepository.findByTenant(tenant);
        }

        return ResponseEntity.ok(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<ServicioDTO> create(@RequestBody ServicioDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Servicio s = new Servicio();
        s.setName(dto.getName());
        s.setDescription(dto.getDescription());
        s.setCost(dto.getCost() != null ? dto.getCost() : 0.0);
        s.setTipo(dto.getTipo() != null ? dto.getTipo() : "INCLUIDO");
        s.setTenant(tenant);

        if (dto.getPropertyId() != null) {
            Property prop = propertyRepository.findById(dto.getPropertyId()).orElse(null);
            if (prop != null && prop.getTenant().getId().equals(tenant.getId()))
                s.setProperty(prop);
        }

        if (dto.getRoomId() != null) {
            Room room = roomRepository.findById(dto.getRoomId()).orElse(null);
            if (room != null) s.setRoom(room);
        }

        return ResponseEntity.status(201).body(toDTO(servicioRepository.save(s)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioDTO> update(@PathVariable Long id, @RequestBody ServicioDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Servicio s = servicioRepository.findById(id).orElse(null);
        if (s == null || !s.getTenant().getId().equals(tenant.getId()))
            return ResponseEntity.status(404).build();

        s.setName(dto.getName());
        s.setDescription(dto.getDescription());
        s.setCost(dto.getCost() != null ? dto.getCost() : 0.0);
        s.setTipo(dto.getTipo() != null ? dto.getTipo() : "INCLUIDO");

        if (dto.getPropertyId() != null) {
            Property prop = propertyRepository.findById(dto.getPropertyId()).orElse(null);
            if (prop != null && prop.getTenant().getId().equals(tenant.getId()))
                s.setProperty(prop);
        } else s.setProperty(null);

        if (dto.getRoomId() != null) {
            Room room = roomRepository.findById(dto.getRoomId()).orElse(null);
            if (room != null) s.setRoom(room);
        } else s.setRoom(null);

        return ResponseEntity.ok(toDTO(servicioRepository.save(s)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Servicio s = servicioRepository.findById(id).orElse(null);
        if (s == null || !s.getTenant().getId().equals(tenant.getId()))
            return ResponseEntity.status(404).build();

        servicioRepository.delete(s);
        return ResponseEntity.ok(java.util.Map.of("message", "Servicio eliminado con éxito"));
    }
}
