package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Data
@Node
public class Deduction {
    @Id
    @GeneratedValue
    private Long id;
    private String section;
    private String description;
    private BigDecimal maxLimit;
    private DeductionType type;
    private String applicableRegime; // "OLD", "NEW", "BOTH"

    public Deduction(String section, String description, BigDecimal maxLimit,
                     DeductionType type, String applicableRegime) {
        this.section = section;
        this.description = description;
        this.maxLimit = maxLimit;
        this.type = type;
        this.applicableRegime = applicableRegime;
    }
}
