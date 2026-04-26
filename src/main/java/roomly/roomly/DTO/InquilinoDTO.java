package roomly.roomly.DTO;

import lombok.Data;

@Data
public class InquilinoDTO {
    private Long id;
    private String name;
    private String document;
    private String email;
    private String phone;
    private String status;
    private Long propertyId;
    private String propertyName; // Useful for UI
    private Long roomId;
    private String roomNumber; // Useful for UI
    private String password; // Solo para creación de cuenta
}
