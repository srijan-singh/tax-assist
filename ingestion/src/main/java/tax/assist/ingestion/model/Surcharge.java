package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Surcharge {
    private String threshold;
    private String rate;
}
