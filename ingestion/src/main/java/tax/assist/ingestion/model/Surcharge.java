package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Surcharge {
    private double rate;
    private double threshold;
    private String description;
    private String applicableFor;
}
