package roomly.roomly.DTO;

import lombok.Data;

@Data
public class RoomDTO {
    private Long id;
    private String roomNumber;
    private Double price;
    private String status;
    private Long propertyId;
}
