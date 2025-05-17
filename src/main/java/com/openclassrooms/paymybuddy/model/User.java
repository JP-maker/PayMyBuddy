package com.openclassrooms.paymybuddy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe représentant un utilisateur du système.
 * Chaque utilisateur a un identifiant unique, un email, un mot de passe (hashé),
 * un solde, une date de création et une date de mise à jour.
 * Les utilisateurs peuvent avoir des connexions (amis) et effectuer des transactions.
 */
@Data
@Entity
@Table(name = "Users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(length = 100)
    private String username; // Optionnel

    @Column(nullable = false, unique = true, length = 255)
    private String email; // Identifiant principal

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relation ManyToMany pour les connexions (amis)
    // L'utilisateur courant est user_id_1 (owner)
    // Ses amis sont user_id_2 (inverse side)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "Connections",
            joinColumns = @JoinColumn(name = "user_id_1"), // Clé de cette entité (User) dans la table de jointure
            inverseJoinColumns = @JoinColumn(name = "user_id_2") // Clé de l'autre entité (les amis) dans la table de jointure
    )
    @ToString.Exclude // Éviter les boucles infinies avec Lombok ToString
    @EqualsAndHashCode.Exclude // Éviter les boucles infinies avec Lombok Equals/HashCode
    private Set<User> connections = new HashSet<>();

    // Pour la relation inverse (ceux dont JE suis l'ami) - Optionnel si non utilisé
    @ManyToMany(mappedBy = "connections", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> connectedBy = new HashSet<>();


    // Transactions envoyées par cet utilisateur
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Transaction> sentTransactions = new HashSet<>();

    // Transactions reçues par cet utilisateur
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Transaction> receivedTransactions = new HashSet<>();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}