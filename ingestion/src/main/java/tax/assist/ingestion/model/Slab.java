package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Slab {
    private String range;
    private String rate;
    private String category;
}
