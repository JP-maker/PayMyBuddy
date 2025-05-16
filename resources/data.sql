DROP DATABASE IF EXISTS paymybuddy_db;

-- Création de la base de données avec un jeu de caractères approprié
CREATE DATABASE paymybuddy_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Sélection de la base de données nouvellement créée pour les commandes suivantes
USE paymybuddy_db;

-- -----------------------------------------------------
-- Table `Users`
-- Stocke les informations sur les utilisateurs
-- -----------------------------------------------------
CREATE TABLE Users (
    `user_id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(100) NULL, -- Nom d'utilisateur optionnel pour affichage
    `email` VARCHAR(255) NOT NULL, -- Identifiant unique pour la connexion et l'ajout d'amis
    `password_hash` VARCHAR(255) NOT NULL, -- Mot de passe haché (ne jamais stocker en clair)
    `balance` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Date de création du compte
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- Date de dernière modification
    PRIMARY KEY (`user_id`),
    UNIQUE INDEX `idx_email_unique` (`email` ASC), -- Assure l'unicité de l'email
);

-- -----------------------------------------------------
-- Table `Connections`
-- Gère les relations d'amitié entre utilisateurs pour faciliter les transferts
-- -----------------------------------------------------
CREATE TABLE Connections (
    `user_id_1` INT NOT NULL, -- Premier utilisateur de la relation
    `user_id_2` INT NOT NULL, -- Second utilisateur de la relation
    PRIMARY KEY (`user_id_1`, `user_id_2`), -- Clé primaire composite pour garantir l'unicité de la paire
    INDEX `fk_connections_user2_idx` (`user_id_2` ASC), -- Index pour la clé étrangère
    CONSTRAINT `fk_connections_user1`
        FOREIGN KEY (`user_id_1`)
        REFERENCES Users (`user_id`)
        ON DELETE CASCADE -- Si un utilisateur est supprimé, ses connexions le sont aussi
        ON UPDATE CASCADE,
    CONSTRAINT `fk_connections_user2`
        FOREIGN KEY (`user_id_2`)
        REFERENCES Users (`user_id`)
        ON DELETE CASCADE -- Si un utilisateur est supprimé, ses connexions le sont aussi
        ON UPDATE CASCADE
);

-- -----------------------------------------------------
-- Table `Transactions`
-- Enregistre l'historique de tous les transferts d'argent
-- -----------------------------------------------------
CREATE TABLE Transactions (
    `transaction_id` INT NOT NULL AUTO_INCREMENT,
    `sender_id` INT NOT NULL, -- Utilisateur qui envoie l'argent
    `receiver_id` INT NOT NULL, -- Utilisateur qui reçoit l'argent
    `amount` DECIMAL(10, 2) NOT NULL, -- Montant transféré (doit être positif)
    `description` VARCHAR(255) NULL, -- Motif/description de la transaction
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Date et heure de la transaction
    PRIMARY KEY (`transaction_id`),
    INDEX `fk_transactions_sender_idx` (`sender_id` ASC), -- Index pour la clé étrangère sender
    INDEX `fk_transactions_receiver_idx` (`receiver_id` ASC), -- Index pour la clé étrangère receiver
    CONSTRAINT `fk_transactions_sender`
        FOREIGN KEY (`sender_id`)
        REFERENCES Users (`user_id`)
        ON DELETE RESTRICT -- Empêche la suppression d'un utilisateur ayant envoyé des fonds (intégrité historique)
        ON UPDATE CASCADE,
    CONSTRAINT `fk_transactions_receiver`
        FOREIGN KEY (`receiver_id`)
        REFERENCES Users (`user_id`)
        ON DELETE RESTRICT -- Empêche la suppression d'un utilisateur ayant reçu des fonds
        ON UPDATE CASCADE
);