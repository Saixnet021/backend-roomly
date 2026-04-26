package roomly.roomly.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roomly.roomly.Services.UsuarioService;
import roomly.roomly.Repository.UsuarioRepository;
import roomly.roomly.Model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    // AuthenticationManager se usa para autenticar programáticamente
    @Autowired
    private AuthenticationManager authenticationManager;

    // servicio que carga el UserDetails (UsuarioService)
    @Autowired
    private UsuarioService usuarioService;

    // utilidad JWT para generar tokens
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository; // para crear nuevos usuarios

    @Autowired
    private roomly.roomly.Repository.TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // para codificar contraseñas al registrar

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            // intenta autenticar las credenciales proporcionadas
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password())
            );
            // si la autenticación es exitosa, extrae el UserDetails
            UserDetails user = (UserDetails) auth.getPrincipal();
            // genera un JWT para el usuario autenticado
            String token = jwtUtil.generateToken(user);
            
            // Obtenemos el slug del tenant
            Usuario u = usuarioRepository.findFirstByEmailIgnoreCase(authRequest.email()).orElse(null);
            String tenantSlug = (u != null && u.getTenant() != null && u.getTenant().getSlug() != null) ? u.getTenant().getSlug() : "";
            String companyName = (u != null && u.getTenant() != null && u.getTenant().getCompanyName() != null) ? u.getTenant().getCompanyName() : "";
            String role = (u != null && u.getRole() != null) ? u.getRole() : "PROPIETARIO";
            String firstName = (u != null && u.getFirstName() != null) ? u.getFirstName() : "";

            // devuelve el token como JSON { "token": "...", "tenant": "...", "companyName": "...", "role": "...", "firstName": "..." }
            return ResponseEntity.ok(Map.of("token", token, "tenant", tenantSlug, "companyName", companyName, "role", role, "firstName", firstName));
        } catch (AuthenticationException ex) {
            // si falla la autenticación, devuelve 401 con mensaje JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            // validar email
            if (usuarioRepository
                .findFirstByEmailIgnoreCase(req.email())
                .isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email ya registrado", "error", "Email ya registrado"));
            }

            // validar largo de company (tenant route)
            if (req.company() != null && req.company().length() > 15) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El nombre de la empresa no puede superar los 15 caracteres", "error", "Nombre de empresa muy largo"));
            }

            // Crea y guarda el usuario con la contraseña codificada
            Usuario u = new Usuario();
            // guardamos la contraseña codificada
            u.setPassword(passwordEncoder.encode(req.password()));
            u.setEmail(req.email());
            u.setFirstName(req.firstName());
            u.setLastName(req.lastName());
            // guardamos el nombre legible de la compañia
            u.setCompanyName(req.company());
            u.setRole("PROPIETARIO");

            // Normalizamos slug de tenant desde company y buscamos/creamos Tenant
            if (req.company() != null && !req.company().isBlank()) {
                String slug = req.company().replaceAll("\\s+", "").toLowerCase().replaceAll("[^a-z0-9]", "");
                roomly.roomly.Model.Tenant tenant = tenantRepository.findBySlug(slug)
                    .orElseGet(() -> tenantRepository.save(new roomly.roomly.Model.Tenant(slug, req.company())));
                // asociamos el tenant al usuario (esto pondrá la FK tenant_ref_id)
                u.setTenant(tenant);
            }

            usuarioRepository.save(u);
            // generar token inmediatamente y devolverlo junto a un mensaje
            UserDetails userDetails = usuarioService.loadUserByUsername(u.getEmail());
            String token = jwtUtil.generateToken(userDetails);
            String tenantSlug = u.getTenant() != null ? u.getTenant().getSlug() : "";
            String companyName = u.getTenant() != null ? u.getTenant().getCompanyName() : "";
            String role = "PROPIETARIO";
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Usuario creado", "token", token,
                "tenant", tenantSlug, "companyName", companyName, "role", role));
        } catch (Exception ex) {
            logger.error("Error en registro de usuario", ex);
            String msg = ex.getMessage() != null ? ex.getMessage() : "Error interno en el servidor";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        }
    }

    // Endpoint separado para que el dueño del SaaS (super-admin) inicie sesión
    // El super-admin debe tener un usuario sin tenantId (tenantId == null)
    @PostMapping("/super-login")
    public ResponseEntity<?> superLogin(@RequestBody AuthRequest authRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password())
            );
            UserDetails user = (UserDetails) auth.getPrincipal();
            // buscamos el Usuario completo para validar tenantId
            var opt = usuarioRepository.findFirstByEmailIgnoreCase(authRequest.email());
            if (opt.isPresent()) {
                Usuario u = opt.get();
                // Ahora comprobamos la relación Tenant; si es null entonces es super-admin
                if (u.getTenant() == null) {
                    String token = jwtUtil.generateToken(user);
                    return ResponseEntity.ok(Map.of("token", token));
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No eres super-admin"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Usuario no encontrado"));
            }
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }
    }
}
