package com.openclassrooms.paymybuddy.service;


import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User receiver;
    private final BigDecimal feePercentage = new BigDecimal("0.005"); // Doit correspondre à la constante dans le service

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setUserId(1);
        sender.setEmail("sender@example.com");
        sender.setBalance(new BigDecimal("200.00"));

        receiver = new User();
        receiver.setUserId(2);
        receiver.setEmail("receiver@example.com");
        receiver.setBalance(new BigDecimal("50.00"));
    }

    @Test
    void transferMoney_shouldSucceed_whenValid() throws Exception {
        // Arrange
        BigDecimal amountToTransfer = new BigDecimal("100.00");
        String description = "Test transfer";

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setTransactionId(1); // Simuler la génération d'ID
            return tx;
        });

        /* for V1
        BigDecimal expectedFee = amountToTransfer.multiply(feePercentage).setScale(4, RoundingMode.HALF_UP);
        BigDecimal expectedTotalDeducted = amountToTransfer.add(expectedFee);
         */

        BigDecimal expectedSenderBalanceAfter = sender.getBalance().subtract(amountToTransfer);
        BigDecimal expectedReceiverBalanceAfter = receiver.getBalance().add(amountToTransfer);

        // Act
        transactionService.transferMoney("sender@example.com", "receiver@example.com", amountToTransfer, description);

        // Assert
        assertEquals(expectedSenderBalanceAfter.setScale(2, RoundingMode.HALF_UP), sender.getBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(expectedReceiverBalanceAfter.setScale(2, RoundingMode.HALF_UP), receiver.getBalance().setScale(2, RoundingMode.HALF_UP));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();

        assertEquals(sender, savedTransaction.getSender());
        assertEquals(receiver, savedTransaction.getReceiver());
        assertEquals(amountToTransfer.setScale(2, RoundingMode.HALF_UP), savedTransaction.getAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals(description, savedTransaction.getDescription());
        /* for V1
        assertEquals(expectedFee.setScale(4, RoundingMode.HALF_UP), savedTransaction.getFee().setScale(4, RoundingMode.HALF_UP));
         */
        assertEquals(amountToTransfer.setScale(2, RoundingMode.HALF_UP), savedTransaction.getSender().getBalance().setScale(2, RoundingMode.HALF_UP));

        verify(userRepository, times(2)).save(any(User.class)); // sender et receiver
    }

    @Test
    void transferMoney_shouldThrowException_whenSenderHasInsufficientBalance() {
        // Arrange
        BigDecimal amountToTransfer = new BigDecimal("300.00"); // Plus que le solde du sender
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            transactionService.transferMoney("sender@example.com", "receiver@example.com", amountToTransfer, "Test");
        });
        assertTrue(exception.getMessage().startsWith("Solde insuffisant"));

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void transferMoney_shouldThrowException_whenSenderIsReceiver() {
        // Arrange
        BigDecimal amountToTransfer = new BigDecimal("10.00");

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            transactionService.transferMoney("sender@example.com", "sender@example.com", amountToTransfer, "Test");
        });
        assertEquals("Vous ne pouvez pas transférer d'argent à vous-même.", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void transferMoney_shouldThrowException_whenAmountIsZeroOrNegative() {
        // Arrange
        BigDecimal zeroAmount = BigDecimal.ZERO;
        BigDecimal negativeAmount = new BigDecimal("-10.00");

        // Act & Assert for zero
        Exception exceptionZero = assertThrows(Exception.class, () -> {
            transactionService.transferMoney("sender@example.com", "receiver@example.com", zeroAmount, "Test");
        });
        assertEquals("Le montant doit être positif.", exceptionZero.getMessage());

        // Act & Assert for negative
        Exception exceptionNegative = assertThrows(Exception.class, () -> {
            transactionService.transferMoney("sender@example.com", "receiver@example.com", negativeAmount, "Test");
        });
        assertEquals("Le montant doit être positif.", exceptionNegative.getMessage());

        verify(userRepository, never()).findByEmail(anyString());
    }


    @Test
    void getTransactionHistory_shouldReturnTransactions_whenUserExists() throws Exception {
        // Arrange
        Transaction tx1 = new Transaction();
        tx1.setSender(sender);
        tx1.setReceiver(receiver);
        tx1.setAmount(new BigDecimal("10.00"));

        Transaction tx2 = new Transaction();
        tx2.setSender(receiver); // L'utilisateur testé est le receiver ici
        tx2.setReceiver(sender);
        tx2.setAmount(new BigDecimal("20.00"));

        List<Transaction> expectedTransactions = Arrays.asList(tx1, tx2);

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(sender, sender))
                .thenReturn(expectedTransactions);

        // Act
        List<Transaction> actualTransactions = transactionService.getTransactionHistory("sender@example.com");

        // Assert
        assertNotNull(actualTransactions);
        assertEquals(2, actualTransactions.size());
        assertEquals(expectedTransactions, actualTransactions);

        verify(userRepository, times(1)).findByEmail("sender@example.com");
        verify(transactionRepository, times(1)).findBySenderOrReceiverOrderByTimestampDesc(sender, sender);
    }

    @Test
    void getTransactionHistory_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            transactionService.getTransactionHistory("unknown@example.com");
        });
        assertEquals("Utilisateur non trouvé.", exception.getMessage());

        verify(transactionRepository, never()).findBySenderOrReceiverOrderByTimestampDesc(any(User.class), any(User.class));
    }
}