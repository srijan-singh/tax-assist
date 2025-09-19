package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Data
@Node
public class DeclaredInvestment {
    @Id
    @GeneratedValue
    private Long id;
    private String section;
    private String description;
    private BigDecimal amount;
    private Boolean proofSubmitted = false;

}
