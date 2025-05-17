package com.openclassrooms.paymybuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object (DTO) utilisé pour capturer les données du formulaire
 * lors d'une demande de changement de mot de passe par un utilisateur.
 * Il contient le mot de passe actuel de l'utilisateur, le nouveau mot de passe souhaité
 * et une confirmation de ce nouveau mot de passe.
 */
@Data
public class ChangePasswordDto {

    /**
     * Le mot de passe actuel de l'utilisateur.
     * Ce champ est obligatoire et ne peut pas être vide ou constitué uniquement d'espaces.
     * Il est utilisé pour vérifier l'identité de l'utilisateur avant d'autoriser le changement.
     */
    @NotBlank(message = "L'ancien mot de passe ne peut pas être vide.")
    private String currentPassword;

    /**
     * Le nouveau mot de passe que l'utilisateur souhaite définir.
     * Ce champ est obligatoire.
     * Il doit avoir une longueur minimale de 6 caractères et maximale de 100 caractères.
     * De plus, il doit respecter un format spécifique : contenir au moins une lettre minuscule,
     * une lettre majuscule et un chiffre.
     */
    @NotBlank(message = "Le nouveau mot de passe ne peut pas être vide.")
    @Size(min = 6, max = 100, message = "Le nouveau mot de passe doit contenir au moins 6 caractères.")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "Le mot de passe doit contenir au moins une lettre minuscule, une lettre majuscule, un chiffre et avoir une longueur minimale de 6 caractères."
    )
    private String newPassword;

    /**
     * La confirmation du nouveau mot de passe.
     * Ce champ est obligatoire et doit correspondre exactement au {@code newPassword}
     * pour s'assurer que l'utilisateur n'a pas fait d'erreur de frappe.
     */
    @NotBlank(message = "La vérification du mot de passe ne peut pas être vide.")
    private String confirmPassword;
}
