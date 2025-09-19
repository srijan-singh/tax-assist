package tax.assist.kg.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tax.assist.kg.model.*;
import tax.assist.kg.repo.Form16Repository;
import tax.assist.kg.repo.TaxRegimeRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class TaxCalculatorServiceTest {

    @Mock
    private TaxRegimeRepository taxRegimeRepository;

    @Mock
    private Form16Repository form16Repository;

    @InjectMocks
    private TaxCalculatorService taxCalculatorService;

    @Test
    public void shouldCalculateTaxCorrectlyForSingleSlab() {
        // Given
        Form16 form16 = createSampleForm16WithSalary(new BigDecimal("400000"));
        TaxRegime oldRegime = createOldRegimeWithSlabs();
        TaxRegime newRegime = createNewRegimeWithSlabs();

        when(form16Repository.findByEmployeePANAndFinancialYear("TEST123", "2024-25")).thenReturn(form16);
        when(taxRegimeRepository.findByFinancialYear("2024-25"))
                .thenReturn(Arrays.asList(oldRegime, newRegime));

        // When
        TaxCalculatorService.TaxScenarioPair scenarios = taxCalculatorService.calculateTaxForBothRegimes("TEST123", "2024-25");

        // Then - For income of 400k, both regimes should have minimal tax
        Assertions.assertTrue(scenarios.getOldScenario().getTotalTax().compareTo(new BigDecimal("10000")) <= 0);
        Assertions.assertTrue(scenarios.getNewScenario().getTotalTax().compareTo(new BigDecimal("5000")) <= 0);
    }

    @Test
    public void shouldHandleHighIncomeTaxCalculation() {
        // Given
        Form16 form16 = createSampleForm16WithSalary(new BigDecimal("2000000"));
        TaxRegime oldRegime = createOldRegimeWithSlabs();
        TaxRegime newRegime = createNewRegimeWithSlabs();

        when(form16Repository.findByEmployeePANAndFinancialYear("TEST123", "2024-25")).thenReturn(form16);
        when(taxRegimeRepository.findByFinancialYear("2024-25"))
                .thenReturn(Arrays.asList(oldRegime, newRegime));

        // When
        TaxCalculatorService.TaxScenarioPair scenarios = taxCalculatorService.calculateTaxForBothRegimes("TEST123", "2024-25");

        // Then - High income should have substantial tax difference
        Assertions.assertTrue(scenarios.getOldScenario().getTotalTax().compareTo(new BigDecimal("100000")) > 0);
        Assertions.assertTrue(scenarios.getNewScenario().getTotalTax().compareTo(new BigDecimal("100000")) > 0);
        Assertions.assertNotEquals(scenarios.getOldScenario().getTotalTax(), scenarios.getNewScenario().getTotalTax());
    }

    private Form16 createSampleForm16WithSalary(BigDecimal grossSalary) {
        Form16 form16 = new Form16(
                "TEST12345T",
                "TEST123",
                "2024-25",
                "2025-26",
                "Test Corp",
                "Test User"
        );

        SalaryStructure salaryStructure = new SalaryStructure("2024-25", grossSalary);
        salaryStructure.setTotalExemptions(BigDecimal.ZERO);
        salaryStructure.setTotalDeductions(BigDecimal.ZERO);

        SalaryComponent basicSalary = new SalaryComponent(
                SalaryComponentType.BASIC_SALARY,
                "Basic Salary",
                grossSalary
        );
        basicSalary.setExemptAmount(BigDecimal.ZERO);

        salaryStructure.getComponents().add(basicSalary);
        form16.setSalaryStructure(salaryStructure);
        return form16;
    }

    private TaxRegime createOldRegimeWithSlabs() {
        TaxRegime regime = new TaxRegime("Old Regime", "2024-25", true);
        Set<TaxSlab> slabs = new HashSet<>();
        slabs.add(new TaxSlab(BigDecimal.ZERO, new BigDecimal("250000"), BigDecimal.ZERO));
        slabs.add(new TaxSlab(new BigDecimal("250000"), new BigDecimal("500000"), new BigDecimal("5")));
        slabs.add(new TaxSlab(new BigDecimal("500000"), new BigDecimal("1000000"), new BigDecimal("20")));
        slabs.add(new TaxSlab(new BigDecimal("1000000"), null, new BigDecimal("30")));
        regime.setSlabs(slabs);
        return regime;
    }

    private TaxRegime createNewRegimeWithSlabs() {
        TaxRegime regime = new TaxRegime("New Regime", "2024-25", true);
        Set<TaxSlab> slabs = new HashSet<>();
        slabs.add(new TaxSlab(BigDecimal.ZERO, new BigDecimal("300000"), BigDecimal.ZERO));
        slabs.add(new TaxSlab(new BigDecimal("300000"), new BigDecimal("600000"), new BigDecimal("5")));
        slabs.add(new TaxSlab(new BigDecimal("600000"), new BigDecimal("900000"), new BigDecimal("10")));
        slabs.add(new TaxSlab(new BigDecimal("900000"), new BigDecimal("1200000"), new BigDecimal("15")));
        slabs.add(new TaxSlab(new BigDecimal("1200000"), new BigDecimal("1500000"), new BigDecimal("20")));
        slabs.add(new TaxSlab(new BigDecimal("1500000"), null, new BigDecimal("30")));
        regime.setSlabs(slabs);
        return regime;
    }
}
