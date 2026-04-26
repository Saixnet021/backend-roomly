package roomly.roomly.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import roomly.roomly.Model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findFirstByEmailIgnoreCase(String email);
    
    // Busca usuarios por tenantId (útil para listar solo entidades del tenant actual)
    java.util.List<Usuario> findByTenantId(String tenantId);
    
}
