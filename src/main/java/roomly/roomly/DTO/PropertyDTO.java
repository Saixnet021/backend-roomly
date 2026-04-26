package roomly.roomly.DTO;

import lombok.Data;

@Data
public class PropertyDTO {
    private Long id;
    private String name;
    private String address;
    private Double price;
    private long roomCount;
    private long occupiedRooms;
    private double totalIncome;
}
