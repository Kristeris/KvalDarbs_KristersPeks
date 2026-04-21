package lv.example.MarketPermitSystem.repo;
 
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lv.example.MarketPermitSystem.model.MyAuthority;
 
@Repository
public interface MyAuthorityRepository extends JpaRepository<MyAuthority, Long> {
    Optional<MyAuthority> findByTitle(String title);
}