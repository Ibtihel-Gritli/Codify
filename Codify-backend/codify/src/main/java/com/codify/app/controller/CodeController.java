package com.codify.app.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

// @RestController : retourne du JSON (pas de pages HTML)
// @RequestMapping : toutes les routes commencent par /api/code
@RestController
@RequestMapping("/api/code")
public class CodeController {

    // ── Sprint 2 : Simuler l'exécution du code ──
    // Route : POST /api/code/executer
    // Appelé par ide.js quand l'utilisateur clique "Exécuter"
    @PostMapping("/executer")
    public Map<String, String> executerCode(
            @RequestParam String code,       // Le code écrit par l'utilisateur
            @RequestParam String langage,    // "python", "java" ou "c"
            @RequestParam(defaultValue = "débutant") String niveau) { // Niveau utilisateur

        // Map = dictionnaire clé→valeur, sera converti en JSON automatiquement
        Map<String, String> response = new HashMap<>();

        // Vérifier que le code n'est pas vide
        if (code == null || code.trim().isEmpty()) {
            response.put("resultat", "⚠️ Le code est vide !");
            return response;
        }

        String resultat;

        // Choisir le comportement selon le langage
        switch (langage.toLowerCase()) {
            case "python":
                // Python : on simule l'exécution des print()
                resultat = simulerPython(code);
                break;
            case "java":
                // Java : on simule une compilation réussie
                resultat = "✅ Compilation Java réussie.\n> Programme exécuté avec succès.";
                break;
            case "c":
                // C : on simule une compilation réussie
                resultat = "✅ Compilation C réussie.\n> Programme exécuté avec succès.";
                break;
            default:
                resultat = "❌ Langage non supporté.";
        }

        // Ajouter le résultat et le message IA dans la réponse JSON
        response.put("resultat", resultat);
        response.put("messageIA", genererMessageIA(resultat, niveau));
        return response;
    }

    // Simuler l'exécution Python en lisant les print()
    // Exemple : print("bonjour") → affiche "> bonjour"
    private String simulerPython(String code) {
        List<String> sorties = new ArrayList<>();

        // Parcourir chaque ligne du code
        for (String ligne : code.split("\\r?\\n")) {
            ligne = ligne.trim();

            // Si la ligne est un print() → extraire le contenu
            if (ligne.startsWith("print(") && ligne.endsWith(")")) {
                // Enlever "print(" au début et ")" à la fin
                String contenu = ligne.substring(6, ligne.length() - 1);
                sorties.add("> " + contenu);
            }
        }

        // Si aucun print trouvé
        if (sorties.isEmpty()) {
            return "✅ Code Python exécuté (aucune sortie print détectée).";
        }

        // Retourner toutes les sorties
        return "✅ Exécution Python :\n" + String.join("\n", sorties);
    }

    // Générer un message d'encouragement selon le niveau
    private String genererMessageIA(String resultat, String niveau) {
        switch (niveau.toLowerCase()) {
            case "débutant":
            case "debutant":
                return "🟢 Débutant : Voici votre résultat !\n" + resultat
                        + "\nN'hésitez pas à poser des questions sur chaque ligne.";
            case "intermédiaire":
            case "intermediaire":
                return "🟡 Intermédiaire : Résultat obtenu. Pensez à optimiser votre code.";
            case "avancé":
            case "avance":
                return "🔴 Avancé : Code exécuté. Analysez les optimisations possibles.";
            default:
                return "Résultat : " + resultat;
        }
    }

    // ── Sprint 4 : Transformation via IA ──
    // Route : POST /api/code/transformer
    // Note : la vraie transformation est faite par AIService
    // Ce controller retourne juste un message d'attente
    @PostMapping("/transformer")
    public Map<String, String> transformerCode(
            @RequestParam String code,
            @RequestParam String cible,                              // Langage cible
            @RequestParam(defaultValue = "python") String source) { // Langage source

        Map<String, String> response = new HashMap<>();

        if (code == null || code.trim().isEmpty()) {
            response.put("resultat", "⚠️ Le code est vide !");
            return response;
        }

        // La transformation réelle est faite par /api/ai/transformer
        response.put("resultat", "⏳ Transformation en cours via l'IA...");
        response.put("source", source);
        response.put("cible", cible);
        return response;
    }
}