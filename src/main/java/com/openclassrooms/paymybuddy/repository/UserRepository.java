package com.openclassrooms.paymybuddy.repository;


import com.openclassrooms.paymybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    // Pour récupérer un utilisateur avec ses connexions pré-chargées
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.connections WHERE u.email = :email")
    Optional<User> findByEmailWithConnections(String email);

    // Pour récupérer un utilisateur avec ses transactions pré-chargées
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.sentTransactions st " +
            "LEFT JOIN FETCH u.receivedTransactions rt " +
            "LEFT JOIN FETCH st.receiver " + // Charger le destinataire de chaque transaction envoyée
            "LEFT JOIN FETCH rt.sender " + // Charger l'expéditeur de chaque transaction reçue
            "WHERE u.email = :email")
    Optional<User> findByEmailWithTransactions(String email);
}