package tax.assist.kg.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@Node
public class TaxProfile {
    @Id
    @GeneratedValue
    private Long id;
    private String pan;
    private String name;
    private Integer age;
    private String category = "GENERAL"; // GENERAL, SC/ST, OBC
    private String residentialStatus = "RESIDENT";

    @Relationship(type = "HAS_FORM16", direction = Relationship.Direction.OUTGOING)
    private Set<Form16> form16Documents = new HashSet<>();

    @Relationship(type = "HAS_INCOME_FROM", direction = Relationship.Direction.OUTGOING)
    private Set<IncomeHead> incomeHeads = new HashSet<>();

    @Relationship(type = "HAS_TAX_SCENARIO", direction = Relationship.Direction.OUTGOING)
    private Set<TaxScenario> taxScenarios = new HashSet<>();

    public TaxProfile(String pan, String name, Integer age) {
        this.pan = pan;
        this.name = name;
        this.age = age;
    }
}
