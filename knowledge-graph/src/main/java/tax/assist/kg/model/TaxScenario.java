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
public class TaxScenario {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String regime; // OLD, NEW
    private BigDecimal grossTotalIncome;
    private BigDecimal totalDeductions;
    private BigDecimal taxableIncome;
    private BigDecimal totalTax;
    private BigDecimal effectiveRate;

    @Relationship(type = "SUGGESTS", direction = Relationship.Direction.OUTGOING)
    private Set<TaxSuggestion> suggestions = new HashSet<>();
}
