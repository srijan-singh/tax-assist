package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.TaxScenario;

@Repository
public interface TaxScenarioRepository extends Neo4jRepository<TaxScenario, Long> {
}
