package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.TaxProfile;

@Repository
public interface TaxProfileRepository extends Neo4jRepository<TaxProfile, Long> {

    TaxProfile findByPan(String pan);
}
