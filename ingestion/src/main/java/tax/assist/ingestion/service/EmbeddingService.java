package tax.assist.ingestion.service;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    @Autowired
    private TaxRuleParserService parserService;

    @Autowired
    private OllamaEmbeddingModel embeddingModel;

    public void ingestITR() throws Exception {
        // TODO: Use parseService to get chunks from text to ingest in VectorDB
    }

    
}

