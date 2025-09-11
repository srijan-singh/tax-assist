package tax.assist.ingestion.service;

import org.springframework.stereotype.Service;
import tax.assist.ingestion.extractor.PdfExtractor;
import tax.assist.ingestion.model.TaxRule;

import java.io.File;

@Service
public class TaxRuleParserService {
    
    public TaxRule ingestITR(File pdfFile) throws Exception {
        String text = PdfExtractor.extractText(pdfFile);
        // TODO: Use Ollama to create chunks from text to ingest in VectorDB
        return null;
    }
}
