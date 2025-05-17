package com.openclassrooms.paymybuddy.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Data Transfer Object (DTO) utilisé pour capturer les données du formulaire
 * lors de l'inscription d'un nouvel utilisateur à l'application.
 * Il contient le nom d'utilisateur, l'adresse e-mail et le mot de passe choisis par l'utilisateur.
 */
@Data
public class UserRegistrationDto {

    /**
     * Le nom d'utilisateur choisi par le nouvel utilisateur.
     * Ce champ est obligatoire.
     * Note : Les contraintes de taille ou de format spécifiques pourraient être ajoutées ici si nécessaire.
     */
    @NotEmpty(message = "Le nom d'utilisateur ne peut pas être vide")
    private String username; // Ou rendre optionnel selon les besoins

    /**
     * L'adresse e-mail fournie par le nouvel utilisateur.
     * Ce champ est obligatoire et doit être une adresse e-mail valide.
     * L'e-mail sera utilisé comme identifiant principal pour la connexion.
     */
    @NotEmpty(message = "L'email ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    private String email;

    /**
     * Le mot de passe choisi par le nouvel utilisateur.
     * Ce champ est obligatoire.
     * Il doit respecter des critères de complexité :
     * - Contenir au moins une lettre minuscule.
     * - Contenir au moins une lettre majuscule.
     * - Contenir au moins un chiffre.
     * - Avoir une longueur minimale de 6 caractères.
     * La contrainte {@code @Size} pourrait également être utilisée ici pour définir une longueur maximale si désiré.
     */
    @NotEmpty(message = "Le mot de passe ne peut pas être vide")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "Le mot de passe doit contenir au moins une lettre minuscule, une lettre majuscule, un chiffre et avoir une longueur minimale de 6 caractères."
    )
    private String password;
}
