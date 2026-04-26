package roomly.roomly.Model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import roomly.roomly.Security.TenantContext;

/**
 * Superclase para entidades que deben ser multitenant.
 * - `tenantId` almacena el identificador normalizado del tenant (ej: "casanovedades/").
 * - `companyName` almacena el nombre legible de la compañia que proporciona el tenant.
 * En `@PrePersist` intentamos asignar `tenantId` desde el TenantContext; si no existe
 * lo generamos a partir de `companyName` (normalizado: sin espacios, lowercase, con '/').
 */
@MappedSuperclass
public abstract class AbstractTenantEntity {

    // Eliminamos el campo `tenantId` string para usar una FK numérica `tenant_id`.
    // Seguimos almacenando el nombre legible de la compañía.
    @Column(name = "company_name")
    private String companyName; // ej: Casa Novedades (legible)

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    // Nota: la asignación de la relación `Tenant` se hace desde la lógica de negocio
    // (por ejemplo en `AuthController` al registrar) para evitar dependencias a repositorios
    // desde la entidad.

}
