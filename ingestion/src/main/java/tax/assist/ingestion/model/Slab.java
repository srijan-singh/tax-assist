package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Slab {
    private double fromAmount;
    private double toAmount;
    private double rate;
    private String description;
}
