package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.TaxRegime;

import java.util.List;

@Repository
public interface TaxRegimeRepository extends Neo4jRepository<TaxRegime, Long> {

    List<TaxRegime> findByFinancialYearAndActive(String financialYear, Boolean active);

    TaxRegime findByNameAndFinancialYear(String name, String financialYear);

    @Query("""
        MATCH (tr:TaxRegime {financialYear: $financialYear, active: true})
        OPTIONAL MATCH (tr)-[:HAS_SLAB]->(ts:TaxSlab)
        OPTIONAL MATCH (tr)-[:ALLOWS_DEDUCTION]->(d:Deduction)
        RETURN tr, collect(ts), collect(d)
    """)
    List<TaxRegime> findActiveRegimesWithDetailsForYear(String financialYear);
}
