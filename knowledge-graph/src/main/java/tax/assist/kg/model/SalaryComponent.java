package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Data
@Node
public class SalaryComponent {
    @Id
    @GeneratedValue
    private Long id;
    private SalaryComponentType type;
    private String description;
    private BigDecimal amount;
    private String exemptionRule;
    private BigDecimal exemptAmount = BigDecimal.ZERO;

    public SalaryComponent(SalaryComponentType type, String description, BigDecimal amount) {
        this.type = type;
        this.description = description;
        this.amount = amount;
    }
}
