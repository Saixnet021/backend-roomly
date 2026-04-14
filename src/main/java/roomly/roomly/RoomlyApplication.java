package roomly.roomly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import roomly.roomly.Repository.UsuarioRepository;
import roomly.roomly.Model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class RoomlyApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomlyApplication.class, args);
	}

	@Autowired
	private PasswordEncoder passwordEncoder; // inyecta el encoder para crear usuario de prueba

	@Bean
	public CommandLineRunner dataInit(UsuarioRepository repo) {
		return args -> {
			// si no existe usuario 'test', lo creamos con password codificado
			if (repo.findByUsername("test").isEmpty()) {
				Usuario u = new Usuario();
				u.setUsername("test");
				u.setPassword(passwordEncoder.encode("password"));
				u.setEmail("test@example.com");
				repo.save(u);
			}
		};
	}

}
