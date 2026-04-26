package roomly.roomly.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.DTO.InquilinoDTO;
import roomly.roomly.Model.Inquilino;
import roomly.roomly.Model.Property;
import roomly.roomly.Model.Tenant;
import roomly.roomly.Model.Usuario;
import roomly.roomly.Repository.InquilinoRepository;
import roomly.roomly.Repository.PropertyRepository;
import roomly.roomly.Repository.RoomRepository;
import roomly.roomly.Repository.UsuarioRepository;
import roomly.roomly.Model.Room;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inquilinos")
public class InquilinoController {

    @Autowired
    private InquilinoRepository inquilinoRepository;

    @Autowired
    private PropertyRepository propertyRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<InquilinoDTO>> getAll() {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.ok(List.of());

        List<Inquilino> list = inquilinoRepository.findByTenant(tenant);
        
        List<InquilinoDTO> dtos = list.stream().map(i -> {
            InquilinoDTO dto = new InquilinoDTO();
            dto.setId(i.getId());
            dto.setName(i.getName());
            dto.setDocument(i.getDocument());
            dto.setEmail(i.getEmail());
            dto.setPhone(i.getPhone());
            dto.setStatus(i.getStatus());
            if (i.getProperty() != null) {
                dto.setPropertyId(i.getProperty().getId());
                dto.setPropertyName(i.getProperty().getName());
            }
            if (i.getRoom() != null) {
                dto.setRoomId(i.getRoom().getId());
                dto.setRoomNumber(i.getRoom().getRoomNumber());
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<InquilinoDTO> getMyInfo() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails ud)) return ResponseEntity.status(403).build();
        
        String email = ud.getUsername();
        System.out.println("DEBUG: Fetching info for inquilino email: " + email);
        
        return inquilinoRepository.findFirstByEmailIgnoreCase(email).map(i -> {
            System.out.println("DEBUG: Found inquilino id: " + i.getId() + ", Property: " + (i.getProperty() != null ? i.getProperty().getName() : "NULL"));
            InquilinoDTO dto = new InquilinoDTO();
            dto.setId(i.getId());
            dto.setName(i.getName());
            dto.setDocument(i.getDocument());
            dto.setEmail(i.getEmail());
            dto.setPhone(i.getPhone());
            dto.setStatus(i.getStatus());
            if (i.getProperty() != null) {
                dto.setPropertyId(i.getProperty().getId());
                dto.setPropertyName(i.getProperty().getName());
            }
            if (i.getRoom() != null) {
                dto.setRoomId(i.getRoom().getId());
                dto.setRoomNumber(i.getRoom().getRoomNumber());
            }
            return ResponseEntity.ok(dto);
        }).orElseGet(() -> {
            System.out.println("DEBUG: No inquilino found with email: " + email);
            return ResponseEntity.status(404).build();
        });
    }

    @PostMapping
    public ResponseEntity<InquilinoDTO> create(@RequestBody InquilinoDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Inquilino i = new Inquilino();
        i.setName(dto.getName());
        i.setDocument(dto.getDocument());
        i.setEmail(dto.getEmail());
        i.setPhone(dto.getPhone());
        i.setStatus(dto.getStatus() == null ? "ACTIVO" : dto.getStatus());
        i.setTenant(tenant);

        // Si mandan propertyId, asignamos la propiedad verificando que sea del tenant
        if (dto.getPropertyId() != null) {
            Property p = propertyRepository.findById(dto.getPropertyId()).orElse(null);
            if (p != null && p.getTenant().getId().equals(tenant.getId())) {
                i.setProperty(p);
            }
        }
        
        // Si mandan roomId, asignamos la habitacion
        if (dto.getRoomId() != null) {
            Room r = roomRepository.findById(dto.getRoomId()).orElse(null);
            if (r != null && r.getTenant().getId().equals(tenant.getId())) {
                i.setRoom(r);
                // Si el cuarto está disponible, pasarlo a ocupado
                r.setStatus("Ocupado");
                roomRepository.save(r);
            }
        }

        Inquilino saved = inquilinoRepository.save(i);

        // Crear usuario para que el inquilino pueda loguearse
        if (dto.getEmail() != null && usuarioRepository.findFirstByEmailIgnoreCase(dto.getEmail()).isEmpty()) {
            Usuario u = new Usuario();
            u.setEmail(dto.getEmail());
            u.setFirstName(dto.getName());
            String pass = (dto.getPassword() != null && !dto.getPassword().isBlank()) ? dto.getPassword() : "123456";
            u.setPassword(passwordEncoder.encode(pass));
            u.setRole("INQUILINO");
            u.setTenant(tenant);
            u.setCompanyName(tenant.getCompanyName());
            usuarioRepository.save(u);
        }
        dto.setId(saved.getId());
        dto.setStatus(saved.getStatus());
        if (saved.getProperty() != null) {
            dto.setPropertyName(saved.getProperty().getName());
        }

        return ResponseEntity.status(201).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InquilinoDTO> update(@PathVariable Long id, @RequestBody InquilinoDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Inquilino i = inquilinoRepository.findById(id).orElse(null);
        if (i == null || !i.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }

        i.setName(dto.getName());
        i.setDocument(dto.getDocument());
        i.setEmail(dto.getEmail());
        i.setPhone(dto.getPhone());
        i.setStatus(dto.getStatus());

        if (dto.getPropertyId() != null) {
            Property p = propertyRepository.findById(dto.getPropertyId()).orElse(null);
            if (p != null && p.getTenant().getId().equals(tenant.getId())) {
                i.setProperty(p);
            }
        } else {
            i.setProperty(null); // Quitar asignación
        }

        if (dto.getRoomId() != null) {
            Room r = roomRepository.findById(dto.getRoomId()).orElse(null);
            if (r != null && r.getTenant().getId().equals(tenant.getId())) {
                i.setRoom(r);
                r.setStatus("Ocupado");
                roomRepository.save(r);
            }
        } else {
            // Liberar cuarto anterior (opcional para un MVP avanzado, acá solo lo desasignamos)
            if (i.getRoom() != null) {
                Room r = i.getRoom();
                r.setStatus("Disponible");
                roomRepository.save(r);
            }
            i.setRoom(null);
        }

        Inquilino saved = inquilinoRepository.save(i);
        if (saved.getProperty() != null) dto.setPropertyName(saved.getProperty().getName());
        dto.setId(saved.getId());

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        Inquilino i = inquilinoRepository.findById(id).orElse(null);
        if (i == null || !i.getTenant().getId().equals(tenant.getId())) {
            return ResponseEntity.status(404).build();
        }

        inquilinoRepository.delete(i);
        return ResponseEntity.ok(java.util.Map.of("message", "Inquilino eliminado con éxito"));
    }
}
