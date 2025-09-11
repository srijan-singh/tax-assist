package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Cess {
    private double rate;
    private String description;
    private String applicableOn; // e.g., "Tax + Surcharge"
}
