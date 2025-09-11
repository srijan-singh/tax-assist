package tax.assist.ingestion.model;

import lombok.Data;

@Data
public class Rebate {
    private String section;
    private String eligibility;
    private double amount;
}
