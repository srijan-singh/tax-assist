package tax.assist.kg.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import tax.assist.kg.model.Form16;

@Repository
public interface Form16Repository extends Neo4jRepository<Form16, Long> {

    Form16 findByEmployeePANAndFinancialYear(String pan, String fy);
}
