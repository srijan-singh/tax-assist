package tax.assist.kg.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tax.assist.kg.model.*;
import tax.assist.kg.repo.Form16Repository;
import tax.assist.kg.repo.TaxRegimeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TaxCalculatorService {

    @Autowired
    private TaxRegimeRepository taxRegimeRepository;

    @Autowired
    private Form16Repository form16Repository;

    public TaxScenarioPair calculateTaxForBothRegimes(String pan, String financialYear) {
        Form16 form16 = form16Repository.findByEmployeePANAndFinancialYear(pan, financialYear);
        if (form16 == null) {
            throw new IllegalArgumentException("Form16 not found for PAN: " + pan);
        }

        List<TaxRegime> regimes = taxRegimeRepository.findByFinancialYear(financialYear);
        TaxRegime oldRegime = regimes.stream()
                .filter(regime -> "Old Regime".equals(regime.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Old regime not found"));

        TaxRegime newRegime = regimes.stream()
                .filter(regime -> "New Regime".equals(regime.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("New regime not found"));

        BigDecimal grossIncome = form16.getSalaryStructure() != null ?
                form16.getSalaryStructure().getGrossSalary() : BigDecimal.ZERO;
        BigDecimal totalExemptions = calculateExemptions(form16);

        // Old Regime Calculation
        BigDecimal oldRegimeDeductions = calculateOldRegimeDeductions(form16, oldRegime);
        BigDecimal oldTaxableIncome = grossIncome.subtract(totalExemptions).subtract(oldRegimeDeductions);
        BigDecimal oldTax = calculateTaxFromSlabs(oldTaxableIncome, oldRegime.getSlabs());

        TaxScenario oldScenario = new TaxScenario(
                "Old Regime",
                "OLD",
                grossIncome,
                oldRegimeDeductions,
                oldTaxableIncome,
                oldTax,
                grossIncome.compareTo(BigDecimal.ZERO) > 0 ?
                        oldTax.divide(grossIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO
        );

        // New Regime Calculation
        BigDecimal newTaxableIncome = grossIncome.subtract(totalExemptions); // No deductions in new regime
        BigDecimal newTax = calculateTaxFromSlabs(newTaxableIncome, newRegime.getSlabs());

        TaxScenario newScenario = new TaxScenario(
                "New Regime",
                "NEW",
                grossIncome,
                BigDecimal.ZERO,
                newTaxableIncome,
                newTax,
                grossIncome.compareTo(BigDecimal.ZERO) > 0 ?
                        newTax.divide(grossIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO
        );

        return new TaxScenarioPair(oldScenario, newScenario);
    }

    private BigDecimal calculateExemptions(Form16 form16) {
        if (form16.getSalaryStructure() == null) {
            return BigDecimal.ZERO;
        }

        return form16.getSalaryStructure().getComponents().stream()
                .map(SalaryComponent::getExemptAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOldRegimeDeductions(Form16 form16, TaxRegime regime) {
        Set<String> allowedSections = regime.getAllowedDeductions().stream()
                .map(Deduction::getSection)
                .collect(java.util.stream.Collectors.toSet());

        return form16.getDeclaredInvestments().stream()
                .filter(investment -> allowedSections.contains(investment.getSection()))
                .map(DeclaredInvestment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTaxFromSlabs(BigDecimal taxableIncome, Set<TaxSlab> slabs) {
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal remainingIncome = taxableIncome;

        List<TaxSlab> sortedSlabs = slabs.stream()
                .sorted(Comparator.comparing(TaxSlab::getMinIncome))
                .toList();

        for (TaxSlab slab : sortedSlabs) {
            if (remainingIncome.compareTo(BigDecimal.ZERO) > 0 &&
                    taxableIncome.compareTo(slab.getMinIncome()) > 0) {

                BigDecimal taxableAtThisSlab;
                if (slab.getMaxIncome() != null) {
                    taxableAtThisSlab = remainingIncome.min(slab.getMaxIncome().subtract(slab.getMinIncome()));
                } else {
                    taxableAtThisSlab = remainingIncome;
                }

                if (taxableAtThisSlab.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal slabTax = taxableAtThisSlab
                            .multiply(slab.getTaxRate())
                            .divide(new BigDecimal("100"));
                    tax = tax.add(slabTax);
                    remainingIncome = remainingIncome.subtract(taxableAtThisSlab);
                }
            }
        }

        return tax.setScale(0, RoundingMode.HALF_UP);
    }

    // Helper class to return pair of scenarios
    @Getter
    public static class TaxScenarioPair {
        private final TaxScenario oldScenario;
        private final TaxScenario newScenario;

        public TaxScenarioPair(TaxScenario oldScenario, TaxScenario newScenario) {
            this.oldScenario = oldScenario;
            this.newScenario = newScenario;
        }
    }
}

