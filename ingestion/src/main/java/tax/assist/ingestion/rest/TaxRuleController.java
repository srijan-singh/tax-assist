package tax.assist.ingestion.rest;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tax.assist.ingestion.model.TaxRule;
import tax.assist.ingestion.service.TaxRuleParserService;

import java.io.File;

@RestController
@RequestMapping("/tax/return")
public class TaxRuleController {
    private final TaxRuleParserService parserService;

    public TaxRuleController(TaxRuleParserService parserService) {
        this.parserService = parserService;
    }

    @PostMapping("/ingest")
    public TaxRule parseITR(@RequestParam("file") MultipartFile file) throws Exception {
        File temp = File.createTempFile("itr", ".pdf");
        file.transferTo(temp);
        return parserService.ingestITR(temp);
    }
}

