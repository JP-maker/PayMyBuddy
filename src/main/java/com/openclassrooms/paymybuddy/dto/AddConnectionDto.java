package com.openclassrooms.paymybuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Data Transfer Object (DTO) utilisé pour capturer les données du formulaire
 * lors de l'ajout d'une nouvelle connexion (ami).
 * Il contient l'adresse e-mail de l'ami que l'utilisateur souhaite ajouter.
 */
@Data
public class AddConnectionDto {
    /**
     * L'adresse e-mail de l'ami à ajouter comme connexion.
     * Ce champ est obligatoire et doit être une adresse e-mail valide.
     */
    @NotEmpty(message = "L'email de l'ami ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    private String friendEmail;
}
