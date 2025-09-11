package tax.assist.ingestion.sematics;

import java.util.Map;

import lombok.Data;

@Data
public class TaxRuleChunk {
    private String id;
    private String content;
    private String originalText;
    private int chunkIndex;
    private String assessmentYear;
    private String taxRegime;
    private Map<String, Object> metadata;
}
