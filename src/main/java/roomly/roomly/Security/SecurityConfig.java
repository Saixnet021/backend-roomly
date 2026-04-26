package roomly.roomly.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import roomly.roomly.Services.UsuarioService;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // inyecta el servicio de usuarios que implementa UserDetailsService
    @Autowired
    private UsuarioService usuarioService;

    // inyecta el filtro JWT que validará el token en cada petición
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // expone el UserDetailsService usando el servicio existente
    @Bean
    public UserDetailsService userDetailsService() {
        return usuarioService;
    }

    // configura el AuthenticationProvider usando DaoAuthenticationProvider y BCrypt
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // En Spring Security 6 / Spring Boot 4 el constructor de DaoAuthenticationProvider
        // acepta el UserDetailsService como argumento. Usamos ese constructor para evitar
        // incompatibilidades con la API.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarioService);
        provider.setPasswordEncoder(passwordEncoder()); // usa BCrypt para comparar contraseñas
        return provider;
    }

    // bean para codificar contraseñas con BCrypt
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    // expone AuthenticationManager para uso en el controlador de autenticación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // configuración principal de seguridad: usa JWT (stateless), habilita CORS y registra el filtro JWT
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // deshabilita CSRF para API REST
                .cors(cors -> {}) // habilita CORS usando el CorsConfigurationSource bean
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // sin sesión
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/register").permitAll() // permite rutas de auth y registro
                        .anyRequest().authenticated() // el resto requiere autenticación
                );

        // añade el filtro que valida JWT antes del filtro de autenticación estándar
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // configuración CORS para permitir llamadas desde Angular en localhost:4200
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        // Leer orígenes permitidos desde la variable de entorno FRONTEND_ORIGINS (coma-separada).
        // Si no existe, usar localhost:4200 y patrones para Vercel (subdominios) por defecto.
        String originsEnv = System.getenv("FRONTEND_ORIGINS");
        List<String> allowedPatterns;
        if (originsEnv != null && !originsEnv.isBlank()) {
            String[] arr = originsEnv.split(",");
            for (int i = 0; i < arr.length; i++) arr[i] = arr[i].trim();
            allowedPatterns = Arrays.asList(arr);
        } else {
            allowedPatterns = List.of("http://localhost:4200", "https://frontend-roomly.vercel.app", "https://*.vercel.app");
        }
        // Usar origin patterns para permitir subdominios en hosts manejados (Vercel, etc.)
        c.setAllowedOriginPatterns(allowedPatterns);
        System.out.println("CORS allowed origin patterns: " + allowedPatterns);
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS")); // métodos permitidos
        c.setAllowedHeaders(List.of("*")); // todos los headers permitidos
        c.setAllowCredentials(true); // permitir credenciales (cookies) si se usa
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

}
