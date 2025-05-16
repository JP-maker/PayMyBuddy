package com.openclassrooms.paymybuddy.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "Le mot de passe doit contenir au moins une lettre minuscule, une lettre majuscule, un chiffre et avoir une longueur minimale de 6 caractères."
    )
    private String password;
}
