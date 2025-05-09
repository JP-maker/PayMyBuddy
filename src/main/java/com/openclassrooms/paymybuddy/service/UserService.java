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

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        boolean alreadyConnected = user.getConnections().contains(friend) ||
                friend.getConnections().contains(user); // Nécessite de charger les connexions de l'ami aussi

        if (alreadyConnected) {
            log.warn("Tentative d'ajout d'une connexion déjà existante entre {} et {}", userEmail, friendEmail);
            throw new Exception("Vous êtes déjà connecté avec cet utilisateur.");
        }

        // Ajoute la connexion. Grâce à la contrainte CHECK(user_id_1 < user_id_2) en BDD,
        // on peut théoriquement ajouter dans un seul sens (le plus petit ID en premier).
        // Mais JPA @ManyToMany ne gère pas ça directement. On ajoute classiquement.
        // L'implémentation JPA standard gère la table de jointure.
        user.getConnections().add(friend);
        // Si on veut une relation bidirectionnelle gérée par JPA aussi :
        // friend.getConnectedBy().add(user); // 'connectedBy' est l'inverse de 'connections'

        User userTx = userRepository.save(user);
        log.info("Connexion ajoutée entre {} et {}", userEmail, friendEmail);
        // Si bidirectionnel: userRepository.save(friend);
    }

    @Transactional
    public void updateUserProfile(String email, String username /* autres champs si besoin */) throws Exception {
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
}
