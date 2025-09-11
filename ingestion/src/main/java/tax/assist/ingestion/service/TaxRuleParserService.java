package tax.assist.ingestion.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import tax.assist.ingestion.extractor.PdfExtractor;
import tax.assist.ingestion.model.Cess;
import tax.assist.ingestion.model.Rebate;
import tax.assist.ingestion.model.Relief;
import tax.assist.ingestion.model.Slab;
import tax.assist.ingestion.model.Surcharge;
import tax.assist.ingestion.model.TaxRule;
import tax.assist.ingestion.sematics.TaxRuleChunk;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TaxRuleParserService {
    
    @Autowired
    private ChatClient chatClient;
    
    private static final int MAX_CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    public TaxRule ingestITR(File pdfFile) throws Exception {
        log.info("Starting ITR ingestion for file: {}", pdfFile.getName());
        
        // Extract text from PDF
        String text = PdfExtractor.extractText(pdfFile);
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("No text could be extracted from the PDF file");
        }
        
        log.info("Extracted {} characters from PDF", text.length());
        
        // Parse structured tax rule data
        TaxRule taxRule = parseStructuredTaxRule(text);
        
        // Create chunks for vector storage
        List<TaxRuleChunk> chunks = createChunks(text, taxRule);
        taxRule.setChunks(chunks);
        
        log.info("Created {} chunks for tax rule", chunks.size());
        return taxRule;
    }
    
    private TaxRule parseStructuredTaxRule(String text) {
        try {
            String prompt = createStructureExtractionPrompt(text);
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            return parseResponseToTaxRule(response);
            
        } catch (Exception e) {
            log.error("Error parsing structured tax rule: {}", e.getMessage());
            // Return a basic tax rule if parsing fails
            return createFallbackTaxRule();
        }
    }
    
    private String createStructureExtractionPrompt(String text) {
        String limitedText = text.length() > 4000 ? text.substring(0, 4000) + "..." : text;
        
        return """
            Extract tax rule information from the following text and format it as JSON.
            
            Text: %s
            
            Please extract:
            1. Assessment Year (e.g., "2024-25")
            2. Tax Regime (e.g., "Old", "New")
            3. Tax Slabs with rates and ranges
            4. Rebate information (section, amount, conditions)
            5. Surcharge details (rates, income thresholds)
            6. Cess information (rate, description)
            7. Any relief provisions
            
            Return ONLY valid JSON in this exact format:
            {
                "assessmentYear": "string",
                "taxRegime": "string",
                "slabs": [{"fromAmount": 0, "toAmount": 250000, "rate": 0}],
                "rebate": {"section": "87A", "maxAmount": 12500, "conditions": "Income up to 5 lakhs"},
                "surcharge": [{"rate": 10, "threshold": 5000000, "description": "For income above 50 lakhs"}],
                "cess": {"rate": 4, "description": "Health and Education Cess"},
                "reliefs": [{"section": "80C", "description": "Investment deduction", "maxAmount": 150000}]
            }
            """.formatted(limitedText);
    }
    
    private TaxRule parseResponseToTaxRule(String response) {
        try {
            // Clean the response to extract only JSON
            String jsonResponse = extractJsonFromResponse(response);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonResponse);
            
            TaxRule taxRule = new TaxRule();
            taxRule.setAssessmentYear(jsonNode.path("assessmentYear").asText("Unknown"));
            taxRule.setTaxRegime(jsonNode.path("taxRegime").asText("Unknown"));
            
            // Parse slabs
            List<Slab> slabs = parseSlabs(jsonNode.path("slabs"));
            taxRule.setSlabs(slabs);
            
            // Parse rebate
            Rebate rebate = parseRebate(jsonNode.path("rebate"));
            taxRule.setRebate(rebate);
            
            // Parse surcharge
            List<Surcharge> surcharges = parseSurcharges(jsonNode.path("surcharge"));
            taxRule.setSurcharge(surcharges);
            
            // Parse cess
            Cess cess = parseCess(jsonNode.path("cess"));
            taxRule.setCess(cess);
            
            // Parse reliefs
            List<Relief> reliefs = parseReliefs(jsonNode.path("reliefs"));
            taxRule.setReliefs(reliefs);
            
            return taxRule;
            
        } catch (Exception e) {
            log.error("Error parsing JSON response: {}", e.getMessage());
            return createFallbackTaxRule();
        }
    }
    
    private String extractJsonFromResponse(String response) {
        // Find JSON block between curly braces
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        return response;
    }
    
    private List<Slab> parseSlabs(JsonNode slabsNode) {
        List<Slab> slabs = new ArrayList<>();
        if (slabsNode.isArray()) {
            for (JsonNode slabNode : slabsNode) {
                Slab slab = new Slab();
                slab.setFromAmount(slabNode.path("fromAmount").asDouble(0));
                slab.setToAmount(slabNode.path("toAmount").asDouble(0));
                slab.setRate(slabNode.path("rate").asDouble(0));
                slab.setDescription(slabNode.path("description").asText(""));
                slabs.add(slab);
            }
        }
        return slabs;
    }
    
    private Rebate parseRebate(JsonNode rebateNode) {
        if (rebateNode.isMissingNode()) return null;
        
        Rebate rebate = new Rebate();
        rebate.setSection(rebateNode.path("section").asText(""));
        rebate.setMaxAmount(rebateNode.path("maxAmount").asDouble(0));
        rebate.setConditions(rebateNode.path("conditions").asText(""));
        return rebate;
    }
    
    private List<Surcharge> parseSurcharges(JsonNode surchargeNode) {
        List<Surcharge> surcharges = new ArrayList<>();
        if (surchargeNode.isArray()) {
            for (JsonNode sNode : surchargeNode) {
                Surcharge surcharge = new Surcharge();
                surcharge.setRate(sNode.path("rate").asDouble(0));
                surcharge.setThreshold(sNode.path("threshold").asDouble(0));
                surcharge.setDescription(sNode.path("description").asText(""));
                surcharges.add(surcharge);
            }
        }
        return surcharges;
    }
    
    private Cess parseCess(JsonNode cessNode) {
        if (cessNode.isMissingNode()) return null;
        
        Cess cess = new Cess();
        cess.setRate(cessNode.path("rate").asDouble(0));
        cess.setDescription(cessNode.path("description").asText(""));
        return cess;
    }
    
    private List<Relief> parseReliefs(JsonNode reliefsNode) {
        List<Relief> reliefs = new ArrayList<>();
        if (reliefsNode.isArray()) {
            for (JsonNode reliefNode : reliefsNode) {
                Relief relief = new Relief();
                relief.setSection(reliefNode.path("section").asText(""));
                relief.setDescription(reliefNode.path("description").asText(""));
                relief.setMaxAmount(reliefNode.path("maxAmount").asDouble(0));
                reliefs.add(relief);
            }
        }
        return reliefs;
    }
    
    private TaxRule createFallbackTaxRule() {
        TaxRule fallback = new TaxRule();
        fallback.setAssessmentYear("Unknown");
        fallback.setTaxRegime("Unknown");
        fallback.setSlabs(new ArrayList<>());
        fallback.setSurcharge(new ArrayList<>());
        fallback.setReliefs(new ArrayList<>());
        return fallback;
    }
    
    private List<TaxRuleChunk> createChunks(String text, TaxRule taxRule) {
        List<TaxRuleChunk> chunks = new ArrayList<>();
        
        // Split text into manageable chunks
        List<String> textChunks = splitTextIntoChunks(text);
        
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);
            
            // Enhance chunk with AI
            String enhancedContent = enhanceChunkContent(chunkText, taxRule);
            
            TaxRuleChunk chunk = new TaxRuleChunk();
            chunk.setId(UUID.randomUUID().toString());
            chunk.setContent(enhancedContent);
            chunk.setOriginalText(chunkText);
            chunk.setChunkIndex(i);
            chunk.setAssessmentYear(taxRule.getAssessmentYear());
            chunk.setTaxRegime(taxRule.getTaxRegime());
            chunk.setMetadata(createChunkMetadata(chunkText, taxRule));
            
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    private List<String> splitTextIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        
        // Split by paragraphs first
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > MAX_CHUNK_SIZE) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    
                    // Add overlap from the end of previous chunk
                    String overlap = getOverlapText(currentChunk.toString(), CHUNK_OVERLAP);
                    currentChunk = new StringBuilder(overlap);
                }
            }
            
            currentChunk.append(paragraph).append("\n\n");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        
        // Find a good breaking point (end of sentence)
        String lastPart = text.substring(text.length() - overlapSize);
        int sentenceEnd = lastPart.indexOf('.');
        
        if (sentenceEnd != -1) {
            return lastPart.substring(sentenceEnd + 1).trim();
        }
        
        return lastPart;
    }
    
    private String enhanceChunkContent(String chunkText, TaxRule taxRule) {
        try {
            String prompt = """
                Enhance the following tax rule text chunk for better semantic search. Add context and keywords while maintaining all specific information.
                
                Assessment Year: %s
                Tax Regime: %s
                
                Original text:
                %s
                
                Please:
                1. Add relevant tax-related keywords and context
                2. Structure the information clearly
                3. Preserve all numbers, rates, and legal references exactly
                4. Make it more searchable while keeping the original meaning
                5. Keep the enhanced text concise and focused
                
                Enhanced text:
                """.formatted(
                    taxRule.getAssessmentYear(), 
                    taxRule.getTaxRegime(), 
                    chunkText.length() > 500 ? chunkText.substring(0, 500) + "..." : chunkText
                );
            
            String enhanced = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            return enhanced != null && !enhanced.trim().isEmpty() ? enhanced : chunkText;
            
        } catch (Exception e) {
            log.warn("Failed to enhance chunk content: {}", e.getMessage());
            return chunkText;
        }
    }
    
    private Map<String, Object> createChunkMetadata(String chunkText, TaxRule taxRule) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("assessmentYear", taxRule.getAssessmentYear());
        metadata.put("taxRegime", taxRule.getTaxRegime());
        metadata.put("length", chunkText.length());
        metadata.put("timestamp", Instant.now().toString());
        
        // Add topic classification based on keywords
        String lowerText = chunkText.toLowerCase();
        if (lowerText.contains("slab") || lowerText.contains("rate") || lowerText.contains("bracket")) {
            metadata.put("topic", "tax_slabs");
        } else if (lowerText.contains("rebate") || lowerText.contains("87a")) {
            metadata.put("topic", "rebate");
        } else if (lowerText.contains("surcharge")) {
            metadata.put("topic", "surcharge");
        } else if (lowerText.contains("cess")) {
            metadata.put("topic", "cess");
        } else if (lowerText.contains("80c") || lowerText.contains("80d") || lowerText.contains("deduction")) {
            metadata.put("topic", "deductions");
        } else {
            metadata.put("topic", "general");
        }
        
        return metadata;
    }
}
