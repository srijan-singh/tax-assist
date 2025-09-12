package tax.assist.ingestion.service;

import java.io.File;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tax.assist.ingestion.model.TaxRule;

/**
 * Service to handle the embedding and ingestion of tax rule documents into the Qdrant vector store.
 */
@Service
@Slf4j
public class EmbeddingService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private TaxRuleParserService taxRuleParserService;

    /**
     * Ingests a mock tax rule document by converting its chunks into Spring AI Documents
     * and storing them in the vector store.
     * @throws Exception 
     */
    public void ingestTaxRules(File pdfFile) throws Exception {
        log.info("Starting tax rules ingestion process...");
        TaxRule taxRule = taxRuleParserService.ingestITR(pdfFile);

        List<Document> documents = taxRule.getChunks().stream()
                .map(chunk -> {
                    // Create a new Document for each TaxRuleChunk
                    Document document = new Document(chunk.getContent());
                    
                    // Populate the metadata with all relevant information for powerful filtering
                    document.getMetadata().put("id", chunk.getId());
                    document.getMetadata().put("chunkIndex", chunk.getChunkIndex());
                    document.getMetadata().put("assessmentYear", chunk.getAssessmentYear());
                    document.getMetadata().put("taxRegime", chunk.getTaxRegime());
                    
                    // Merge chunk-specific metadata
                    if (chunk.getMetadata() != null) {
                        chunk.getMetadata().forEach(document.getMetadata()::put);
                    }

                    log.info("Creating document with metadata: {}", document.getMetadata());
                    return document;
                })
                .toList();

        log.info("Ingesting {} documents into the vector store...", documents.size());
        vectorStore.add(documents);
        log.info("Tax rules ingestion completed successfully.");
    }
}
