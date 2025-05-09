package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.dto.UserRegistrationDto;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        if (userRepository.findByEmail(registrationDto.getEmail()).isPresent()) {
            throw new Exception("Un compte existe déjà avec cet email : " + registrationDto.getEmail());
        }
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setUsername(registrationDto.getUsername()); // Peut être null ou vide si non fourni
        // Le solde (balance) est déjà à 0 par défaut dans l'entité
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByEmailWithConnections(String email) {
        return userRepository.findByEmailWithConnections(email);
    }

    public Optional<User> findByEmailWithTransactions(String email) {
        return userRepository.findByEmailWithTransactions(email);
    }

    @Transactional
    public void addConnection(String userEmail, String friendEmail) throws Exception {
        if (userEmail.equalsIgnoreCase(friendEmail)) {
            throw new Exception("Vous ne pouvez pas vous ajouter vous-même comme ami.");
        }

        User user = userRepository.findByEmailWithConnections(userEmail)
                .orElseThrow(() -> new Exception("Utilisateur courant non trouvé."));
        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new Exception("L'utilisateur avec l'email '" + friendEmail + "' n'a pas été trouvé."));

        // Vérifier si la connexion existe déjà (dans un sens ou l'autre)
        boolean alreadyConnected = user.getConnections().contains(friend) ||
                friend.getConnections().contains(user); // Nécessite de charger les connexions de l'ami aussi

        if (alreadyConnected) {
            throw new Exception("Vous êtes déjà connecté avec cet utilisateur.");
        }

        // Ajoute la connexion. Grâce à la contrainte CHECK(user_id_1 < user_id_2) en BDD,
        // on peut théoriquement ajouter dans un seul sens (le plus petit ID en premier).
        // Mais JPA @ManyToMany ne gère pas ça directement. On ajoute classiquement.
        // L'implémentation JPA standard gère la table de jointure.
        user.getConnections().add(friend);
        // Si on veut une relation bidirectionnelle gérée par JPA aussi :
        // friend.getConnectedBy().add(user); // 'connectedBy' est l'inverse de 'connections'

        userRepository.save(user);
        // Si bidirectionnel: userRepository.save(friend);
    }

    @Transactional
    public void updateUserProfile(String email, String username /* autres champs si besoin */) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Utilisateur non trouvé."));
        user.setUsername(username);
        userRepository.save(user);
    }
}
