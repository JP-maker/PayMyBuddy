package com.openclassrooms.paymybuddy.controler;

import com.openclassrooms.paymybuddy.dto.AddConnectionDto;
import com.openclassrooms.paymybuddy.model.User;
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

@Controller
public class ConnectionController {

    private final UserService userService;

    public ConnectionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/connections")
    public String connectionsPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userService.findByEmailWithConnections(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        model.addAttribute("connections", currentUser.getConnections());
        model.addAttribute("addConnectionDto", new AddConnectionDto());
        return "connections"; // Retourne connections.html
    }

    @PostMapping("/connections/add")
    public String addConnection(@Valid @ModelAttribute("addConnectionDto") AddConnectionDto addConnectionDto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) { // Ajout Model pour retour erreur

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        if (result.hasErrors()) {
            // Recharger les connexions actuelles pour les réafficher avec le formulaire en erreur
            User currentUser = userService.findByEmailWithConnections(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            model.addAttribute("connections", currentUser.getConnections());
            // Le addConnectionDto avec l'erreur est déjà là via @ModelAttribute
            return "connections"; // Retourne à la page avec l'erreur de validation
        }

        try {
            userService.addConnection(userEmail, addConnectionDto.getFriendEmail());
            redirectAttributes.addFlashAttribute("connectionSuccess", "Ami ajouté avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("connectionError", "Erreur lors de l'ajout : " + e.getMessage());
        }

        return "redirect:/connections"; // Redirige vers la page des connexions
    }
}