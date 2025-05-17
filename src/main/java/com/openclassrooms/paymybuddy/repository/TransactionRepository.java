package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Interface de repository Spring Data JPA pour l'entité {@link Transaction}.
 * Fournit des méthodes pour effectuer des opérations de persistance (CRUD)
 * sur les transactions stockées dans la base de données, ainsi que des méthodes
 * de recherche personnalisées.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * Recherche et retourne une liste de transactions où l'utilisateur spécifié
     * est soit l'expéditeur (sender) soit le destinataire (receiver).
     * Les transactions sont triées par leur date et heure (timestamp) en ordre décroissant (les plus récentes d'abord).
     *
     * @param sender L'utilisateur en tant qu'expéditeur potentiel.
     * @param receiver L'utilisateur en tant que destinataire potentiel (doit être le même que le sender pour cette requête spécifique).
     * @return Une liste de {@link Transaction} impliquant l'utilisateur, triée par date décroissante.
     */
    List<Transaction> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);

    /**
     * Recherche et retourne une liste de transactions où l'utilisateur spécifié
     * est l'expéditeur (sender).
     * Les transactions sont triées par leur date et heure (timestamp) en ordre décroissant (les plus récentes d'abord).
     *
     * @param sender L'utilisateur qui a initié les transactions.
     * @return Une liste des {@link Transaction} envoyées par l'utilisateur, triée par date décroissante.
     */
    List<Transaction> findBySenderOrderByTimestampDesc(User sender);

    /**
     * Recherche et retourne une liste de transactions où l'utilisateur spécifié
     * est le destinataire (receiver).
     * Les transactions sont triées par leur date et heure (timestamp) en ordre décroissant (les plus récentes d'abord).
     *
     * @param receiver L'utilisateur qui a reçu les transactions.
     * @return Une liste des {@link Transaction} reçues par l'utilisateur, triée par date décroissante.
     */
    List<Transaction> findByReceiverOrderByTimestampDesc(User receiver);
}