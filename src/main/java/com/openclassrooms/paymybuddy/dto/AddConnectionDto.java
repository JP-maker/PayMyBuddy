package com.openclassrooms.paymybuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AddConnectionDto {
    @NotEmpty(message = "L'email de l'ami ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    private String friendEmail;
}
