package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.005"); // 0.5%

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(rollbackFor = Exception.class) // S'assurer que tout est annulé en cas d'erreur
    public void transferMoney(String senderEmail, String receiverEmail, BigDecimal amount, String description) throws Exception {

        if (senderEmail.equalsIgnoreCase(receiverEmail)) {
            throw new Exception("Vous ne pouvez pas transférer d'argent à vous-même.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Le montant doit être positif.");
        }

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new Exception("Utilisateur expéditeur non trouvé."));
        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new Exception("Utilisateur destinataire non trouvé."));

        // Calculer les frais (pour V1, mais bon à avoir)
        BigDecimal fee = amount.multiply(FEE_PERCENTAGE).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalDeducted = amount.add(fee);

        // Vérifier le solde de l'expéditeur
        if (sender.getBalance().compareTo(totalDeducted) < 0) {
            throw new Exception("Solde insuffisant pour effectuer ce transfert (incluant les frais de " + fee.setScale(2, RoundingMode.HALF_UP) + ").");
        }

        // Mettre à jour les soldes
        sender.setBalance(sender.getBalance().subtract(totalDeducted));
        receiver.setBalance(receiver.getBalance().add(amount));

        // Sauvegarder les utilisateurs mis à jour
        userRepository.save(sender);
        userRepository.save(receiver);

        // Créer et sauvegarder l'enregistrement de la transaction
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setFee(fee);
        // timestamp est mis par défaut

        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true) // Pas de modification de données ici
    public List<Transaction> getTransactionHistory(String userEmail) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new Exception("Utilisateur non trouvé."));
        // Retourne les transactions où l'utilisateur est soit expéditeur, soit destinataire
        return transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
    }
}
