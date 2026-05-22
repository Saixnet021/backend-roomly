package roomly.roomly.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import roomly.roomly.Model.Payment;
import roomly.roomly.Model.Tenant;
import roomly.roomly.Model.Inquilino;
import roomly.roomly.Model.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Buscar pagos de un inquilino específico ordenados por fecha de vencimiento
     */
    List<Payment> findByInquilinoOrderByDueDateDesc(Inquilino inquilino);
    
    /**
     * Buscar pagos por estado
     */
    List<Payment> findByStatusAndTenant(String status, Tenant tenant);
    
    /**
     * Buscar pagos vencidos de un tenant
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN ('PENDIENTE', 'VENCIDO', 'PAGADO_PARCIAL') " +
           "AND p.dueDate < CURRENT_DATE AND p.tenant = :tenant ORDER BY p.dueDate ASC")
    List<Payment> findOverduePayments(@Param("tenant") Tenant tenant);
    
    /**
     * Buscar pagos por rango de fechas
     */
    List<Payment> findByDueDateBetweenAndTenant(LocalDate startDate, LocalDate endDate, Tenant tenant);
    
    /**
     * Buscar pagos por inquilino y estado
     */
    List<Payment> findByInquilinoAndStatus(Inquilino inquilino, String status);
    
    /**
     * Buscar pagos por cuarto
     */
    List<Payment> findByRoomOrderByDueDateDesc(Room room);
    
    /**
     * Buscar pagos por tipo
     */
    List<Payment> findByPaymentTypeAndTenant(String paymentType, Tenant tenant);
    
    /**
     * Contar pagos pendientes de un tenant
     */
    Long countByStatusAndTenant(String status, Tenant tenant);
}
