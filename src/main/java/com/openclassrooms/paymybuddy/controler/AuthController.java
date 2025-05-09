package com.openclassrooms.paymybuddy.controler;

import com.openclassrooms.paymybuddy.dto.UserRegistrationDto;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Retourne le nom de la vue Thymeleaf (login.html)
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register"; // Retourne register.html
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                                      BindingResult result,
                                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register"; // Retourne au formulaire avec les erreurs
        }
        try {
            userService.registerNewUser(userDto);
            redirectAttributes.addFlashAttribute("successMessage", "Inscription réussie ! Vous pouvez maintenant vous connecter.");
            return "redirect:/login"; // Redirige vers la page de login
        } catch (Exception e) {
            // Met l'erreur dans le BindingResult pour l'afficher sur le formulaire
            result.rejectValue("email", "error.userDto", e.getMessage());
            // Ou utiliser redirectAttributes si on redirige vers register?error=true
            // redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // return "redirect:/register?error";
            return "register"; // Reste sur la page register avec l'erreur affichée
        }
    }
}
