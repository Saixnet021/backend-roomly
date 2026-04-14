package roomly.roomly.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.Services.UsuarioService;
import roomly.roomly.Repository.UsuarioRepository;
import roomly.roomly.Model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

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
            // devuelve el token como JSON { "token": "..." }
            return ResponseEntity.ok(Map.of("token", token));
        } catch (AuthenticationException ex) {
            // si falla la autenticación, devuelve 401 con mensaje JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        // Verifica si el username ya existe
        if (usuarioRepository.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Usuario ya existe"));
        }
        // validar email
        if (usuarioRepository
            .findByEmail(req.email())
            .isPresent()) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error",
                "Email ya registrado"
            ));
        }
        // Crea y guarda el usuario con la contraseña codificada
        Usuario u = new Usuario();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setEmail(req.email());
        usuarioRepository.save(u);
        // Opcional: generar token inmediatamente y devolverlo
        // cargamos el UserDetails usando el email (UsuarioService busca por email)
        UserDetails userDetails = usuarioService.loadUserByUsername(u.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token));
    }
}
