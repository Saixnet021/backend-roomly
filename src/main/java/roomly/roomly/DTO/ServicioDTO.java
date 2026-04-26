package roomly.roomly.DTO;

import lombok.Data;

@Data
public class ServicioDTO {
    private Long id;
    private String name;
    private String description;
    private Double cost;
    private String tipo;
    private Long propertyId;
    private String propertyName;
    private Long roomId;
    private String roomNumber;
}
