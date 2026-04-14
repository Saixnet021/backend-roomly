package roomly.roomly.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import roomly.roomly.Services.UsuarioService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // utilidad para parsear/validar token

    @Autowired
    private UsuarioService usuarioService; // carga UserDetails desde la BD

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization"); // obtiene header Authorization
        String username = null;
        String jwt = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // extrae el token después de 'Bearer '
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt); // extrae el username del token
            } catch (Exception e) {
                // token inválido o parseo fallido -> continuar sin autenticación
            }
        }
        // si hay username y aún no hay autenticación en el contexto, validamos
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = usuarioService.loadUserByUsername(username); // carga datos del usuario
            if (jwtUtil.validateToken(jwt, userDetails)) { // valida token contra UserDetails
                UsernamePasswordAuthenticationToken token =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // establece la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(token);
            }
        }
        // continúa la cadena de filtros
        chain.doFilter(request, response);
    }
}
