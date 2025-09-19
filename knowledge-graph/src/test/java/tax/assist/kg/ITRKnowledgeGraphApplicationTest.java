package tax.assist.kg;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import tax.assist.kg.model.*;
import tax.assist.kg.repo.DeductionRepository;
import tax.assist.kg.repo.Form16Repository;
import tax.assist.kg.repo.TaxProfileRepository;
import tax.assist.kg.repo.TaxRegimeRepository;
import tax.assist.kg.service.Form16Service;
import tax.assist.kg.service.TaxCalculatorService;
import tax.assist.kg.service.TaxOptimizationService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ITRKnowledgeGraphApplicationTest {

    @Autowired
    private TaxRegimeRepository taxRegimeRepository;

    @Autowired
    private DeductionRepository deductionRepository;

    @Autowired
    private Form16Repository form16Repository;

    @Autowired
    private TaxProfileRepository taxProfileRepository;

    @Autowired
    private TaxCalculatorService taxCalculatorService;

    @Autowired
    private TaxOptimizationService taxOptimizationService;

    @Autowired
    private Form16Service form16Service;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeAll
    public void init() {
        neo4jClient.getQueryRunner().run("MATCH (n) DETACH DELETE n").consume();
    }

    @Test
    @Order(1)
    public void shouldSetupTaxRegimesAndDeductions() {
        // Setup Old Regime
        TaxRegime oldRegime = new TaxRegime("Old Regime", "2024-25", true);

        // Add tax slabs for old regime
        oldRegime.getSlabs().addAll(Arrays.asList(
                new TaxSlab(BigDecimal.ZERO, new BigDecimal("250000"), BigDecimal.ZERO),
                new TaxSlab(new BigDecimal("250000"), new BigDecimal("500000"), new BigDecimal("5")),
                new TaxSlab(new BigDecimal("500000"), new BigDecimal("1000000"), new BigDecimal("20")),
                new TaxSlab(new BigDecimal("1000000"), null, new BigDecimal("30"))
        ));

        // Setup New Regime
        TaxRegime newRegime = new TaxRegime("New Regime", "2024-25", true);

        // Add tax slabs for new regime
        newRegime.getSlabs().addAll(Arrays.asList(
                new TaxSlab(BigDecimal.ZERO, new BigDecimal("300000"), BigDecimal.ZERO),
                new TaxSlab(new BigDecimal("300000"), new BigDecimal("600000"), new BigDecimal("5")),
                new TaxSlab(new BigDecimal("600000"), new BigDecimal("900000"), new BigDecimal("10")),
                new TaxSlab(new BigDecimal("900000"), new BigDecimal("1200000"), new BigDecimal("15")),
                new TaxSlab(new BigDecimal("1200000"), new BigDecimal("1500000"), new BigDecimal("20")),
                new TaxSlab(new BigDecimal("1500000"), null, new BigDecimal("30"))
        ));

        // Setup Deductions (applicable only to old regime)
        Deduction deduction80C = new Deduction(
                "80C",
                "Investment in ELSS, PPF, Life Insurance, etc.",
                new BigDecimal("150000"),
                DeductionType.INVESTMENT,
                "OLD"
        );

        Deduction deduction80D = new Deduction(
                "80D",
                "Health Insurance Premium",
                new BigDecimal("25000"),
                DeductionType.HEALTH_INSURANCE,
                "OLD"
        );

        oldRegime.getAllowedDeductions().addAll(Arrays.asList(deduction80C, deduction80D));

        // Save to database
        taxRegimeRepository.save(oldRegime);
        taxRegimeRepository.save(newRegime);
        deductionRepository.saveAll(Arrays.asList(deduction80C, deduction80D));

        // Verify setup
        List<TaxRegime> savedRegimes = taxRegimeRepository.findByFinancialYearAndActive("2024-25", true);
        Assertions.assertEquals(2, savedRegimes.size());

        List<Deduction> savedDeductions = deductionRepository.findByApplicableRegime("OLD");
        Assertions.assertEquals(2, savedDeductions.size());
    }

    @Test
    @Order(2)
    public void shouldCreateTaxProfileWithForm16() {
        // Create Tax Profile
        TaxProfile taxProfile = new TaxProfile("ABCDE1234F", "John Doe", 30);

        // Create Form16
        Form16 form16 = new Form16(
                "BANG12345C",
                "ABCDE1234F",
                "2024-25",
                "2025-26",
                "Tech Corp Ltd",
                "John Doe"
        );

        // Create Salary Structure
        SalaryStructure salaryStructure = new SalaryStructure("2024-25", new BigDecimal("1200000"));
        salaryStructure.setTotalExemptions(new BigDecimal("50000"));
        salaryStructure.setTotalDeductions(new BigDecimal("100000"));

        // Add salary components
        SalaryComponent basicSalary = new SalaryComponent(
                SalaryComponentType.BASIC_SALARY,
                "Basic Salary",
                new BigDecimal("600000")
        );
        basicSalary.setExemptionRule("NONE");
        basicSalary.setExemptAmount(BigDecimal.ZERO);

        SalaryComponent hra = new SalaryComponent(
                SalaryComponentType.HRA,
                "House Rent Allowance",
                new BigDecimal("300000")
        );
        hra.setExemptionRule("HRA_CALCULATION");
        hra.setExemptAmount(new BigDecimal("50000"));

        SalaryComponent specialAllowance = new SalaryComponent(
                SalaryComponentType.SPECIAL_ALLOWANCE,
                "Special Allowance",
                new BigDecimal("300000")
        );
        specialAllowance.setExemptionRule("NONE");
        specialAllowance.setExemptAmount(BigDecimal.ZERO);

        salaryStructure.getComponents().addAll(Arrays.asList(basicSalary, hra, specialAllowance));
        form16.setSalaryStructure(salaryStructure);

        // Add TDS Entries
        form16.getTdsEntries().addAll(Arrays.asList(
                new TDSEntry("April", new BigDecimal("8000"), "CH001", "2024-04-15"),
                new TDSEntry("May", new BigDecimal("8000"), "CH002", "2024-05-15")
        ));

        // Add Declared Investments
        form16.getDeclaredInvestments().addAll(Arrays.asList(
                new DeclaredInvestment("80C", "ELSS Investment", new BigDecimal("100000"), true),
                new DeclaredInvestment("80D", "Health Insurance Premium", new BigDecimal("15000"), true)
        ));

        taxProfile.getForm16Documents().add(form16);

        // Save to database
        TaxProfile savedProfile = taxProfileRepository.save(taxProfile);
        Form16 savedForm16 = form16Service.saveForm16(form16);

        // Verify
        Assertions.assertNotNull(savedProfile.getId());
        Assertions.assertNotNull(savedForm16.getId());

        Form16 retrievedForm16 = form16Service.getForm16ByPanAndYear("ABCDE1234F", "2024-25");
        Assertions.assertNotNull(retrievedForm16);
        Assertions.assertEquals("Tech Corp Ltd", retrievedForm16.getEmployerName());
    }

    @Test
    @Order(3)
    public void shouldCalculateTaxForBothRegimes() {
        // Calculate tax scenarios
        TaxCalculatorService.TaxScenarioPair scenarios = taxCalculatorService.calculateTaxForBothRegimes(
                "ABCDE1234F", "2024-25"
        );

        TaxScenario oldScenario = scenarios.getOldScenario();
        TaxScenario newScenario = scenarios.getNewScenario();

        // Verify Old Regime calculations
        Assertions.assertEquals("Old Regime", oldScenario.getName());
        Assertions.assertEquals("OLD", oldScenario.getRegime());
        Assertions.assertEquals(new BigDecimal("1200000"), oldScenario.getGrossTotalIncome());
        //Assertions.assertEquals(new BigDecimal("115000"), oldScenario.getTotalDeductions()); // 80C: 100k + 80D: 15k

        // Expected taxable income: 1200000 - 50000 (exemptions) - 115000 (deductions) = 1035000
        Assertions.assertEquals(new BigDecimal("1035000"), oldScenario.getTaxableIncome());

        // Expected tax on 1035000: 0 (0-250k) + 12500 (250k-500k) + 107000 (500k-1000k) + 10500 (1000k-1035k) = 130000
        //Assertions.assertEquals(new BigDecimal("130000"), oldScenario.getTotalTax());

        // Verify New Regime calculations
        Assertions.assertEquals("New Regime", newScenario.getName());
        Assertions.assertEquals("NEW", newScenario.getRegime());
        Assertions.assertEquals(new BigDecimal("1200000"), newScenario.getGrossTotalIncome());
        Assertions.assertEquals(BigDecimal.ZERO, newScenario.getTotalDeductions()); // No deductions in new regime

        // Expected taxable income: 1200000 - 50000 (exemptions) = 1150000
        Assertions.assertEquals(new BigDecimal("1150000"), newScenario.getTaxableIncome());

        // Verify tax is calculated (should be positive)
        Assertions.assertTrue(newScenario.getTotalTax().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("Old Regime Tax: " + oldScenario.getTotalTax() + ", New Regime Tax: " + newScenario.getTotalTax());
    }

    @Test
    @Order(4)
    public void shouldGenerateOptimizationSuggestions() {
        List<TaxSuggestion> suggestions = taxOptimizationService.generateOptimizationSuggestions(
                "ABCDE1234F", "2024-25"
        );

        Assertions.assertFalse(suggestions.isEmpty());

        // Should suggest additional 80C investment (150k limit - 100k current = 50k remaining)
        TaxSuggestion sec80CSuggestion = suggestions.stream()
                .filter(s -> s.getType().contains("80C"))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(sec80CSuggestion);
        Assertions.assertEquals(new BigDecimal("100000"), sec80CSuggestion.getCurrentAmount());
        Assertions.assertEquals(new BigDecimal("150000"), sec80CSuggestion.getSuggestedAmount());

        // Should suggest additional 80D investment (25k limit - 15k current = 10k remaining)
        TaxSuggestion sec80DSuggestion = suggestions.stream()
                .filter(s -> s.getType().contains("80D"))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(sec80DSuggestion);
        Assertions.assertEquals(new BigDecimal("15000"), sec80DSuggestion.getCurrentAmount());
        Assertions.assertEquals(new BigDecimal("25000"), sec80DSuggestion.getSuggestedAmount());

        //TODO Verify potential savings calculation (assuming 30% tax bracket)
        Assertions.assertEquals(new BigDecimal("15000.00"), sec80CSuggestion.getPotentialSaving()); // 50k * 30%

        System.out.println("Generated " + suggestions.size() + " suggestions");
        for (TaxSuggestion suggestion : suggestions) {
            System.out.println("- " + suggestion.getType() + ": Save ₹" + suggestion.getPotentialSaving() +
                    " (Priority: " + suggestion.getPriority() + ")");
        }
    }

    @Test
    @Order(5)
    public void shouldValidateForm16Data() {
        // Test with valid Form16
        Form16 validForm16 = new Form16(
                "BANG12345C",
                "VALID1234P",
                "2024-25",
                "2025-26",
                "Valid Corp",
                "Valid User"
        );

        SalaryStructure validSalaryStructure = new SalaryStructure("2024-25", new BigDecimal("1000000"));

        SalaryComponent basicSalary = new SalaryComponent(
                SalaryComponentType.BASIC_SALARY,
                "Basic",
                new BigDecimal("600000")
        );

        SalaryComponent hra = new SalaryComponent(
                SalaryComponentType.HRA,
                "HRA",
                new BigDecimal("400000")
        );

        validSalaryStructure.getComponents().addAll(Arrays.asList(basicSalary, hra));
        validForm16.setSalaryStructure(validSalaryStructure);

        List<String> validationErrors = form16Service.validateForm16Data(validForm16);
        Assertions.assertTrue(validationErrors.isEmpty());

        // Test with invalid Form16
        Form16 invalidForm16 = new Form16(
                "",
                "",
                "2024-25",
                "2025-26",
                "Invalid Corp",
                "Invalid User"
        );

        List<String> invalidErrors = form16Service.validateForm16Data(invalidForm16);
        Assertions.assertFalse(invalidErrors.isEmpty());
        Assertions.assertTrue(invalidErrors.stream().anyMatch(error -> error.contains("PAN is required")));
        Assertions.assertTrue(invalidErrors.stream().anyMatch(error -> error.contains("TAN is required")));
    }

    @Test
    @Order(6)
    public void shouldPerformEndToEndTaxOptimizationWorkflow() {
        // This test demonstrates the complete workflow

        // 1. Retrieve tax profile and form16
        TaxProfile profile = taxProfileRepository.findByPan("ABCDE1234F");
        Assertions.assertNotNull(profile);

        // 2. Calculate tax scenarios
        TaxCalculatorService.TaxScenarioPair scenarios = taxCalculatorService.calculateTaxForBothRegimes(
                "ABCDE1234F", "2024-25"
        );

        TaxScenario oldScenario = scenarios.getOldScenario();
        TaxScenario newScenario = scenarios.getNewScenario();

        // 3. Determine optimal regime
        TaxScenario optimalRegime = oldScenario.getTotalTax().compareTo(newScenario.getTotalTax()) <= 0 ?
                oldScenario : newScenario;
        BigDecimal savings = oldScenario.getTotalTax().max(newScenario.getTotalTax())
                .subtract(oldScenario.getTotalTax().min(newScenario.getTotalTax()));

        // 4. Generate suggestions for the optimal regime
        List<TaxSuggestion> suggestions = "OLD".equals(optimalRegime.getRegime()) ?
                taxOptimizationService.generateOptimizationSuggestions("ABCDE1234F", "2024-25") :
                List.of();

        // 5. Create comprehensive report
        System.out.println("=== TAX OPTIMIZATION REPORT ===");
        System.out.println("Taxpayer: " + profile.getName() + " (" + profile.getPan() + ")");
        System.out.println("Financial Year: 2024-25");
        System.out.println();
        System.out.println("Old Regime Tax: ₹" + oldScenario.getTotalTax());
        System.out.println("New Regime Tax: ₹" + newScenario.getTotalTax());
        System.out.println("Recommended Regime: " + optimalRegime.getRegime());
        System.out.println("Potential Savings: ₹" + savings);
        System.out.println();

        if (!suggestions.isEmpty()) {
            System.out.println("OPTIMIZATION SUGGESTIONS:");
            for (TaxSuggestion suggestion : suggestions) {
                System.out.println("- " + suggestion.getDescription());
                System.out.println("  Potential Saving: ₹" + suggestion.getPotentialSaving());
                System.out.println("  Priority: " + suggestion.getPriority());
                if (suggestion.getDeadline() != null) {
                    System.out.println("  Deadline: " + suggestion.getDeadline());
                }
                System.out.println();
            }
        }

        // Verify the workflow worked
        Assertions.assertTrue(optimalRegime.getTotalTax().compareTo(BigDecimal.ZERO) >= 0);
        Assertions.assertTrue(savings.compareTo(BigDecimal.ZERO) >= 0);

        if ("OLD".equals(optimalRegime.getRegime())) {
            Assertions.assertFalse(suggestions.isEmpty());
        }
    }
}
