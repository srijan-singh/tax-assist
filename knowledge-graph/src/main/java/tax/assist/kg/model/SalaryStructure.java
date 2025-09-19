package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Node
public class SalaryStructure {
    @Id
    @GeneratedValue
    private Long id;
    private String financialYear;
    private BigDecimal grossSalary;
    private BigDecimal totalExemptions = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Relationship(type = "HAS_COMPONENT", direction = Relationship.Direction.OUTGOING)
    private Set<SalaryComponent> components = new HashSet<>();

    public SalaryStructure(String financialYear, BigDecimal grossSalary) {
        this.financialYear = financialYear;
        this.grossSalary = grossSalary;
    }
}
