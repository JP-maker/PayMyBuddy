package com.openclassrooms.paymybuddy.controller;


import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        log.debug("Affichage de la page de profil pour {}", userEmail);
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        model.addAttribute("user", currentUser);
        log.debug("Utilisateur connecté : {}", currentUser.getEmail());
        return "profile"; // retourne profile.html
    }

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

    // Ajouter ici plus tard la logique pour changer le mot de passe si nécessaire
}