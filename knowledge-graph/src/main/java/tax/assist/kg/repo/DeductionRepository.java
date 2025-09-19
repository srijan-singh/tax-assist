package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.Deduction;

import java.util.List;

@Repository
public interface DeductionRepository extends Neo4jRepository<Deduction, Long> {

    List<Deduction> findByApplicableRegime(String regime);
}
