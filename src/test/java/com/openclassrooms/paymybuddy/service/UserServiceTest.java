package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.dto.UserRegistrationDto;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Intègre Mockito avec JUnit 5
class UserServiceTest {

    @Mock // Mockito va créer un mock de cette dépendance
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Mockito va injecter les mocks ci-dessus dans cette instance
    private UserService userService;

    private UserRegistrationDto registrationDto;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password123");

        user1 = new User();
        user1.setUserId(1);
        user1.setEmail("user1@example.com");
        user1.setUsername("userOne");
        user1.setPasswordHash("hashedPassword1");
        user1.setBalance(new BigDecimal("100.00"));
        user1.setConnections(new HashSet<>()); // Initialiser pour éviter NPE

        user2 = new User();
        user2.setUserId(2);
        user2.setEmail("user2@example.com");
        user2.setUsername("userTwo");
        user2.setPasswordHash("hashedPassword2");
        user2.setBalance(new BigDecimal("50.00"));
        user2.setConnections(new HashSet<>());
    }

    @Test
    void registerNewUser_shouldSucceed_whenEmailNotExists() throws Exception {
        // Arrange
        when(userRepository.findByEmail(registrationDto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Retourne l'objet User passé à save

        // Act
        User registeredUser = userService.registerNewUser(registrationDto);

        // Assert
        assertNotNull(registeredUser);
        assertEquals(registrationDto.getEmail(), registeredUser.getEmail());
        assertEquals(registrationDto.getUsername(), registeredUser.getUsername());
        assertEquals("encodedPassword", registeredUser.getPasswordHash());
        assertEquals(BigDecimal.ZERO.setScale(2), registeredUser.getBalance().setScale(2)); // Vérifier le solde initial

        verify(userRepository, times(1)).findByEmail(registrationDto.getEmail());
        verify(passwordEncoder, times(1)).encode(registrationDto.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerNewUser_shouldThrowException_whenEmailExists() {
        // Arrange
        when(userRepository.findByEmail(registrationDto.getEmail())).thenReturn(Optional.of(new User()));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.registerNewUser(registrationDto);
        });
        assertEquals("Un compte existe déjà avec cet email : " + registrationDto.getEmail(), exception.getMessage());

        verify(userRepository, times(1)).findByEmail(registrationDto.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        // Arrange
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));

        // Act
        Optional<User> foundUser = userService.findByEmail("user1@example.com");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(user1.getEmail(), foundUser.get().getEmail());
        verify(userRepository, times(1)).findByEmail("user1@example.com");
    }

    @Test
    void findByEmail_shouldThrowRuntimeException_whenUserNotExists() { // Renomme le test pour refléter le comportement
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findByEmail(nonExistentEmail); // Appelle la méthode qui devrait lancer l'exception
        });

        // Assertions supplémentaires sur l'exception (optionnel mais bien)
        assertEquals("Utilisateur non trouvé avec l'email : " + nonExistentEmail, exception.getMessage());

        // Vérifie que le repository a bien été appelé
        verify(userRepository, times(1)).findByEmail(nonExistentEmail);
    }

    @Test
    void addConnection_shouldSucceed_whenUsersExistAndNotConnected() throws Exception {
        // Arrange
        when(userRepository.findByEmailWithConnections(user1.getEmail())).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail(user2.getEmail())).thenReturn(Optional.of(user2));
        // Simule que save persiste les modifications
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        userService.addConnection(user1.getEmail(), user2.getEmail());

        // Assert
        assertTrue(user1.getConnections().contains(user2));
        // Si la relation est bidirectionnelle dans le service et testée :
        // assertTrue(user2.getConnectedBy().contains(user1));

        verify(userRepository, times(1)).findByEmailWithConnections(user1.getEmail());
        verify(userRepository, times(1)).findByEmail(user2.getEmail());
        verify(userRepository, times(1)).save(user1); // Vérifie que user1 (avec la nouvelle connexion) est sauvegardé
    }

    @Test
    void addConnection_shouldThrowException_whenAddingSelf() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.addConnection(user1.getEmail(), user1.getEmail());
        });
        assertEquals("Vous ne pouvez pas vous ajouter vous-même comme ami.", exception.getMessage());
        verify(userRepository, never()).findByEmailWithConnections(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addConnection_shouldThrowException_whenFriendNotFound() {
        // Arrange
        when(userRepository.findByEmailWithConnections(user1.getEmail())).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail(user2.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.addConnection(user1.getEmail(), user2.getEmail());
        });
        assertEquals("L'utilisateur avec l'email '" + user2.getEmail() + "' n'a pas été trouvé.", exception.getMessage());
        verify(userRepository, times(1)).findByEmailWithConnections(user1.getEmail());
        verify(userRepository, times(1)).findByEmail(user2.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addConnection_shouldThrowException_whenAlreadyConnected() {
        // Arrange
        // Simuler que user1 a déjà user2 dans ses connexions
        user1.getConnections().add(user2); // Ajout direct pour le test
        // user2.getConnections().add(user1); // Si c'est une ManyToMany bidirectionnelle

        when(userRepository.findByEmailWithConnections(user1.getEmail())).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail(user2.getEmail())).thenReturn(Optional.of(user2));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.addConnection(user1.getEmail(), user2.getEmail());
        });
        assertEquals("Vous êtes déjà connecté avec cet utilisateur.", exception.getMessage());

        verify(userRepository, times(1)).findByEmailWithConnections(user1.getEmail());
        verify(userRepository, times(1)).findByEmail(user2.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_shouldUpdateUsername() throws Exception {
        // Arrange
        String newUsername = "updatedUserOne";
        when(userRepository.findByEmail(user1.getEmail())).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.updateUserProfile(user1.getEmail(), newUsername);

        // Assert
        assertEquals(newUsername, user1.getUsername());
        verify(userRepository, times(1)).findByEmail(user1.getEmail());
        verify(userRepository, times(1)).save(user1);
    }
}