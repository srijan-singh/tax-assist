package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Data
@Node
public class TDSEntry {
    @Id
    @GeneratedValue
    private Long id;
    private String month;
    private BigDecimal amount;
    private String challanNo;
    private String dateOfDeposit;
}
