package roomly.roomly.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Payment: Representa un pago de alquiler o servicio.
 * Extiende AbstractTenantEntity para ser multitenant.
 * 
 * Estados: PENDIENTE, PAGADO, PAGADO_PARCIAL, VENCIDO, CANCELADO
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el inquilino
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id_fk", nullable = false)
    private Inquilino inquilino;

    // Relación con la propiedad/cuarto (para referencia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // Monto total del pago
    @Column(nullable = false)
    private Double amount;

    // Monto pagado hasta ahora
    @Column(nullable = false)
    private Double amountPaid = 0.0;

    // Mora acumulada
    @Column(nullable = false)
    private Double delayPenalty = 0.0;

    // Fecha de vencimiento del pago
    @Column(nullable = false)
    private LocalDate dueDate;

    // Fecha del último pago
    private LocalDate lastPaymentDate;

    // Estado: PENDIENTE, PAGADO, PAGADO_PARCIAL, VENCIDO, CANCELADO
    @Column(nullable = false)
    private String status = "PENDIENTE";

    // Tipo de pago: ALQUILER, SERVICIO
    @Column(nullable = false)
    private String paymentType = "ALQUILER";

    // Descripción o concepto del pago
    private String description;

    // Referencia de comprobante/recibo
    private String receiptReference;

    // Fecha de creación del registro
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Fecha de última actualización
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relación multitenant directa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_ref_id")
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (amountPaid == null) {
            amountPaid = 0.0;
        }
        if (delayPenalty == null) {
            delayPenalty = 0.0;
        }
        if (status == null) {
            status = "PENDIENTE";
        }
        if (paymentType == null) {
            paymentType = "ALQUILER";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Recalcular estado si es necesario
        if ("PENDIENTE".equals(status) && LocalDate.now().isAfter(dueDate)) {
            status = "VENCIDO";
        }
    }

    /**
     * Calcula la mora basándose en días vencidos y una tasa diaria
     * @param dailyPenaltyRate Tasa de mora diaria (ej: 0.01 para 1% diario)
     */
    public void calculateDelayPenalty(Double dailyPenaltyRate) {
        if ("PAGADO".equals(status)) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(dueDate)) {
            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
            if (daysLate > 5) {
                delayPenalty = (daysLate - 5) * 5.0;
            } else {
                delayPenalty = 0.0;
            }
        } else {
            delayPenalty = 0.0;
        }
    }

    /**
     * Registra un pago parcial o total
     * @param amountToAdd Monto a agregar
     */
    public void recordPayment(Double amountToAdd) {
        if (amountToAdd == null || amountToAdd <= 0) {
            return;
        }

        this.amountPaid = (this.amountPaid != null ? this.amountPaid : 0.0) + amountToAdd;
        this.lastPaymentDate = LocalDate.now();

        // Actualizar estado
        if (this.amountPaid >= this.amount) {
            this.status = "PAGADO";
            this.delayPenalty = 0.0;
        } else if (this.amountPaid > 0) {
            this.status = "PAGADO_PARCIAL";
        }
    }

    /**
     * Calcula el monto pendiente total (incluyendo mora)
     */
    public Double getTotalPending() {
        Double pending = (amount - amountPaid);
        return pending + (delayPenalty != null ? delayPenalty : 0.0);
    }
}
