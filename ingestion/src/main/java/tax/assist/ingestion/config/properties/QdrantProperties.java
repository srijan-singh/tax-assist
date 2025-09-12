package tax.assist.ingestion.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "spring.ai.vectorstore.qdrant")
@Data
public class QdrantProperties {
    private String host = "localhost";
    private int port = 6334;
    private boolean useTls = false;
    private String apiKey;
    private String collectionName = "tax-rules";
    private int dimension = 1536;
}
