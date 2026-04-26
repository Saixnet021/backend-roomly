package roomly.roomly.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Inquilino extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String document; // DNI, Pasaporte
    private String email;
    private String phone;
    private String status; // Ej: ACTIVO, INACTIVO, MOROSO

    // Propiedad en la que reside (puede estar vacía al crearse)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    // Cuarto al que está asignado, si existe.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // Relación multitenant estricta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
}
