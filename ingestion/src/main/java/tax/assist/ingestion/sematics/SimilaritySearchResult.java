package tax.assist.ingestion.sematics;

import java.util.Map;

import lombok.Data;

@Data
public class SimilaritySearchResult {
    private String id;
    private float score;
    private String content;
    private String originalText;
    private String assessmentYear;
    private String taxRegime;
    private Integer chunkIndex;
    private String topic;
    private Map<String, Object> metadata;
}
