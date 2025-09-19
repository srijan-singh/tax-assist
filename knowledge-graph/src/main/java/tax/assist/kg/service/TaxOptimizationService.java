package tax.assist.kg.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tax.assist.kg.model.*;
import tax.assist.kg.repo.DeductionRepository;
import tax.assist.kg.repo.Form16Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaxOptimizationService {

    @Autowired
    private DeductionRepository deductionRepository;

    @Autowired
    private Form16Repository form16Repository;

    public List<TaxSuggestion> generateOptimizationSuggestions(String pan, String financialYear) {
        Form16 form16 = form16Repository.findByEmployeePANAndFinancialYear(pan, financialYear);
        if (form16 == null) {
            throw new IllegalArgumentException("Form16 not found");
        }

        List<TaxSuggestion> suggestions = new ArrayList<>();
        List<Deduction> availableDeductions = deductionRepository.findByApplicableRegime("OLD");
        Map<String, BigDecimal> currentInvestments = form16.getDeclaredInvestments().stream()
                .collect(Collectors.toMap(
                        DeclaredInvestment::getSection,
                        DeclaredInvestment::getAmount,
                        (existing, replacement) -> existing
                ));

        // Check each available deduction
        for (Deduction deduction : availableDeductions) {
            BigDecimal currentAmount = currentInvestments.getOrDefault(deduction.getSection(), BigDecimal.ZERO);
            BigDecimal remainingLimit = deduction.getMaxLimit().subtract(currentAmount);

            if (remainingLimit.compareTo(BigDecimal.ZERO) > 0) {
                TaxSuggestion suggestion = createSuggestion(deduction, currentAmount, remainingLimit);
                suggestions.add(suggestion);
            }
        }

        // Add HRA optimization if applicable
        TaxSuggestion hraOptimization = checkHRAOptimization(form16);
        if (hraOptimization != null) {
            suggestions.add(hraOptimization);
        }

        return suggestions.stream()
                .sorted((s1, s2) -> s2.getPotentialSaving().compareTo(s1.getPotentialSaving()))
                .toList();
    }

    private TaxSuggestion createSuggestion(Deduction deduction, BigDecimal currentAmount, BigDecimal remainingLimit) {
        BigDecimal potentialSaving = remainingLimit.multiply(new BigDecimal("0.30")); // Assuming 30% tax bracket

        TaxSuggestion suggestion = new TaxSuggestion(
                "ADDITIONAL_" + deduction.getSection() + "_INVESTMENT",
                "Invest additional ₹" + remainingLimit + " in " + deduction.getDescription() + " to save tax",
                currentAmount,
                currentAmount.add(remainingLimit),
                potentialSaving,
                determinePriority(potentialSaving)
        );

        if ("80C".equals(deduction.getSection())) {
            suggestion.setDeadline("March 31");
        }
        return suggestion;
    }

    private Priority determinePriority(BigDecimal potentialSaving) {
        if (potentialSaving.compareTo(new BigDecimal("15000")) >= 0) {
            return Priority.HIGH;
        } else if (potentialSaving.compareTo(new BigDecimal("5000")) >= 0) {
            return Priority.MEDIUM;
        } else {
            return Priority.LOW;
        }
    }

    private TaxSuggestion checkHRAOptimization(Form16 form16) {
        if (form16.getSalaryStructure() == null) {
            return null;
        }

        Optional<SalaryComponent> hraComponent = form16.getSalaryStructure().getComponents().stream()
                .filter(component -> component.getType() == SalaryComponentType.HRA)
                .findFirst();

        if (hraComponent.isPresent()) {
            SalaryComponent hra = hraComponent.get();
            if (hra.getExemptAmount().compareTo(hra.getAmount()) < 0) {
                BigDecimal additionalExemption = hra.getAmount().subtract(hra.getExemptAmount());
                return new TaxSuggestion(
                        "HRA_OPTIMIZATION",
                        "Submit rent receipts to claim additional HRA exemption",
                        hra.getExemptAmount(),
                        hra.getAmount(),
                        additionalExemption.multiply(new BigDecimal("0.30")),
                        Priority.HIGH
                );
            }
        }
        return null;
    }
}

