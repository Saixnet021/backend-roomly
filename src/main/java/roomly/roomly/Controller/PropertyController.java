package roomly.roomly.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.DTO.PropertyDTO;
import roomly.roomly.Model.Property;
import roomly.roomly.Model.Room;
import roomly.roomly.Model.Usuario;
import roomly.roomly.Repository.PropertyRepository;
import roomly.roomly.Repository.RoomRepository;
import roomly.roomly.Repository.UsuarioRepository;
import roomly.roomly.Repository.InquilinoRepository;
import roomly.roomly.Model.Tenant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    @Autowired
    private PropertyRepository propertyRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private InquilinoRepository inquilinoRepository;

    private Tenant getCurrentTenant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername(); // loadUserByUsername uses email
            Usuario user = usuarioRepository.findFirstByEmailIgnoreCase(email).orElse(null);
            if (user != null) {
                return user.getTenant();
            }
        }
        return null;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<PropertyDTO>> getAllProperties() {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            // El usuario actual no tiene Tenant (cuenta antigua o super-admin).
            // Retornamos lista vacía en lugar de 403 para no romper el dashboard frontend.
            return ResponseEntity.ok(List.of());
        }

        List<Property> properties = propertyRepository.findByTenant(tenant);
        
        List<PropertyDTO> dtos = properties.stream().map(p -> {
            PropertyDTO dto = new PropertyDTO();
            dto.setId(p.getId());
            dto.setName(p.getName());
            dto.setAddress(p.getAddress());
            dto.setPrice(p.getPrice());

            // Datos reales de cuartos
            List<Room> rooms = roomRepository.findByProperty(p);
            long total = rooms.size();
            long occupied = rooms.stream().filter(r -> "Ocupado".equals(r.getStatus())).count();
            dto.setRoomCount(total);
            dto.setOccupiedRooms(occupied);
            
            // Cálculo de ingresos
            double income = rooms.stream()
                .filter(r -> "Ocupado".equals(r.getStatus()) && r.getPrice() != null)
                .mapToDouble(Room::getPrice).sum();
                
            // + Ingreso por departamento completo si aplica
            boolean isFullRented = inquilinoRepository.findByTenant(tenant).stream()
                .anyMatch(i -> i.getProperty() != null && i.getProperty().getId().equals(p.getId()) && i.getRoom() == null);
            
            if (isFullRented && p.getPrice() != null) {
                income += p.getPrice();
            }

            dto.setTotalIncome(income);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping
    public ResponseEntity<PropertyDTO> createProperty(@RequestBody PropertyDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        Property p = new Property();
        p.setName(dto.getName());
        p.setAddress(dto.getAddress());
        p.setPrice(dto.getPrice());
        p.setTenant(tenant);
        Property saved = propertyRepository.save(p);
        dto.setId(saved.getId());
        dto.setRoomCount(0);
        dto.setOccupiedRooms(0);
        dto.setTotalIncome(0.0);
        return ResponseEntity.status(201).body(dto);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PropertyDTO> updateProperty(@PathVariable Long id, @RequestBody PropertyDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        
        Property p = propertyRepository.findById(id).orElse(null);
        if (p == null || !p.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }
        
        p.setName(dto.getName());
        p.setAddress(dto.getAddress());
        p.setPrice(dto.getPrice());
        propertyRepository.save(p);
        
        // Mantener stats pasados
        dto.setId(p.getId());
        return ResponseEntity.ok(dto);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable Long id) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        
        Property p = propertyRepository.findById(id).orElse(null);
        if (p == null || !p.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build(); // No existe o no le pertenece
        }
        
        propertyRepository.delete(p);
        return ResponseEntity.ok(java.util.Map.of("message", "Propiedad eliminada con éxito"));
    }
}
