package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.Deduction;
import tax.assist.kg.model.DeductionType;

import java.util.List;

@Repository
public interface DeductionRepository extends Neo4jRepository<Deduction, Long> {

    Deduction findBySection(String section);

    List<Deduction> findByApplicableRegimeInAndType(List<String> applicableRegime, DeductionType type);

    @Query("""
        MATCH (d:Deduction) 
        WHERE d.applicableRegime = $regime OR d.applicableRegime = 'BOTH'
        RETURN d ORDER BY d.section
    """)
    List<Deduction> findByApplicableRegime(String regime);
}
