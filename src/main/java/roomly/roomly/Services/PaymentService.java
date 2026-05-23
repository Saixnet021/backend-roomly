package roomly.roomly.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomly.roomly.DTO.PaymentDTO;
import roomly.roomly.Model.*;
import roomly.roomly.Repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InquilinoRepository inquilinoRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private static final Double DAILY_PENALTY_RATE = 0.01; // 1% diario

    /**
     * Crear un nuevo pago (ej: alquiler del mes)
     */
    public PaymentDTO createPayment(PaymentDTO dto, Tenant tenant) {
        Payment payment = new Payment();
        
        // Asignar inquilino
        if (dto.getInquilinoId() != null) {
            Optional<Inquilino> opt = inquilinoRepository.findById(dto.getInquilinoId());
            if (opt.isPresent()) {
                payment.setInquilino(opt.get());
            } else {
                throw new RuntimeException("Inquilino no encontrado");
            }
        }
        
        // Asignar cuarto si aplica
        if (dto.getRoomId() != null) {
            Optional<Room> opt = roomRepository.findById(dto.getRoomId());
            if (opt.isPresent()) {
                payment.setRoom(opt.get());
            }
        }
        
        payment.setAmount(dto.getAmount());
        payment.setAmountPaid(dto.getAmountPaid() != null ? dto.getAmountPaid() : 0.0);
        payment.setDueDate(dto.getDueDate());
        payment.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDIENTE");
        payment.setPaymentType(dto.getPaymentType() != null ? dto.getPaymentType() : "ALQUILER");
        payment.setDescription(dto.getDescription());
        payment.setTenant(tenant);
        
        Payment saved = paymentRepository.save(payment);
        return entityToDTO(saved);
    }

    /**
     * Registrar un pago (parcial o total)
     */
    public PaymentDTO recordPayment(Long paymentId, Double amount, String receiptRef) {
        Optional<Payment> opt = paymentRepository.findById(paymentId);
        if (!opt.isPresent()) {
            throw new RuntimeException("Pago no encontrado");
        }

        Payment payment = opt.get();
        
        // Calcular mora antes de registrar el pago
        payment.calculateDelayPenalty(DAILY_PENALTY_RATE);
        
        // Registrar el pago
        payment.recordPayment(amount);
        
        // Generar referencia de comprobante si no existe
        if (receiptRef == null || receiptRef.isEmpty()) {
            receiptRef = generateReceiptReference(paymentId);
        }
        payment.setReceiptReference(receiptRef);
        
        Payment updated = paymentRepository.save(payment);
        return entityToDTO(updated);
    }

    /**
     * Obtener todos los pagos de un inquilino
     */
    public List<PaymentDTO> getPaymentsByInquilino(Long inquilinoId) {
        Optional<Inquilino> opt = inquilinoRepository.findById(inquilinoId);
        if (!opt.isPresent()) {
            return List.of();
        }
        List<Payment> payments = paymentRepository.findByInquilinoOrderByDueDateDesc(opt.get());
        return payments.stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    /**
     * Obtener pagos vencidos
     */
    public List<PaymentDTO> getOverduePayments(Tenant tenant) {
        List<Payment> overdues = paymentRepository.findOverduePayments(tenant);
        return overdues.stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    /**
     * Obtener todos los pagos de un tenant (para dashboard del propietario)
     */
    public List<PaymentDTO> getAllPaymentsByTenant(Tenant tenant) {
        // Simulación: obtener todos los pagos del tenant
        // En producción podrías hacer una query más optimizada
        List<Payment> allPayments = paymentRepository.findByStatusAndTenant("PENDIENTE", tenant);
        // Agregar otros estados
        allPayments.addAll(paymentRepository.findByStatusAndTenant("PAGADO", tenant));
        allPayments.addAll(paymentRepository.findByStatusAndTenant("VENCIDO", tenant));
        allPayments.addAll(paymentRepository.findByStatusAndTenant("PAGADO_PARCIAL", tenant));
        
        return allPayments.stream().map(this::entityToDTO).collect(Collectors.toList());
    }

    /**
     * Obtener resumen de mora total
     */
    public Double getTotalDelayPenalty(Tenant tenant) {
        List<Payment> payments = paymentRepository.findOverduePayments(tenant);
        return payments.stream()
                .mapToDouble(p -> {
                    p.calculateDelayPenalty(DAILY_PENALTY_RATE);
                    return p.getDelayPenalty() != null ? p.getDelayPenalty() : 0.0;
                })
                .sum();
    }

    /**
     * Generar referencia de comprobante
     */
    private String generateReceiptReference(Long paymentId) {
        return "REC-" + paymentId + "-" + System.currentTimeMillis();
    }

    // Mapper
    private PaymentDTO entityToDTO(Payment p) {
        p.calculateDelayPenalty(DAILY_PENALTY_RATE);
        PaymentDTO dto = new PaymentDTO();
        dto.setId(p.getId());
        if (p.getInquilino() != null) {
            dto.setInquilinoId(p.getInquilino().getId());
            dto.setInquilinoName(p.getInquilino().getName());
        }
        if (p.getRoom() != null) {
            dto.setRoomId(p.getRoom().getId());
            dto.setRoomNumber(p.getRoom().getRoomNumber());
        }
        dto.setAmount(p.getAmount());
        dto.setAmountPaid(p.getAmountPaid());
        dto.setDelayPenalty(p.getDelayPenalty());
        dto.setTotalPending(p.getTotalPending());
        dto.setDueDate(p.getDueDate());
        dto.setLastPaymentDate(p.getLastPaymentDate());
        dto.setStatus(p.getStatus());
        dto.setPaymentType(p.getPaymentType());
        dto.setDescription(p.getDescription());
        dto.setReceiptReference(p.getReceiptReference());
        dto.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        dto.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return dto;
    }
}
