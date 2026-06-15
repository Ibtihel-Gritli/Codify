package com.codify.app.controller;

import com.codify.app.model.HistoriqueExercice;
import com.codify.app.model.User;
import com.codify.app.repository.HistoriqueRepository;
import com.codify.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private HistoriqueRepository historiqueRepository;
    // ── Page d'accueil ──
    // Quand l'utilisateur va sur localhost:9090/
    // Spring retourne le fichier index.html
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // ── Page connexion ──
    // @RequestParam : récupère le paramètre "tab" dans l'URL
    // Exemple : /login?tab=inscription
    // defaultValue="connexion" : si pas de paramètre, on affiche l'onglet connexion
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(defaultValue = "connexion") String tab,
            Model model) {
        // Envoyer "tab" à la page HTML pour savoir quel onglet afficher
        model.addAttribute("tab", tab);
        return "login";
    }

    // ── Page inscription ──
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ── Traitement inscription ──
    // @PostMapping : reçoit les données du formulaire HTML (méthode POST)
    // @RequestParam : récupère chaque champ du formulaire
    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String level,
            Model model) {

        // Vérifier si l'email est déjà utilisé
        if (userService.existsByEmail(email)) {
            // Envoyer le message d'erreur à la page HTML
            model.addAttribute("error", "Email déjà utilisé !");
            return "register"; // Rester sur la page inscription
        }

        // Créer un nouvel utilisateur et remplir ses informations
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setLevel(level);

        // Sauvegarder dans MySQL
        userService.saveUser(user);

        // Rediriger vers la page connexion
        return "redirect:/login";
    }

    // ── Traitement connexion ──
    // HttpSession : permet de mémoriser l'utilisateur connecté
    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        // Vérifier les identifiants
        User user = userService.loginUser(email, password);

        if (user != null) {
            // Sauvegarder l'utilisateur dans la session
            // Cela permet de savoir qui est connecté sur toutes les pages
            session.setAttribute("user", user);
            return "redirect:/dashboard"; // Aller au tableau de bord
        }

        // Identifiants incorrects → afficher l'erreur
        model.addAttribute("error", "Email ou mot de passe incorrect !");
        return "login";
    }

    // ── Tableau de bord ──
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Récupérer l'historique des exercices
        List<HistoriqueExercice> historique =
            historiqueRepository.findByUserIdOrderByIdDesc(user.getId());

        model.addAttribute("username", user.getUsername());
        model.addAttribute("level", user.getLevel());
        model.addAttribute("niveau", user.getLevel());
        model.addAttribute("points", user.getPoints());
        model.addAttribute("exercicesCompletes", user.getExercicesCompletes());
        model.addAttribute("historique", historique);
        return "dashboard";
    }

    // ── Page éditeur de code ──
    @GetMapping("/ide")
    public String ide(HttpSession session) {
        User user = (User) session.getAttribute("user");

        // Protection : si pas connecté → login
        if (user == null) {
            return "redirect:/login";
        }
        return "ide";
    }

    // ── API JSON : infos utilisateur pour ide.js ──
    // @ResponseBody : retourne du JSON au lieu d'une page HTML
    // ide.js appelle cette URL pour connaître le niveau de l'utilisateur
    @GetMapping("/api/user/info")
    @ResponseBody
    public Map<String, Object> getUserInfo(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> info = new HashMap<>();

        if (user != null) {
            info.put("username", user.getUsername());
            info.put("level", user.getLevel());
            info.put("points", user.getPoints());
        }
        return info; // Retourne : {"username":"Ibtihel","level":"debutant","points":0}
    }

    // ── Déconnexion ──
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Effacer toutes les données de session
        return "redirect:/"; // Retourner à l'accueil
    }
}