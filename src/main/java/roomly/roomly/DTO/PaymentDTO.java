package roomly.roomly.DTO;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PaymentDTO {
    private Long id;
    private Long inquilinoId;
    private String inquilinoName;
    private Long roomId;
    private String roomNumber;
    private Double amount;
    private Double amountPaid;
    private Double delayPenalty;
    private Double totalPending;
    private LocalDate dueDate;
    private LocalDate lastPaymentDate;
    private String status; // PENDIENTE, PAGADO, PAGADO_PARCIAL, VENCIDO, CANCELADO
    private String paymentType; // ALQUILER, SERVICIO
    private String description;
    private String receiptReference;
    private String createdAt;
    private String updatedAt;
}
