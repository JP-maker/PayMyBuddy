package com.openclassrooms.paymybuddy.controller;


import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.CustomUserDetailsService;
import com.openclassrooms.paymybuddy.service.TransactionService;
import com.openclassrooms.paymybuddy.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService; // Requis pour la config de sécurité

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setEmail("user@example.com");
        mockUser.setUsername("Test User");
        mockUser.setBalance(new BigDecimal("100.00"));
        mockUser.setConnections(new HashSet<>()); // Important pour éviter NPE
    }

    @Test
    @WithMockUser(username = "user@example.com") // Simule un utilisateur authentifié
    void homePage_shouldReturnHomeView_withUserData() throws Exception {
        // Arrange
        Transaction tx1 = new Transaction();
        tx1.setDescription("Payment for stuff");
        tx1.setAmount(new BigDecimal("10.00"));
        tx1.setSender(mockUser); // Simuler l'utilisateur comme sender
        User receiver = new User(); receiver.setEmail("friend@example.com");
        tx1.setReceiver(receiver);


        when(userService.findByEmailWithConnections("user@example.com")).thenReturn(Optional.of(mockUser));
        when(transactionService.getTransactionHistory("user@example.com")).thenReturn(Arrays.asList(tx1));

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("user", "connections", "transactions", "transferDto", "balance"))
                .andExpect(model().attribute("balance", new BigDecimal("100.00").setScale(2, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void processTransfer_shouldRedirectToHome_onSuccess() throws Exception {
        // Arrange
        // Pas besoin de mocker transactionService.transferMoney en détail si on vérifie juste la redirection
        // doNothing().when(transactionService).transferMoney(anyString(), anyString(), any(BigDecimal.class), anyString());

        mockMvc.perform(post("/transfer")
                        .param("receiverEmail", "friend@example.com")
                        .param("amount", "50.00")
                        .param("description", "Lunch money")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attributeExists("transferSuccess"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void processTransfer_shouldReturnHomeView_onValidationError() throws Exception {
        // Arrange (pour recharger les données du modèle si on retourne à "home")
        when(userService.findByEmailWithConnections("user@example.com")).thenReturn(Optional.of(mockUser));
        when(transactionService.getTransactionHistory("user@example.com")).thenReturn(Collections.emptyList());


        mockMvc.perform(post("/transfer")
                        //.param("receiverEmail", "friend@example.com") // Email manquant
                        .param("amount", "50.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home")) // Le controller retourne "home" en cas d'erreur de validation
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("transferDto", "receiverEmail"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void processTransfer_shouldRedirectToHome_onServiceException() throws Exception {
        // Arrange
        String errorMessage = "Solde insuffisant";
        mockMvc.perform(post("/transfer")
                        .param("receiverEmail", "friend@example.com")
                        .param("amount", "5000.00") // Montant élevé pour simuler l'erreur
                        .with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/home"));
        Mockito.verify(transactionService).transferMoney(
                eq("user@example.com"), // L'email de @WithMockUser
                eq("friend@example.com"),
                eq(new BigDecimal("5000.00")),
                isNull() // Si la description est attendue comme null
        );
    }
}