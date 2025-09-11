package tax.assist.ingestion.model;

import lombok.Data;

import tax.assist.ingestion.sematics.TaxRuleChunk;

import java.util.List;

@Data
public class TaxRule {
    private String assessmentYear;
    private String taxRegime;
    private List<Slab> slabs;
    private Rebate rebate;
    private List<Surcharge> surcharge;
    private Cess cess;
    private List<Relief> reliefs;
    private List<TaxRuleChunk> chunks; // Added for embedding storage
}
