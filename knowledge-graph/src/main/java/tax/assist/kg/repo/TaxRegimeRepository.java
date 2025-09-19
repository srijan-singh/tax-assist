package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.TaxRegime;

import java.util.List;

@Repository
public interface TaxRegimeRepository extends Neo4jRepository<TaxRegime, Long> {

    List<TaxRegime> findByFinancialYearAndActive(String financialYear, Boolean active);

    List<TaxRegime> findByFinancialYear(String financialYear);
}
