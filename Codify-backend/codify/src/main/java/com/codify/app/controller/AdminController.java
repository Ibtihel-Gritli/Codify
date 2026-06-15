package com.codify.app.controller;

import com.codify.app.model.User;
import com.codify.app.repository.DefiRepository;
import com.codify.app.repository.HistoriqueRepository;
import com.codify.app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

// Controller admin : gère les pages d'administration
@Controller
@RequestMapping("/admin")
public class AdminController {

    // Identifiants admin fixes (simple pour un projet académique)
    private static final String ADMIN_EMAIL = "admin@codify.com";
    private static final String ADMIN_MDP   = "admin123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HistoriqueRepository historiqueRepository;

    @Autowired
    private DefiRepository defiRepository;

    // ── Page login admin ──
    // Affiche le formulaire de connexion admin
    @GetMapping("/login")
    public String loginPage() {
        return "admin-login";
    }

    // ── Traitement connexion admin ──
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String mdp,
            Model model,
            HttpSession session) {

        // Vérifier les identifiants admin
        if (ADMIN_EMAIL.equals(email) && ADMIN_MDP.equals(mdp)) {
            // Sauvegarder le rôle admin dans la session
            session.setAttribute("admin", true);
            return "redirect:/admin/dashboard";
        }

        // Identifiants incorrects
        model.addAttribute("erreur", "Email ou mot de passe incorrect !");
        return "admin-login";
    }

    // ── Dashboard admin ──
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        // Vérifier que l'admin est connecté
        Boolean admin = (Boolean) session.getAttribute("admin");
        if (admin == null || !admin) {
            return "redirect:/admin/login";
        }

        // Récupérer tous les utilisateurs
        List<User> tousUsers = userRepository.findAll();

        // Séparer par niveau
        List<User> debutants      = filtrerParNiveau(tousUsers, "debutant");
        List<User> intermediaires = filtrerParNiveau(tousUsers, "intermediaire");
        List<User> avances        = filtrerParNiveau(tousUsers, "avance");

        // Trier chaque groupe par points décroissants
        debutants.sort((a, b)      -> b.getPoints() - a.getPoints());
        intermediaires.sort((a, b) -> b.getPoints() - a.getPoints());
        avances.sort((a, b)        -> b.getPoints() - a.getPoints());

        // Statistiques globales
        long totalUsers    = userRepository.count();
        long totalDefis    = defiRepository.count();
        long defisTermines = defiRepository.findByStatutOrderByIdDesc("termine").size();

        // Envoyer les données à la page HTML
        model.addAttribute("debutants",      debutants);
        model.addAttribute("intermediaires", intermediaires);
        model.addAttribute("avances",        avances);
        model.addAttribute("totalUsers",     totalUsers);
        model.addAttribute("totalDefis",     totalDefis);
        model.addAttribute("defisTermines",  defisTermines);
        model.addAttribute("tousUsers",      tousUsers);

        return "admin-dashboard";
    }

    // ── Déconnexion admin ──
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("admin");
        return "redirect:/admin/login";
    }

    // ── Supprimer un utilisateur ──
    @PostMapping("/supprimer/{id}")
    public String supprimerUser(@PathVariable Long id, HttpSession session) {
        Boolean admin = (Boolean) session.getAttribute("admin");
        if (admin == null || !admin) return "redirect:/admin/login";

        userRepository.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    // ── Méthode utilitaire : filtrer par niveau ──
    private List<User> filtrerParNiveau(List<User> users, String niveau) {
        List<User> resultat = new ArrayList<>();
        for (User u : users) {
            String niveauUser = u.getLevel() != null
                ? u.getLevel().toLowerCase()
                    .replace("é","e").replace("è","e")
                : "";
            if (niveauUser.equals(niveau)) {
                resultat.add(u);
            }
        }
        return resultat;
    }
}