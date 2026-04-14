package roomly.roomly.Services;

import java.util.Optional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import roomly.roomly.Model.Usuario;
import roomly.roomly.Repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;


@Service
@AllArgsConstructor
public class UsuarioService implements UserDetailsService{

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isPresent()) {

            var userObj =  usuario.get();
            return User.builder()
                    .username(userObj.getEmail())
                    .password(userObj.getPassword())
                    .roles("USER")
                    .build();
        } else {

            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        
    }

    
}
