package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Node
public class TaxRegime {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String financialYear;
    private Boolean active;

    @Relationship(type = "HAS_SLAB", direction = Relationship.Direction.OUTGOING)
    private Set<TaxSlab> slabs = new HashSet<>();

    @Relationship(type = "ALLOWS_DEDUCTION", direction = Relationship.Direction.OUTGOING)
    private Set<Deduction> allowedDeductions = new HashSet<>();

    public TaxRegime(String name, String financialYear, Boolean active) {
        this.name = name;
        this.financialYear = financialYear;
        this.active = active;
    }
}
