package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;

@Data
@Node
public class TaxSlab {
    @Id
    @GeneratedValue
    private Long id;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private BigDecimal taxRate;

    public TaxSlab(BigDecimal minIncome, BigDecimal maxIncome, BigDecimal taxRate) {
        this.minIncome = minIncome;
        this.maxIncome = maxIncome;
        this.taxRate = taxRate;
    }
}
