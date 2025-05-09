package com.openclassrooms.paymybuddy.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationDto {
    @NotEmpty(message = "Le nom d'utilisateur ne peut pas être vide")
    private String username; // Ou rendre optionnel selon les besoins

    @NotEmpty(message = "L'email ne peut pas être vide")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotEmpty(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;
}
