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
	public CommandLineRunner dataInit(
			UsuarioRepository repo,
			roomly.roomly.Repository.TenantRepository tenantRepo,
			roomly.roomly.Repository.PropertyRepository propRepo) {
		return args -> {
			// Evaluamos si ya hay tenants, para no duplicar datos
			if (tenantRepo.count() == 0) {
				System.out.println("[SEEDER] Inicializando datos de pruebas para Roomly SaaS...");

				// 1. Crear el Tenant Personalizado
				roomly.roomly.Model.Tenant premiumTenant = new roomly.roomly.Model.Tenant();
				premiumTenant.setCompanyName("Inmobiliaria Premium");
				premiumTenant.setSlug("premium");
				premiumTenant = tenantRepo.save(premiumTenant);

				// 2. Crear el Usuario Seeder asociado a este Tenant
				if (repo.findFirstByEmailIgnoreCase("admin@premium.com").isEmpty()) {
					Usuario u = new Usuario();
					u.setPassword(passwordEncoder.encode("123456"));
					u.setEmail("admin@premium.com");
					u.setTenant(premiumTenant);
					u.setCompanyName("Premium");
					repo.save(u);
				}

				// 3. Crear Propiedades
				roomly.roomly.Model.Property prop1 = new roomly.roomly.Model.Property();
				prop1.setName("Departamento centro - Edificio A");
				prop1.setAddress("Av. Larco 123, Miraflores, Lima, Perú");
				prop1.setTenant(premiumTenant);
				propRepo.save(prop1);

				roomly.roomly.Model.Property prop2 = new roomly.roomly.Model.Property();
				prop2.setName("Complejo Residencial B");
				prop2.setAddress("San Isidro, Lima, Perú");
				prop2.setTenant(premiumTenant);
				propRepo.save(prop2);

				System.out.println("[SEEDER] Datos cargados exitosamente. Loguéate con: admin@premium.com / 123456");
			}
		};
	}

}
