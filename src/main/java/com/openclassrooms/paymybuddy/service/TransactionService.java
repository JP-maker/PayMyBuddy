package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service métier pour la gestion des transactions financières entre utilisateurs.
 * Ce service encapsule la logique de transfert d'argent, y compris la vérification des soldes,
 * la mise à jour des comptes des utilisateurs, et l'enregistrement des transactions.
 * Il permet également de récupérer l'historique des transactions pour un utilisateur.
 */
@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Pourcentage de frais appliqué sur chaque transaction.
     * Actuellement, la logique des frais est commentée (0.5%).
     */
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.005"); // 0.5%

    /**
     * Construit une instance de {@code TransactionService} avec les repositories nécessaires.
     *
     * @param transactionRepository Le repository pour accéder aux données des transactions.
     * @param userRepository        Le repository pour accéder aux données des utilisateurs.
     */
    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Effectue un transfert d'argent d'un utilisateur (expéditeur) à un autre (destinataire).
     * Cette opération est transactionnelle : si une étape échoue, toutes les modifications
     * de la base de données sont annulées (rollback).
     * <p>
     * Le processus inclut :
     * <ul>
     *     <li>La vérification que l'expéditeur ne transfère pas d'argent à lui-même.</li>
     *     <li>La vérification que le montant du transfert est positif.</li>
     *     <li>La récupération des entités utilisateur pour l'expéditeur et le destinataire.</li>
     *     <li>La vérification que l'expéditeur dispose d'un solde suffisant. (Note: la logique des frais est actuellement commentée).</li>
     *     <li>La mise à jour des soldes de l'expéditeur et du destinataire.</li>
     *     <li>La sauvegarde des modifications des utilisateurs.</li>
     *     <li>La création et la sauvegarde d'un nouvel enregistrement de transaction.</li>
     * </ul>
     *
     * @param senderEmail L'adresse e-mail de l'utilisateur qui envoie l'argent.
     * @param receiverEmail L'adresse e-mail de l'utilisateur qui reçoit l'argent.
     * @param amount Le montant à transférer.
     * @param description Une description optionnelle pour la transaction.
     * @throws Exception Si l'expéditeur ou le destinataire n'est pas trouvé, si le montant est invalide,
     *                   si l'expéditeur tente de transférer de l'argent à lui-même, ou si le solde de l'expéditeur est insuffisant.
     */
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

    /**
     * Récupère l'historique des transactions pour un utilisateur spécifié par son e-mail.
     * Cette méthode retourne une liste de transactions où l'utilisateur est soit l'expéditeur,
     * soit le destinataire, triées par date et heure de transaction en ordre décroissant
     * (les plus récentes d'abord).
     * L'opération est marquée comme transactionnelle en lecture seule pour optimisations.
     *
     * @param userEmail L'adresse e-mail de l'utilisateur dont l'historique des transactions est demandé.
     * @return Une liste de {@link Transaction} impliquant l'utilisateur, triée par date décroissante.
     * @throws Exception Si l'utilisateur spécifié par {@code userEmail} n'est pas trouvé.
     */
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
