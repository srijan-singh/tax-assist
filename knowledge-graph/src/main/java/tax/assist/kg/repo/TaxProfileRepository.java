package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.TaxProfile;

@Repository
public interface TaxProfileRepository extends Neo4jRepository<TaxProfile, Long> {

    TaxProfile findByPan(String pan);

    @Query("""
        MATCH (tp:TaxProfile {pan: $pan})
        OPTIONAL MATCH (tp)-[:HAS_FORM16]->(f:Form16 {financialYear: $fy})
        OPTIONAL MATCH (tp)-[:HAS_INCOME_FROM]->(ih:IncomeHead)
        OPTIONAL MATCH (tp)-[:HAS_TAX_SCENARIO]->(ts:TaxScenario)
        OPTIONAL MATCH (ts)-[:SUGGESTS]->(suggestions:TaxSuggestion)
        RETURN tp, f, collect(ih), collect(ts), collect(suggestions)
    """)
    TaxProfile findProfileWithDetails(String pan, String fy);
}
