package com.openclassrooms.paymybuddy.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) utilisé pour capturer les données du formulaire
 * lors d'une opération de transfert d'argent d'un utilisateur à un autre (ami/connexion).
 * Il contient l'adresse e-mail du destinataire, le montant à transférer et une description optionnelle.
 */
@Data
public class TransferDto {

    /**
     * L'adresse e-mail du destinataire du transfert.
     * Ce champ est obligatoire et doit être une adresse e-mail valide.
     */
    @NotEmpty(message = "L'email du destinataire ne peut pas être vide")
    @Email(message = "Format d'email invalide pour le destinataire")
    private String receiverEmail;

    /**
     * Le montant à transférer.
     * Ce champ est obligatoire et doit être une valeur numérique positive, supérieure à 0.00.
     */
    @NotNull(message = "Le montant ne peut pas être vide")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;

    /**
     * Une description optionnelle pour le transfert.
     * Si fournie, sa longueur ne doit pas dépasser 255 caractères.
     */
    @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
    private String description;
}