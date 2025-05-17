package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.dto.UserRegistrationDto;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur gérant les processus d'authentification des utilisateurs,
 * y compris l'affichage des pages de connexion et d'inscription,
 * ainsi que le traitement de l'inscription des nouveaux utilisateurs.
 */
@Slf4j
@Controller
public class AuthController {

    private final UserService userService;

    /**
     * Construit une instance de {@code AuthController} avec le service utilisateur requis.
     *
     * @param userService Le service pour les opérations liées aux utilisateurs.
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gère les requêtes GET vers "/login" et affiche la page de connexion.
     *
     * @return Le nom de la vue (template Thymeleaf) pour la page de connexion ("login").
     */
    @GetMapping("/login")
    public String loginPage() {
        log.debug("Accès à la page de login");
        return "login"; // Retourne le nom de la vue Thymeleaf (login.html)
    }

    /**
     * Gère les requêtes GET vers "/register" et affiche le formulaire d'inscription.
     * Un DTO vide {@link UserRegistrationDto} est ajouté au modèle pour le binding du formulaire.
     *
     * @param model L'objet Model de Spring pour passer des données à la vue.
     * @return Le nom de la vue (template Thymeleaf) pour la page d'inscription ("register").
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        log.debug("Accès au formulaire d'inscription");
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register"; // Retourne register.html
    }

    /**
     * Gère les requêtes POST vers "/register" pour traiter l'inscription d'un nouvel utilisateur.
     * Valide les données d'inscription fournies. Si la validation échoue, retourne au formulaire
     * d'inscription avec les erreurs. Si l'inscription réussit, redirige vers la page de connexion
     * avec un message de succès. En cas d'erreur (par exemple, e-mail déjà utilisé),
     * retourne au formulaire d'inscription avec un message d'erreur.
     *
     * @param userDto Les données d'inscription de l'utilisateur, validées.
     * @param result L'objet {@link BindingResult} qui contient les résultats de la validation.
     * @param redirectAttributes Utilisé pour ajouter des attributs flash pour les messages lors de la redirection.
     * @return Une chaîne de redirection vers la page de connexion ("/login") en cas de succès,
     *         ou le nom de la vue "register" en cas d'échec de validation ou d'erreur.
     */
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                                      BindingResult result,
                                      RedirectAttributes redirectAttributes) {
        log.info("Tentative d'inscription pour l'email: {}", userDto.getEmail());
        // 1. LA VALIDATION SE PRODUIT ICI AUTOMATIQUEMENT
        // grâce à @Valid sur userDto.
        // Les annotations @NotEmpty, @Size, @Pattern sur les champs de userDto
        if (result.hasErrors()) {
            log.warn("Echec de validation pour l'inscription de {}: {}", userDto.getEmail(), result.getAllErrors());
            return "register"; // Retourne au formulaire avec les erreurs
        }
        try {
            userService.registerNewUser(userDto);
            log.info("Inscription réussie pour: {}", userDto.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Inscription réussie ! Vous pouvez maintenant vous connecter.");
            return "redirect:/login"; // Redirige vers la page de login
        } catch (Exception e) {
            log.error("Erreur lors de l'inscription pour {}: {}", userDto.getEmail(), e.getMessage());
            // Met l'erreur dans le BindingResult pour l'afficher sur le formulaire
            result.rejectValue("email", "error.userDto", e.getMessage());

            return "register"; // Reste sur la page register avec l'erreur affichée
        }
    }
}
