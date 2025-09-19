package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.TaxScenario;

import java.util.List;

@Repository
public interface TaxScenarioRepository extends Neo4jRepository<TaxScenario, Long> {

    @Query("""
        MATCH (tp:TaxProfile {pan: $pan})-[:HAS_TAX_SCENARIO]->(ts:TaxScenario)
        OPTIONAL MATCH (ts)-[:SUGGESTS]->(suggestions:TaxSuggestion)
        RETURN ts, collect(suggestions)
        ORDER BY ts.totalTax ASC
    """)
    List<TaxScenario> findOptimalScenariosForUser(String pan);
}
