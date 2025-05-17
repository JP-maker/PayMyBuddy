package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections; // Correction: Utiliser Collections.singletonList

/**
 * Service d'implémentation de {@link UserDetailsService} de Spring Security.
 * Ce service est responsable de charger les détails spécifiques à l'utilisateur (comme l'e-mail,
 * le mot de passe haché et les autorités/rôles) à partir de la base de données
 * lors du processus d'authentification.
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Construit une instance de {@code CustomUserDetailsService} avec le repository utilisateur requis.
     *
     * @param userRepository Le repository pour accéder aux données des utilisateurs.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge les données d'un utilisateur par son e-mail (utilisé ici comme nom d'utilisateur).
     * Cette méthode est appelée par Spring Security lors de la tentative d'authentification.
     * Elle recherche l'utilisateur dans la base de données via son e-mail. Si l'utilisateur est trouvé,
     * un objet {@link UserDetails} est construit avec l'e-mail de l'utilisateur, son mot de passe haché
     * et ses autorités (rôles).
     * Pour cet exemple, un rôle "ROLE_USER" est attribué par défaut à tous les utilisateurs.
     *
     * @param email L'adresse e-mail de l'utilisateur à charger.
     * @return Un objet {@link UserDetails} contenant les informations de l'utilisateur.
     * @throws UsernameNotFoundException si aucun utilisateur n'est trouvé avec l'e-mail fourni,
     *                                   ou si l'e-mail est nul ou vide.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Chargement de l'utilisateur avec l'email : {}", email);
        if (email == null || email.isEmpty()) {
            log.warn("Email vide ou nul fourni pour le chargement de l'utilisateur.");
            throw new UsernameNotFoundException("Email vide ou nul fourni.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + email));
        log.info("Utilisateur trouvé : {}", user.getEmail());
        // Pour cet exemple simple, on donne juste un rôle USER à tout le monde
        // Dans une vraie appli, les rôles seraient stockés en BDD
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        log.info("Rôle attribué à l'utilisateur : {}", authority.getAuthority());
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(authority) // Donner le rôle/autorité
        );
    }
}