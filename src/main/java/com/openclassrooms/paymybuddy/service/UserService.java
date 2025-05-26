package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.dto.UserRegistrationDto;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service métier pour la gestion des utilisateurs.
 * Ce service encapsule la logique liée aux opérations sur les utilisateurs,
 * telles que l'inscription, la recherche, l'ajout de connexions (amis),
 * la mise à jour du profil et le changement de mot de passe.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construit une instance de {@code UserService} avec le repository utilisateur et l'encodeur de mot de passe.
     *
     * @param userRepository  Le repository pour accéder aux données des utilisateurs.
     * @param passwordEncoder L'encodeur pour hacher les mots de passe des utilisateurs.
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Enregistre un nouvel utilisateur dans le système.
     * Vérifie si un utilisateur avec le même e-mail existe déjà. Si c'est le cas, une exception est levée.
     * Sinon, un nouvel utilisateur est créé avec les informations fournies, son mot de passe est haché,
     * et il est sauvegardé en base de données. L'opération est transactionnelle.
     *
     * @param registrationDto Le DTO {@link UserRegistrationDto} contenant les informations d'inscription.
     *                        L'annotation {@code @Valid} est indicative ; la validation est généralement
     *                        gérée par le contrôleur avant l'appel à ce service.
     * @return L'entité {@link User} persistée.
     * @throws Exception Si un compte existe déjà avec l'e-mail fourni.
     */
    @Transactional
    public User registerNewUser(@Valid UserRegistrationDto registrationDto) throws Exception {
        log.info("Tentative d'inscription pour l'email : {}", registrationDto.getEmail());
        if (userRepository.findByEmail(registrationDto.getEmail()).isPresent()) {
            log.warn("Tentative d'inscription avec un email déjà utilisé : {}", registrationDto.getEmail());
            throw new Exception("Un compte existe déjà avec cet email : " + registrationDto.getEmail());
        }
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setUsername(registrationDto.getUsername()); // Peut être null ou vide si non fourni
        // Le solde (balance) est déjà à 0 par défaut dans l'entité
        User userTx = userRepository.save(user);
        log.info("Nouvel utilisateur enregistré : {}", userTx.getEmail());
        return userTx;
    }

    /**
     * Recherche un utilisateur par son adresse e-mail.
     * Si l'e-mail est nul ou vide, retourne un {@link Optional#empty()}.
     * Si aucun utilisateur n'est trouvé pour l'e-mail donné, une {@link RuntimeException} est levée.
     *
     * @param email L'adresse e-mail de l'utilisateur à rechercher.
     * @return Un {@link Optional} contenant l'{@link User} trouvé.
     * @throws RuntimeException si aucun utilisateur n'est trouvé avec l'e-mail fourni (après la vérification de null/vide).
     */
    public Optional<User> findByEmail(String email) {
        log.info("Recherche de l'utilisateur avec l'email : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour la recherche.");
            return Optional.empty();
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
        log.info("Utilisateur trouvé : {}", user.getEmail());
        return Optional.of(user);
    }

    /**
     * Recherche un utilisateur par son adresse e-mail et charge de manière anticipée ses connexions (amis).
     * Si l'e-mail est nul ou vide, retourne un {@link Optional#empty()}.
     * Si aucun utilisateur n'est trouvé, une {@link RuntimeException} est levée.
     *
     * @param email L'adresse e-mail de l'utilisateur à rechercher.
     * @return Un {@link Optional} contenant l'{@link User} trouvé avec ses connexions initialisées.
     * @throws RuntimeException si aucun utilisateur n'est trouvé avec l'e-mail fourni.
     */
    public Optional<User> findByEmailWithConnections(String email) {
        log.info("Recherche de l'utilisateur avec l'email et ses connexions : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour la recherche.");
            return Optional.empty();
        }
        User user = userRepository.findByEmailWithConnections(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
        log.info("Utilisateur trouvé avec connexions : {}", user.getEmail());
        return Optional.of(user);
    }

    /**
     * Recherche un utilisateur par son adresse e-mail et charge de manière anticipée ses transactions.
     * Si l'e-mail est nul ou vide, retourne un {@link Optional#empty()}.
     * Si aucun utilisateur n'est trouvé, une {@link RuntimeException} est levée.
     *
     * @param email L'adresse e-mail de l'utilisateur à rechercher.
     * @return Un {@link Optional} contenant l'{@link User} trouvé avec ses transactions initialisées.
     * @throws RuntimeException si aucun utilisateur n'est trouvé avec l'e-mail fourni.
     */
    public Optional<User> findByEmailWithTransactions(String email) {
        log.info("Recherche de l'utilisateur avec l'email et ses transactions : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour la recherche.");
            return Optional.empty();
        }
        User user = userRepository.findByEmailWithTransactions(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
        log.info("Utilisateur trouvé avec transactions : {}", user.getEmail());
        return Optional.of(user);
    }

    /**
     * Ajoute une connexion (ami) à un utilisateur.
     * L'opération est transactionnelle. Vérifie que l'utilisateur ne s'ajoute pas lui-même,
     * que les deux utilisateurs existent, et qu'ils ne sont pas déjà connectés.
     *
     * @param userEmail L'e-mail de l'utilisateur qui initie l'ajout de connexion.
     * @param friendEmail L'e-mail de l'utilisateur à ajouter comme connexion.
     * @throws Exception Si l'un des e-mails est invalide, si l'utilisateur tente de s'ajouter lui-même,
     *                   si l'un des utilisateurs n'est pas trouvé, ou si la connexion existe déjà.
     */
    @Transactional
    public void addConnection(String userEmail, String friendEmail) throws Exception {
        log.info("Ajout d'une connexion entre {} et {}", userEmail, friendEmail);
        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("Email de l'utilisateur courant vide ou nul fourni pour l'ajout de connexion.");
            throw new Exception("Email de l'utilisateur courant vide ou nul fourni.");
        }
        if (userEmail.equalsIgnoreCase(friendEmail)) {
            log.warn("Tentative d'ajout de soi-même comme ami : {}", userEmail);
            throw new Exception("Vous ne pouvez pas vous ajouter vous-même comme ami.");
        }

        User user = userRepository.findByEmailWithConnections(userEmail)
                .orElseThrow(() -> {
                    log.error("Utilisateur courant non trouvé : {}", userEmail);
                    return new Exception("Utilisateur courant non trouvé.");
                });
        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> {
                    log.error("Ami non trouvé avec l'email : {}", friendEmail);
                    return new Exception("L'utilisateur avec l'email '" + friendEmail + "' n'a pas été trouvé.");
                });

        // Vérifier si la connexion existe déjà (dans un sens ou l'autre)
        log.info("Vérification de l'existence de la connexion entre {} et {}", userEmail, friendEmail);
        boolean alreadyConnected = user.getConnections().contains(friend);

        if (alreadyConnected) {
            log.warn("Tentative d'ajout d'une connexion déjà existante entre {} et {}", userEmail, friendEmail);
            throw new Exception("Vous êtes déjà connecté avec cet utilisateur.");
        }

        user.getConnections().add(friend);
        // Si on veut une relation bidirectionnelle gérée par JPA aussi :
        // friend.getConnectedBy().add(user); // 'connectedBy' est l'inverse de 'connections'

        User userTx = userRepository.save(user);
        log.info("Connexion ajoutée entre {} et {}", userEmail, friendEmail);
        // Si bidirectionnel: userRepository.save(friend);
    }

    /**
     * Met à jour le profil d'un utilisateur, spécifiquement son nom d'utilisateur.
     * L'opération est transactionnelle.
     *
     * @param email L'e-mail de l'utilisateur dont le profil doit être mis à jour.
     * @param username Le nouveau nom d'utilisateur.
     * @throws Exception Si l'e-mail est invalide ou si l'utilisateur n'est pas trouvé.
     */
    @Transactional
    public void updateUserProfile(String email, String username) throws Exception {
        log.info("Mise à jour du profil de l'utilisateur : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour la mise à jour.");
            throw new Exception("Email vide ou nul fourni.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("Utilisateur non trouvé pour mise à jour : {}", email);
                    return new Exception("Utilisateur non trouvé.");
                });
        user.setUsername(username);
        User userTx = userRepository.save(user);
        log.info("Profil mis à jour pour l'utilisateur : {}", userTx.getEmail());
    }

    /**
     * Vérifie si le mot de passe actuel fourni correspond au mot de passe haché stocké pour l'utilisateur.
     *
     * @param email L'e-mail de l'utilisateur.
     * @param currentPassword Le mot de passe actuel à vérifier.
     * @return {@code true} si le mot de passe correspond, {@code false} sinon.
     * @throws RuntimeException si l'utilisateur n'est pas trouvé.
     */
    @Transactional
    public boolean checkCurrentPassword(String email, String currentPassword) {
        log.info("Vérification du mot de passe actuel pour l'utilisateur : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour la vérification du mot de passe.");
            return false;
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("Utilisateur non trouvé pour vérification du mot de passe : {}", email);
                    return new RuntimeException("Utilisateur non trouvé.");
                });
        boolean matches = passwordEncoder.matches(currentPassword, user.getPasswordHash());
        log.info("Mot de passe actuel vérifié pour l'utilisateur : {}", email);
        return matches;
    }

    /**
     * Change le mot de passe d'un utilisateur après avoir vérifié son ancien mot de passe.
     * L'opération est transactionnelle.
     *
     * @param email L'e-mail de l'utilisateur.
     * @param oldPassword L'ancien mot de passe de l'utilisateur pour vérification.
     * @param newPassword Le nouveau mot de passe à définir.
     * @throws Exception Si l'e-mail est invalide, si l'utilisateur n'est pas trouvé,
     *                   ou si l'ancien mot de passe est incorrect.
     */
    @Transactional
    public void changeUserPassword(String email, String oldPassword, String newPassword) throws Exception {
        log.info("Changement de mot de passe pour l'utilisateur : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour le changement de mot de passe.");
            throw new Exception("Email vide ou nul fourni.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("Utilisateur non trouvé pour changement de mot de passe : {}", email);
                    return new Exception("Utilisateur non trouvé.");
                });
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            log.warn("Ancien mot de passe incorrect pour l'utilisateur : {}", email);
            throw new Exception("Ancien mot de passe incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        User userTx = userRepository.save(user);
        log.info("Mot de passe changé avec succès pour l'utilisateur : {}", userTx.getEmail());
    }
}
