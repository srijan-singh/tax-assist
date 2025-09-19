package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Data
@Node
public class TaxSuggestion {
    @Id
    @GeneratedValue
    private Long id;
    private String type;
    private String description;
    private BigDecimal currentAmount;
    private BigDecimal suggestedAmount;
    private BigDecimal potentialSaving;
    private Priority priority;
    private String deadline;
}
