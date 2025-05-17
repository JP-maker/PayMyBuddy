package com.openclassrooms.paymybuddy.controller;


import com.openclassrooms.paymybuddy.dto.ChangePasswordDto;
import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur Spring MVC gérant la page de profil de l'utilisateur.
 * Permet à l'utilisateur de visualiser ses informations de profil,
 * de mettre à jour son nom d'utilisateur et de changer son mot de passe.
 */
@Slf4j
@Controller
public class ProfileController {

    private final UserService userService;

    /**
     * Construit une instance de {@code ProfileController} avec le service utilisateur requis.
     *
     * @param userService Le service pour gérer les opérations liées aux utilisateurs.
     */
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gère les requêtes GET vers "/profile" et affiche la page de profil de l'utilisateur connecté.
     * Récupère les informations de l'utilisateur actuellement authentifié et les ajoute au modèle.
     * Prépare également un DTO vide {@link ChangePasswordDto} pour le formulaire de changement de mot de passe.
     *
     * @param model L'objet Model de Spring pour passer des données à la vue (utilisateur, DTO de changement de mot de passe).
     * @return Le nom de la vue (template Thymeleaf) pour la page de profil ("profile").
     * @throws RuntimeException si l'utilisateur actuellement authentifié n'est pas trouvé.
     */
    @GetMapping("/profile")
    public String profilePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        log.debug("Affichage de la page de profil pour {}", userEmail);
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        model.addAttribute("user", currentUser);
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        log.debug("Utilisateur connecté : {}", currentUser.getEmail());
        return "profile";
    }

    /**
     * Gère les requêtes POST vers "/profile/update" pour mettre à jour le nom d'utilisateur du profil.
     * Récupère l'utilisateur authentifié et tente de mettre à jour son nom d'utilisateur.
     * Redirige vers la page de profil avec un message de succès ou d'erreur.
     *
     * @param username Le nouveau nom d'utilisateur à définir. Peut être nul ou vide si non fourni.
     * @param redirectAttributes Utilisé pour ajouter des attributs flash pour les messages lors de la redirection.
     * @return Une chaîne de redirection vers la page de profil ("/profile").
     */
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(required = false) String username, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Tentative de mise à jour du profil pour {}", authentication.getName());
        String userEmail = authentication.getName();
        try {
            userService.updateUserProfile(userEmail, username);
            log.info("Profil mis à jour pour {}", userEmail);
            redirectAttributes.addFlashAttribute("profileSuccess", "Profil mis à jour.");
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du profil pour {}: {}", userEmail, e.getMessage());
            redirectAttributes.addFlashAttribute("profileError", "Erreur lors de la mise à jour : " + e.getMessage());
        }
        return "redirect:/profile";
    }


    /**
     * Gère les requêtes POST vers "/profile/change-password" pour changer le mot de passe de l'utilisateur.
     * Valide le mot de passe actuel, la conformité du nouveau mot de passe et sa confirmation.
     * Si une validation échoue, redirige vers la page de profil avec un message d'erreur approprié.
     * Si toutes les validations passent et le changement est réussi, redirige avec un message de succès.
     *
     * @param changePasswordDto Le DTO {@link ChangePasswordDto} contenant les mots de passe actuel, nouveau et de confirmation, validé.
     * @param result L'objet {@link BindingResult} qui contient les résultats de la validation des annotations sur le DTO.
     * @param redirectAttributes Utilisé pour ajouter des attributs flash pour les messages lors de la redirection.
     * @return Une chaîne de redirection vers la page de profil ("/profile").
     */
    @PostMapping("/profile/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Tentative de changement de mot de passe pour {}", authentication.getName());
        String userEmail = authentication.getName();

        if (!userService.checkCurrentPassword(userEmail, changePasswordDto.getCurrentPassword())) {
            log.error("Mot de passe actuel incorrect pour {}", userEmail);
            redirectAttributes.addFlashAttribute("passwordError", "Mot de passe actuel incorrect.");
            return "redirect:/profile";
        }
        if (result.hasErrors()) {
            log.warn("Echec de validation pour le changement de mot de passe de {}: {}", userEmail, result.getAllErrors());
            redirectAttributes.addFlashAttribute("passwordError", result.getFieldError("newPassword").getDefaultMessage());
            return "redirect:/profile";
        }
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            log.error("Les mots de passe ne correspondent pas");
            redirectAttributes.addFlashAttribute("passwordError", "Les mots de passe ne correspondent pas.");
            return "redirect:/profile";
        }

        try {
            userService.changeUserPassword(userEmail, changePasswordDto.getCurrentPassword(), changePasswordDto.getNewPassword());
            log.info("Mot de passe changé pour {}", userEmail);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Mot de passe changé.");
        } catch (Exception e) {
            log.error("Erreur lors du changement de mot de passe pour {}: {}", userEmail, e.getMessage());
            redirectAttributes.addFlashAttribute("passwordError", "Erreur lors du changement : " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("passwordSuccess", "Mot de passe modifié !");
        return "redirect:/profile";
    }

}