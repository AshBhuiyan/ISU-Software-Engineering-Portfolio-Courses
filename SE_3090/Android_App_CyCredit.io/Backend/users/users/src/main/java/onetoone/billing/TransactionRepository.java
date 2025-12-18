package onetoone.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
 List<Transaction> findByUser_IdOrderByTimestampDesc(Integer userId);
 java.util.Optional<Transaction> findByPurchaseNonce(String purchaseNonce);
}