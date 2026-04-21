package lv.example.MarketPermitSystem.repo;
 
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
 
@Repository
public interface PermitRepository extends JpaRepository<Permit, Long> {
    List<Permit> findByUserOrderBySubmittedAtDesc(MyUser user);
    List<Permit> findAllByOrderBySubmittedAtDesc();
    List<Permit> findByStatus(PermitStatus status);
    long countByStatus(PermitStatus status);
}
 