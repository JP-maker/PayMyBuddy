package com.openclassrooms.paymybuddy.controller;


import com.openclassrooms.paymybuddy.config.SecurityConfig;
import com.openclassrooms.paymybuddy.dto.UserRegistrationDto;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.CustomUserDetailsService;
import com.openclassrooms.paymybuddy.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class) // Teste uniquement AuthController
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // Permet de simuler des requêtes HTTP

    @MockitoBean // Spring va injecter un mock de ce service dans le contexte du test
    private UserService userService;

    @MockitoBean // Requis car SecurityConfig dépend de lui, même si pas utilisé directement ici
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithAnonymousUser // Exécuter le test comme un utilisateur anonyme
    void loginPage_shouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithAnonymousUser
    void showRegistrationForm_shouldReturnRegisterView_withUserDto() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("userDto"));
    }

    @Test
    @WithAnonymousUser
    void processRegistration_shouldRedirectToLogin_onSuccess() throws Exception {
        // Arrange
        when(userService.registerNewUser(any(UserRegistrationDto.class))).thenReturn(new User()); // Simule succès

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("email", "test@example.com")
                        .param("password", "Password123")
                        .with(csrf())) // Important si CSRF est activé (par défaut dans Spring Security)
                .andExpect(status().is3xxRedirection()) // Statut de redirection
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithAnonymousUser
    void processRegistration_shouldReturnRegisterView_onValidationError() throws Exception {
        // Test avec un email manquant pour déclencher une erreur de validation
        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        //.param("email", "test@example.com") // Email manquant
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors()) // Vérifie qu'il y a des erreurs dans le modèle
                .andExpect(model().attributeHasFieldErrors("userDto", "email")); // Erreur spécifique sur l'email
    }

    @Test
    @WithAnonymousUser
    void processRegistration_shouldReturnRegisterView_onServiceException() throws Exception {
        // Arrange
        String errorMessage = "Email déjà utilisé";
        when(userService.registerNewUser(any(UserRegistrationDto.class)))
                .thenThrow(new Exception(errorMessage));

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("email", "existing@example.com")
                        .param("password", "Password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrorCode("userDto", "email", "error.userDto"));
        // Ou vérifier le message d'erreur directement si le contrôleur l'ajoute au modèle
        // .andExpect(model().attribute("errorMessage", errorMessage));
    }
}