package tax.assist.kg.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tax.assist.kg.model.Form16;
import tax.assist.kg.model.SalaryComponent;
import tax.assist.kg.model.SalaryStructure;
import tax.assist.kg.model.TaxProfile;
import tax.assist.kg.repo.Form16Repository;
import tax.assist.kg.repo.TaxProfileRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class Form16Service {

    @Autowired
    private Form16Repository form16Repository;

    @Autowired
    private TaxProfileRepository taxProfileRepository;

    public Form16 saveForm16(Form16 form16) {
        // Link to tax profile
        TaxProfile profile = taxProfileRepository.findByPan(form16.getEmployeePAN());
        if (profile == null) {
            throw new IllegalArgumentException("Tax profile not found for PAN: " + form16.getEmployeePAN());
        }

        profile.getForm16Documents().add(form16);
        taxProfileRepository.save(profile);

        return form16Repository.save(form16);
    }

    public Form16 getForm16ByPanAndYear(String pan, String financialYear) {
        return form16Repository.findByEmployeePANAndFinancialYear(pan, financialYear);
    }

    public List<String> validateForm16Data(Form16 form16) {
        List<String> errors = new ArrayList<>();

        if (form16.getEmployeePAN() == null || form16.getEmployeePAN().isBlank()) {
            errors.add("Employee PAN is required");
        }

        if (form16.getEmployerTAN() == null || form16.getEmployerTAN().isBlank()) {
            errors.add("Employer TAN is required");
        }

        SalaryStructure salaryStructure = form16.getSalaryStructure();
        if (salaryStructure == null) {
            errors.add("Salary structure is required");
        } else {
            if (salaryStructure.getGrossSalary().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Gross salary must be greater than zero");
            }

            BigDecimal totalComponents = salaryStructure.getComponents().stream()
                    .map(SalaryComponent::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalComponents.compareTo(salaryStructure.getGrossSalary()) != 0) {
                errors.add("Sum of salary components doesn't match gross salary");
            }
        }
        return errors;
    }
}
