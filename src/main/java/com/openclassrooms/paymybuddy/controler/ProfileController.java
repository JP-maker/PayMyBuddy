package com.openclassrooms.paymybuddy.controler;


import com.openclassrooms.paymybuddy.model.User;
import com.openclassrooms.paymybuddy.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        model.addAttribute("user", currentUser);
        return "profile"; // retourne profile.html
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(required = false) String username, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        try {
            userService.updateUserProfile(userEmail, username);
            redirectAttributes.addFlashAttribute("profileSuccess", "Profil mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("profileError", "Erreur lors de la mise à jour : " + e.getMessage());
        }
        return "redirect:/profile";
    }

    // Ajouter ici plus tard la logique pour changer le mot de passe si nécessaire
}