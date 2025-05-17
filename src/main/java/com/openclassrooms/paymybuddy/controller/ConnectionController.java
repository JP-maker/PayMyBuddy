package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.dto.AddConnectionDto;
import com.openclassrooms.paymybuddy.model.User;
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

/**
 * Contrôleur Spring MVC responsable de la gestion des connexions (amis) pour l'utilisateur connecté.
 * Il permet d'afficher la liste des connexions existantes et d'en ajouter de nouvelles.
 */
@Slf4j
@Controller
public class ConnectionController {

    private final UserService userService;

    /**
     * Construit une instance de {@code ConnectionController} avec le service utilisateur requis.
     *
     * @param userService Le service pour gérer les opérations liées aux utilisateurs et à leurs connexions.
     */
    public ConnectionController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gère les requêtes GET vers "/connections" et affiche la page des connexions de l'utilisateur.
     * Récupère l'utilisateur actuellement authentifié, charge ses connexions et les prépare
     * pour l'affichage. Un DTO vide {@link AddConnectionDto} est également ajouté au modèle
     * pour le formulaire d'ajout de connexion.
     *
     * @param model L'objet Model de Spring pour passer des données à la vue (liste des connexions, DTO pour l'ajout).
     * @return Le nom de la vue (template Thymeleaf) pour la page des connexions ("connections").
     * @throws RuntimeException si l'utilisateur actuellement authentifié n'est pas trouvé dans la base de données.
     */
    @GetMapping("/connections")
    public String connectionsPage(Model model) {
        log.debug("Accès à la page des connexions");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userService.findByEmailWithConnections(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        model.addAttribute("connections", currentUser.getConnections());
        model.addAttribute("addConnectionDto", new AddConnectionDto());
        log.debug("Utilisateur connecté : {}", currentUser.getEmail());

        return "connections"; // Retourne connections.html
    }

    /**
     * Gère les requêtes POST vers "/connections/add" pour ajouter une nouvelle connexion (ami).
     * Valide l'email de l'ami fourni. Si la validation échoue, retourne à la page des connexions
     * avec les erreurs affichées, en s'assurant de recharger la liste des connexions existantes.
     * Si l'ajout est réussi, redirige vers la page des connexions avec un message de succès.
     * En cas d'erreur métier (ex: ami non trouvé, déjà ami), redirige avec un message d'erreur.
     *
     * @param addConnectionDto Le DTO {@link AddConnectionDto} contenant l'email de l'ami à ajouter, validé.
     * @param result L'objet {@link BindingResult} qui contient les résultats de la validation.
     * @param redirectAttributes Utilisé pour ajouter des attributs flash pour les messages de succès/erreur lors de la redirection.
     * @param model L'objet Model de Spring, utilisé pour repasser les connexions existantes à la vue en cas d'erreur de validation.
     * @return Une chaîne de redirection vers "/connections" après la tentative d'ajout,
     *         ou le nom de la vue "connections" en cas d'échec de validation du formulaire.
     * @throws RuntimeException si l'utilisateur actuellement authentifié n'est pas trouvé lors du rechargement pour affichage d'erreur.
     */
    @PostMapping("/connections/add")
    public String addConnection(@Valid @ModelAttribute("addConnectionDto") AddConnectionDto addConnectionDto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) { // Ajout Model pour retour erreur

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        log.debug("Tentative d'ajout de connexion pour {}: {}", userEmail, addConnectionDto.getFriendEmail());

        if (result.hasErrors()) {
            // Recharger les connexions actuelles pour les réafficher avec le formulaire en erreur
            User currentUser = userService.findByEmailWithConnections(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            model.addAttribute("connections", currentUser.getConnections());

            log.error("Erreur de validation pour l'ajout de connexion: {}", result.getAllErrors());
            // Le addConnectionDto avec l'erreur est déjà là via @ModelAttribute
            return "connections"; // Retourne à la page avec l'erreur de validation
        }

        try {
            userService.addConnection(userEmail, addConnectionDto.getFriendEmail());
            log.info("Connexion ajoutée avec succès entre {} et {}", userEmail, addConnectionDto.getFriendEmail());
            redirectAttributes.addFlashAttribute("connectionSuccess", "Ami ajouté avec succès !");
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de connexion pour {}: {}", userEmail, e.getMessage());
            redirectAttributes.addFlashAttribute("connectionError", "Erreur lors de l'ajout : " + e.getMessage());
        }

        return "redirect:/connections"; // Redirige vers la page des connexions
    }
}