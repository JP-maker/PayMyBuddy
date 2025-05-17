package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
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
            log.warn("Tentative de transfert vers soi-même par {}", senderEmail);
            throw new Exception("Vous ne pouvez pas transférer d'argent à vous-même.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Le montant doit être positif.");
        }

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> {
                        log.error("Utilisateur expéditeur non trouvé: {}", senderEmail);
                        return new Exception("Utilisateur expéditeur non trouvé.");
                });
        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> {
                    log.error("Utilisateur destinataire non trouvé: {}", receiverEmail);
                    return new Exception("Utilisateur destinataire non trouvé.");
                });

        // Calculer les frais (pour V1, mais bon à avoir)
        /*
        BigDecimal fee = amount.multiply(FEE_PERCENTAGE).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalDeducted = amount.add(fee);
        log.debug("Transfert de {} par {}: Montant={}, Frais={}, Total={}", amount, senderEmail, amount, fee, totalDeducted);
         */
        BigDecimal totalDeducted = amount;

        // Vérifier le solde de l'expéditeur
        if (sender.getBalance().compareTo(totalDeducted) < 0) {
            log.warn("Solde insuffisant pour {} : Solde={}, Requis={}", senderEmail, sender.getBalance(), totalDeducted);
            //throw new Exception("Solde insuffisant pour effectuer ce transfert (incluant les frais de " + fee.setScale(2, RoundingMode.HALF_UP) + ").");
            throw new Exception("Solde insuffisant pour effectuer ce transfert.");
        }

        // Mettre à jour les soldes
        log.debug("Mise à jour solde sender {} : {} -> {}", senderEmail, sender.getBalance(), sender.getBalance().subtract(totalDeducted));
        sender.setBalance(sender.getBalance().subtract(totalDeducted));
        log.debug("Mise à jour solde receiver {} : {} -> {}", receiverEmail, receiver.getBalance(), receiver.getBalance().add(amount));
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
        // Pour  la V1
        //transaction.setFee(fee);
        // timestamp est mis par défaut

        Transaction savedTx = transactionRepository.save(transaction);
        log.info("Transaction {} créée avec succès entre {} et {}", savedTx.getTransactionId(), senderEmail, receiverEmail);
    }

    @Transactional(readOnly = true) // Pas de modification de données ici
    public List<Transaction> getTransactionHistory(String userEmail) throws Exception {
        log.debug("Récupération de l'historique pour {}", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("Utilisateur non trouvé pour l'historique: {}", userEmail);
                    return new Exception("Utilisateur non trouvé.");
                    });
        // Retourne les transactions où l'utilisateur est soit expéditeur, soit destinataire
        List<Transaction> transactions = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
        log.debug("Trouvé {} transactions pour {}", transactions.size(), userEmail);
        return transactions;
    }
}
