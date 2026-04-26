package roomly.roomly.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Tenant simple: guarda `slug` (normalizado) y `companyName` legible.
 * Hibernate creará la tabla automáticamente gracias a `spring.jpa.hibernate.ddl-auto=update`.
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug; // ej: casanovedades

    @Column(name = "company_name")
    private String companyName; // ej: Casa Novedades

    public Tenant(String slug, String companyName) {
        this.slug = slug;
        this.companyName = companyName;
    }

}
