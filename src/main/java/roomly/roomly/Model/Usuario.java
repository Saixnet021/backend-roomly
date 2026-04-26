package roomly.roomly.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Usuario extiende `AbstractTenantEntity` para ser multitenant.
 * - `tenantId` y `companyName` vienen de la superclase.
 */
@Entity
@Getter
@Setter
public class Usuario extends AbstractTenantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String role; // "PROPIETARIO" or "INQUILINO"

    // Relación opcional hacia `Tenant` (referencia por FK `tenant_ref_id`).
    // Mantenemos también la columna `tenant_id` string heredada por compatibilidad.
    @jakarta.persistence.ManyToOne
    // Usamos `tenant_id` como FK numérica hacia la tabla `tenant` (reemplaza el campo string)
    @jakarta.persistence.JoinColumn(name = "tenant_id")
    private roomly.roomly.Model.Tenant tenant;

    // Nota: no eliminamos `tenantId` string heredada (si existe) para evitar migraciones manuales.
}
