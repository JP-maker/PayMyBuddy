package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Trouve les transactions où l'utilisateur est soit l'expéditeur soit le destinataire
    List<Transaction> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);

    // Ou séparément si besoin :
    List<Transaction> findBySenderOrderByTimestampDesc(User sender);
    List<Transaction> findByReceiverOrderByTimestampDesc(User receiver);
}