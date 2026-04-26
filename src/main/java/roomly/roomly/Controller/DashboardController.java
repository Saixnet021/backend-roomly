package roomly.roomly.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.Model.*;
import roomly.roomly.Repository.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private InquilinoRepository inquilinoRepository;

    private Tenant getCurrentTenant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return usuarioRepository.findFirstByEmailIgnoreCase(ud.getUsername())
                .map(Usuario::getTenant).orElse(null);
        }
        return null;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails ud)) return ResponseEntity.status(403).build();
        
        Usuario user = usuarioRepository.findFirstByEmailIgnoreCase(ud.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();
        
        // Si el usuario es inquilino, devolvemos todo en cero para el MVP (su panel es diferente)
        if ("INQUILINO".equals(user.getRole())) {
            return ResponseEntity.ok(Map.of(
                "totalProperties", 0, "totalRooms", 0, "occupiedRooms", 0,
                "availableRooms", 0, "monthlyIncome", 0, "totalInquilinos", 0, "occupancyRate", 0
            ));
        }

        Tenant tenant = user.getTenant();
        if (tenant == null) return ResponseEntity.status(403).build();

        List<Property> props = propertyRepository.findByTenant(tenant);
        long totalProps = props.size();

        // Acumular stats de cuartos y propiedades
        long totalRooms = 0, occupiedRooms = 0;
        double monthlyIncome = 0.0;
        
        for (Property p : props) {
            List<Room> rooms = roomRepository.findByProperty(p);
            totalRooms += rooms.size();
            
            // Caso 1: Renta por cuartos
            for (Room r : rooms) {
                if ("Ocupado".equals(r.getStatus())) {
                    occupiedRooms++;
                    if (r.getPrice() != null) monthlyIncome += r.getPrice();
                }
            }
            
            // Caso 2: Renta de departamento completo 
            // Buscamos inquilinos asignados a la propiedad pero sin cuarto
            List<Inquilino> fullPropTenants = inquilinoRepository.findByTenant(tenant).stream()
                .filter(i -> i.getProperty() != null && i.getProperty().getId().equals(p.getId()) && i.getRoom() == null)
                .toList();
                
            if (!fullPropTenants.isEmpty() && p.getPrice() != null) {
                // Si hay inquilinos en el dpto completo, sumamos el precio de la propiedad una sola vez (un contrato)
                monthlyIncome += p.getPrice();
            }
        }

        long totalInquilinos = inquilinoRepository.findByTenant(tenant).size();
        double occupancyRate = totalRooms > 0 ? (double) occupiedRooms / totalRooms * 100 : 0;

        return ResponseEntity.ok(Map.of(
            "totalProperties", totalProps,
            "totalRooms", totalRooms,
            "occupiedRooms", occupiedRooms,
            "availableRooms", totalRooms - occupiedRooms,
            "monthlyIncome", monthlyIncome,
            "totalInquilinos", totalInquilinos,
            "occupancyRate", Math.round(occupancyRate * 10.0) / 10.0
        ));
    }
}
