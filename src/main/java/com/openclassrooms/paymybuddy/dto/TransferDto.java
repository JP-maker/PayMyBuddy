package com.openclassrooms.paymybuddy.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferDto {
    @NotEmpty(message = "L'email du destinataire ne peut pas être vide")
    @Email(message = "Format d'email invalide pour le destinataire")
    private String receiverEmail;

    @NotNull(message = "Le montant ne peut pas être vide")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;

    @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
    private String description;
}