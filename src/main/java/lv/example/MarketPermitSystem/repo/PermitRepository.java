package lv.example.MarketPermitSystem.repo;
 
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
 
@Repository
public interface PermitRepository extends JpaRepository<Permit, Long> {

    // Pagination lietotāja pieteikumiem
    Page<Permit> findByUserOrderBySubmittedAtDesc(MyUser user, Pageable pageable);
 
    // Pagination admin skatam
    Page<Permit> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    // Bez pagination (saglabātas esošās metodes statistikai)
    List<Permit> findByUserOrderBySubmittedAtDesc(MyUser user);
    List<Permit> findAllByOrderBySubmittedAtDesc();
    List<Permit> findByStatus(PermitStatus status);
    long countByStatus(PermitStatus status);
}
 