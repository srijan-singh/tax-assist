package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.Form16;

@Repository
public interface Form16Repository extends Neo4jRepository<Form16, Long> {

    Form16 findByEmployeePANAndFinancialYear(String employeePAN, String financialYear);

    @Query("""
        MATCH (f:Form16 {employeePAN: $pan, financialYear: $fy})
        OPTIONAL MATCH (f)-[:CONTAINS_SALARY_STRUCTURE]->(ss:SalaryStructure)
        OPTIONAL MATCH (ss)-[:HAS_COMPONENT]->(sc:SalaryComponent)
        OPTIONAL MATCH (f)-[:HAS_TDS_ENTRY]->(tds:TDSEntry)
        OPTIONAL MATCH (f)-[:DECLARES_INVESTMENT]->(di:DeclaredInvestment)
        RETURN f, ss, collect(sc), collect(tds), collect(di)
    """)
    Form16 findCompleteForm16(String pan, String fy);
}
