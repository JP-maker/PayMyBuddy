package com.openclassrooms.paymybuddy.repository;


import com.openclassrooms.paymybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;


/**
 * Interface de repository Spring Data JPA pour l'entité {@link User}.
 * Fournit des méthodes pour effectuer des opérations de persistance (CRUD)
 * sur les utilisateurs stockés dans la base de données, ainsi que des méthodes
 * de recherche personnalisées.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Recherche et retourne un {@link Optional} contenant l'utilisateur correspondant
     * à l'adresse e-mail fournie.
     *
     * @param email L'adresse e-mail de l'utilisateur à rechercher.
     * @return Un {@link Optional} contenant l'{@link User} trouvé, ou {@link Optional#empty()}
     *         si aucun utilisateur ne correspond à cet e-mail.
     */
    Optional<User> findByEmail(String email);

    /**
     * Recherche un utilisateur par son adresse e-mail et charge de manière anticipée (eagerly fetches)
     * sa liste de connexions (amis).
     * Cette méthode utilise une requête JPQL avec `LEFT JOIN FETCH` pour s'assurer que la collection
     * `connections` de l'utilisateur est initialisée lors de la récupération de l'entité User,
     * évitant ainsi des problèmes de type `LazyInitializationException` si les connexions
     * sont accédées en dehors d'une session transactionnelle active.
     *
     * @param email L'adresse e-mail de l'utilisateur à rechercher.
     * @return Un {@link Optional} contenant l'{@link User} trouvé avec sa liste de connexions initialisée,
     *         ou {@link Optional#empty()} si aucun utilisateur ne correspond à cet e-mail.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.connections WHERE u.email = :email")
    Optional<User> findByEmailWithConnections(String email);

    /**
     * Recherche un utilisateur par son adresse e-mail et charge de manière anticipée (eagerly fetches)
     * ses transactions envoyées (`sentTransactions`) et reçues (`receivedTransactions`).
     * De plus, pour chaque transaction (envoyée ou reçue), les informations de l'autre utilisateur
     * impliqué (destinataire pour les transactions envoyées, expéditeur pour les transactions reçues)
     * sont également chargées via `JOIN FETCH`.
     * Cette méthode est utile pour afficher un historique complet des transactions sans
     * déclencher de requêtes supplémentaires pour les entités associées (évite les `LazyInitializationException`).
     *
     * @param email L'adresse e-mail de l'utilisateur à rechercher.
     * @return Un {@link Optional} contenant l'{@link User} trouvé avec ses listes de transactions
     *         (envoyées et reçues) et les utilisateurs associés à ces transactions initialisés,
     *         ou {@link Optional#empty()} si aucun utilisateur ne correspond à cet e-mail.
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.sentTransactions st " +
            "LEFT JOIN FETCH u.receivedTransactions rt " +
            "LEFT JOIN FETCH st.receiver " + // Charger le destinataire de chaque transaction envoyée
            "LEFT JOIN FETCH rt.sender " + // Charger l'expéditeur de chaque transaction reçue
            "WHERE u.email = :email")
    Optional<User> findByEmailWithTransactions(String email);
}