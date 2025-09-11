package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Relief {
    private String section;
    private String description;
    private double maxAmount;
    private String conditions;
    private String category; // e.g., "80C", "80D", etc.
}
