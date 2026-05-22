package roomly.roomly.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import roomly.roomly.DTO.PaymentDTO;
import roomly.roomly.Model.Tenant;
import roomly.roomly.Model.Usuario;
import roomly.roomly.Repository.UsuarioRepository;
import roomly.roomly.Repository.TenantRepository;
import roomly.roomly.Services.PaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    /**
     * Obtener el tenant actual del usuario autenticado
     */
    private Tenant getCurrentTenant() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            Usuario user = usuarioRepository.findFirstByEmailIgnoreCase(email).orElse(null);
            if (user != null) {
                return user.getTenant();
            }
        }
        return null;
    }

    /**
     * POST /api/payments - Crear nuevo pago (registro manual del propietario)
     */
    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody PaymentDTO dto) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        try {
            PaymentDTO created = paymentService.createPayment(dto, tenant);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/payments - Obtener todos los pagos del tenant
     */
    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        List<PaymentDTO> payments = paymentService.getAllPaymentsByTenant(tenant);
        return ResponseEntity.ok(payments);
    }

    /**
     * GET /api/payments/inquilino/{inquilinoId} - Obtener pagos de un inquilino específico
     */
    @GetMapping("/inquilino/{inquilinoId}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByInquilino(@PathVariable Long inquilinoId) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        List<PaymentDTO> payments = paymentService.getPaymentsByInquilino(inquilinoId);
        return ResponseEntity.ok(payments);
    }

    /**
     * GET /api/payments/overdue - Obtener pagos vencidos
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<PaymentDTO>> getOverduePayments() {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        List<PaymentDTO> overdues = paymentService.getOverduePayments(tenant);
        return ResponseEntity.ok(overdues);
    }

    /**
     * GET /api/payments/summary - Resumen de mora total
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getPaymentSummary() {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        Double totalDelay = paymentService.getTotalDelayPenalty(tenant);
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("totalDelayPenalty", totalDelay);
        }});
    }

    /**
     * PATCH /api/payments/{paymentId}/record - Registrar un pago (parcial o total)
     * Request body: { "amount": 1000.0, "receiptReference": "REC-123" }
     */
    @PatchMapping("/{paymentId}/record")
    public ResponseEntity<PaymentDTO> recordPayment(
            @PathVariable Long paymentId,
            @RequestBody java.util.Map<String, Object> body) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        try {
            Double amount = Double.parseDouble(body.get("amount").toString());
            String receiptRef = (String) body.get("receiptReference");
            
            PaymentDTO updated = paymentService.recordPayment(paymentId, amount, receiptRef);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/payments/{paymentId} - Obtener detalles de un pago específico
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long paymentId) {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(403).build();
        }
        // Aquí podrías recuperar el pago desde el servicio
        // Por ahora retornamos un placeholder
        return ResponseEntity.ok(new PaymentDTO());
    }
}
