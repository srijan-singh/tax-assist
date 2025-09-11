package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Rebate {
    private String section;
    private double maxAmount;
    private String conditions;
    private double incomeThreshold;
}
