package com.openclassrooms.paymybuddy.controler;


import com.openclassrooms.paymybuddy.dto.TransferDto;
import com.openclassrooms.paymybuddy.model.Transaction;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.TransactionService;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.validation.Valid;
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

@Controller
public class HomeController {

    private final UserService userService;
    private final TransactionService transactionService;

    public HomeController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @GetMapping(value = {"/", "/home"})
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Charger l'utilisateur avec ses connexions ET ses transactions
        User currentUser = userService.findByEmailWithConnections(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        // Recharger avec transactions si nécessaire séparément ou adapter la requête UserRepository
        List<Transaction> transactions = Collections.emptyList();
        try {
            transactions = transactionService.getTransactionHistory(userEmail);
        } catch (Exception e) {
            model.addAttribute("transactionError", "Erreur lors de la récupération de l'historique.");
            // Log l'erreur côté serveur
        }


        model.addAttribute("user", currentUser);
        model.addAttribute("connections", currentUser.getConnections()); // Liste des amis
        model.addAttribute("transactions", transactions);
        model.addAttribute("transferDto", new TransferDto()); // Pour le formulaire de transfert
        model.addAttribute("balance", currentUser.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP));

        return "home"; // Retourne home.html
    }

    @PostMapping("/transfer")
    public String processTransfer(@Valid @ModelAttribute("transferDto") TransferDto transferDto,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes,
                                  Model model) { // Ajouter Model pour pouvoir renvoyer les infos si erreur

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = authentication.getName();

        if (result.hasErrors()) {
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
            redirectAttributes.addFlashAttribute("transferSuccess", "Transfert effectué avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("transferError", "Erreur lors du transfert : " + e.getMessage());
        }

        return "redirect:/home"; // Redirige vers home pour recharger les données à jour
    }
}
