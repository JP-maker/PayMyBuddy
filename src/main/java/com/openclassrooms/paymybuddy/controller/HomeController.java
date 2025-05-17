package com.openclassrooms.paymybuddy.controller;


import com.openclassrooms.paymybuddy.dto.TransferDto;
import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.TransactionService;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Contrôleur principal de l'application, gérant la page d'accueil.
 * Cette page affiche les informations de l'utilisateur connecté, son solde,
 * la liste de ses connexions (amis), son historique de transactions,
 * et fournit un formulaire pour effectuer des transferts d'argent.
 */
@Slf4j
@Controller
public class HomeController {

    private final UserService userService;
    private final TransactionService transactionService;

    /**
     * Construit une instance de {@code HomeController} avec les services requis.
     *
     * @param userService        Le service pour les opérations liées aux utilisateurs.
     * @param transactionService Le service pour gérer les transactions financières.
     */
    public HomeController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    /**
     * Gère les requêtes GET vers "/" et "/home" pour afficher la page d'accueil.
     * Récupère l'utilisateur actuellement authentifié, son solde, ses connexions,
     * et son historique de transactions. Ces informations sont ajoutées au modèle
     * pour être affichées dans la vue. Un {@link TransferDto} vide est également
     * préparé pour le formulaire de transfert.
     *
     * @param model L'objet Model de Spring pour passer des données à la vue.
     * @return Le nom de la vue (template Thymeleaf) pour la page d'accueil ("home").
     * @throws RuntimeException si l'utilisateur actuellement authentifié n'est pas trouvé.
     */
    @GetMapping(value = {"/", "/home"})
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        log.debug("Affichage de la page home pour {}", userEmail);

        // Charger l'utilisateur avec ses connexions ET ses transactions
        User currentUser = userService.findByEmailWithConnections(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        // Recharger avec transactions si nécessaire séparément ou adapter la requête UserRepository
        List<Transaction> transactions = Collections.emptyList();
        try {
            transactions = transactionService.getTransactionHistory(userEmail);

        } catch (Exception e) {
            model.addAttribute("transactionError", "Erreur lors de la récupération de l'historique.");
            log.error("Erreur lors du chargement de l'historique pour {}: {}", userEmail, e.getMessage());
        }


        model.addAttribute("user", currentUser);
        model.addAttribute("connections", currentUser.getConnections()); // Liste des amis
        model.addAttribute("transactions", transactions);
        model.addAttribute("transferDto", new TransferDto()); // Pour le formulaire de transfert
        model.addAttribute("balance", currentUser.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP));
        log.debug("Historique chargé pour {}: {} transactions", userEmail, transactions.size());

        return "home"; // Retourne home.html
    }

    /**
     * Gère les requêtes POST vers "/transfer" pour traiter une demande de transfert d'argent.
     * Valide les données du transfert. Si la validation échoue, les informations nécessaires
     * sont rechargées et l'utilisateur est retourné à la page d'accueil avec les erreurs.
     * Si la validation réussit, le service de transaction est appelé pour effectuer le transfert.
     * L'utilisateur est ensuite redirigé vers la page d'accueil avec un message de succès ou d'erreur.
     *
     * @param transferDto        Le DTO {@link TransferDto} contenant les détails du transfert, validé.
     * @param result             L'objet {@link BindingResult} qui contient les résultats de la validation.
     * @param redirectAttributes Utilisé pour ajouter des attributs flash pour les messages lors de la redirection.
     * @param model              L'objet Model de Spring, utilisé pour repasser les données nécessaires à la vue home
     *                           en cas d'échec de validation.
     * @return Une chaîne de redirection vers "/home" après la tentative de transfert,
     *         ou le nom de la vue "home" en cas d'échec de validation du formulaire.
     * @throws RuntimeException si l'utilisateur actuellement authentifié (expéditeur) n'est pas trouvé
     *                          lors du rechargement des données pour affichage d'erreur.
     */
    @PostMapping("/transfer")
    public String processTransfer(@Valid @ModelAttribute("transferDto") TransferDto transferDto,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes,
                                  Model model) { // Ajouter Model pour pouvoir renvoyer les infos si erreur

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = authentication.getName();

        log.info("Tentative de transfert de {} vers {} par {} pour un montant de {}",
                senderEmail, transferDto.getReceiverEmail(), senderEmail, transferDto.getAmount());

        if (result.hasErrors()) {
            log.warn("Echec de validation pour le transfert de {}: {}", senderEmail, result.getAllErrors());
            // Recharger les données nécessaires pour la vue home si on y retourne directement
            User currentUser = userService.findByEmailWithConnections(senderEmail).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            List<Transaction> transactions = Collections.emptyList();
            try {
                transactions = transactionService.getTransactionHistory(senderEmail);
            } catch (Exception e) { model.addAttribute("transactionError", "Erreur récupération historique."); }

            model.addAttribute("user", currentUser);
            model.addAttribute("connections", currentUser.getConnections());
            model.addAttribute("transactions", transactions);
            model.addAttribute("balance", currentUser.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP));
            // Le transferDto avec les erreurs est déjà dans le modèle grâce à @ModelAttribute
            return "home"; // Retourne à la page home avec les erreurs de validation affichées
        }

        try {
            transactionService.transferMoney(
                    senderEmail,
                    transferDto.getReceiverEmail(),
                    transferDto.getAmount(),
                    transferDto.getDescription()
            );
            log.info("Transfert réussi de {} vers {} par {}", senderEmail, transferDto.getReceiverEmail(), senderEmail);
            redirectAttributes.addFlashAttribute("transferSuccess", "Transfert effectué avec succès !");
        } catch (Exception e) {
            log.error("Erreur lors du transfert de {} vers {}: {}", senderEmail, transferDto.getReceiverEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("transferError", "Erreur lors du transfert : " + e.getMessage());
        }

        return "redirect:/home"; // Redirige vers home pour recharger les données à jour
    }
}
