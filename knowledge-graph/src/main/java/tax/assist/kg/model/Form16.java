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
public class Form16 {
    @Id
    @GeneratedValue
    private Long id;
    private String employerTAN;
    private String employeePAN;
    private String financialYear;
    private String assessmentYear;
    private String employerName;
    private String employeeName;

    @Relationship(type = "CONTAINS_SALARY_STRUCTURE", direction = Relationship.Direction.OUTGOING)
    private SalaryStructure salaryStructure;

    @Relationship(type = "HAS_TDS_ENTRY", direction = Relationship.Direction.OUTGOING)
    private Set<TDSEntry> tdsEntries = new HashSet<>();

    @Relationship(type = "DECLARES_INVESTMENT", direction = Relationship.Direction.OUTGOING)
    private Set<DeclaredInvestment> declaredInvestments = new HashSet<>();
}
