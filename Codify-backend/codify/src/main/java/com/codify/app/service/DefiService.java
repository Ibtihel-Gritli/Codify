package com.codify.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class DefiService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final String groqUrl = "https://api.groq.com/openai/v1/chat/completions";

    // Générer un vrai problème de défi
    public String genererProblemeDefi(String niveau, String langage) {
        String difficulte;
        switch (niveau.toLowerCase()) {
            case "debutant": case "débutant":
                difficulte = "Easy — inspiré de LeetCode Easy";
                break;
            case "intermediaire": case "intermédiaire":
                difficulte = "Medium — inspiré de LeetCode Medium";
                break;
            default:
                difficulte = "Hard — inspiré de LeetCode Hard / Codeforces";
        }

        String prompt = "Génère UN seul vrai problème de programmation compétitive "
            + "en " + langage.toUpperCase() + ".\n\n"
            + "Niveau : " + difficulte + "\n\n"
            + "IMPORTANT :\n"
            + "- C'est un défi entre 2 programmeurs en temps réel\n"
            + "- Problème ORIGINAL et PRÉCIS avec exemples concrets\n"
            + "- Inspire-toi de LeetCode, Codeforces, HackerRank\n"
            + "- Le problème doit avoir une solution optimale claire\n\n"
            + "Format EXACT :\n\n"
            + "TITRE DU DÉFI\n"
            + "[titre court et accrocheur]\n\n"
            + "PROBLÈME\n"
            + "[description claire et précise du problème]\n\n"
            + "EXEMPLES\n"
            + "Entrée 1 : [valeur] → Sortie 1 : [valeur]\n"
            + "Entrée 2 : [valeur] → Sortie 2 : [valeur]\n\n"
            + "CONTRAINTES\n"
            + "[contraintes du problème]\n\n"
            + "INDICE\n"
            + "[un petit indice sur l'approche à utiliser]";

        return appelGroq(prompt, 1000);
    }

    // Évaluer la solution soumise
    public Map<String, Object> evaluerSolution(
            String probleme, String solution,
            String niveau, String langage,
            long tempsSecondes, int dureeTotal) {

        String prompt = "Tu es un juge de compétition de programmation strict.\n\n"
            + "Problème posé :\n" + probleme + "\n\n"
            + "Solution soumise en " + langage.toUpperCase() + " :\n"
            + "```" + langage + "\n" + solution + "\n```\n\n"
            + "Évalue selon ces critères STRICTS :\n\n"
            + "1. CORRECTION\n"
            + "   La solution est-elle correcte ? Oui/Non/Partielle\n"
            + "   Vérifie avec les exemples donnés.\n\n"
            + "2. COMPLEXITE\n"
            + "   Quelle est la complexité temporelle ? O(?)\n"
            + "   Quelle est la complexité spatiale ? O(?)\n\n"
            + "3. OPTIMISATION\n"
            + "   La solution est-elle optimale ?\n"
            + "   S'il y a mieux, propose la complexité optimale.\n\n"
            + "4. SCORE /100\n"
            + "   Calcule un score sur 100 :\n"
            + "   - Correction : 60 points (0 si incorrect, 30 si partiel, 60 si correct)\n"
            + "   - Complexité optimale : 25 points\n"
            + "   - Code propre et lisible : 15 points\n"
            + "   Écris : SCORE FINAL : XX/100\n\n"
            + "5. VERDICT\n"
            + "   ACCEPTE si score >= 60, REFUSE sinon.";

        String evaluation = appelGroq(prompt, 1000);

        // Extraire le score
        int score = extraireScore(evaluation);

        // Calculer bonus de rapidité
        int bonusRapidite = calculerBonusRapidite(tempsSecondes, dureeTotal * 60L);

        // Score final avec bonus
        int scoreFinal = Math.min(100, score + bonusRapidite);

        // Extraire complexité
        String complexite = extraireComplexite(evaluation);

        // Vérifier si accepté
        boolean accepte = evaluation.toUpperCase().contains("ACCEPTE")
                       || evaluation.toUpperCase().contains("ACCEPTÉ")
                       || score >= 60;

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("evaluation", evaluation);
        resultat.put("score", scoreFinal);
        resultat.put("scoreSansBonus", score);
        resultat.put("bonusRapidite", bonusRapidite);
        resultat.put("complexite", complexite);
        resultat.put("accepte", accepte);
        return resultat;
    }

    // Bonus rapidité : plus vite = plus de points bonus
    private int calculerBonusRapidite(long tempsSecondes, long dureeTotaleSecondes) {
        if (dureeTotaleSecondes <= 0) return 0;
        double ratio = (double) tempsSecondes / dureeTotaleSecondes;
        if (ratio <= 0.25) return 20;      // Moins de 25% du temps → +20 pts
        if (ratio <= 0.50) return 10;      // Moins de 50% du temps → +10 pts
        if (ratio <= 0.75) return 5;       // Moins de 75% du temps → +5 pts
        return 0;
    }

    private int extraireScore(String texte) {
        try {
            int idx = texte.indexOf("SCORE FINAL : ");
            if (idx == -1) idx = texte.indexOf("SCORE FINAL:");
            if (idx != -1) {
                String apres = texte.substring(idx).replaceAll("[^0-9]", " ").trim();
                String[] parts = apres.split("\\s+");
                if (parts.length > 0) {
                    return Math.min(100, Integer.parseInt(parts[0]));
                }
            }
        } catch (Exception e) {}
        return 50; // Score par défaut
    }

    private String extraireComplexite(String texte) {
        if (texte.contains("O(1)")) return "O(1)";
        if (texte.contains("O(log n)")) return "O(log n)";
        if (texte.contains("O(n log n)")) return "O(n log n)";
        if (texte.contains("O(n²)") || texte.contains("O(n^2)")) return "O(n²)";
        if (texte.contains("O(n)")) return "O(n)";
        return "O(n)";
    }

    private String appelGroq(String prompt, int maxTokens) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + groqApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.3-70b-versatile");
            body.put("messages", List.of(message));
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.4);

            HttpEntity<Map<String, Object>> requete = new HttpEntity<>(body, headers);
            ResponseEntity<String> reponse = restTemplate.postForEntity(
                groqUrl, requete, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode racine = mapper.readTree(reponse.getBody());
            return racine.path("choices").get(0)
                         .path("message").path("content").asText();
        } catch (Exception e) {
            return "⚠️ Erreur : " + e.getMessage();
        }
    }
 // Comparer les 2 solutions et désigner le gagnant
 // Appelé quand les 2 joueurs ont soumis OU quand le temps est écoulé
 public String comparerDeuxSolutions(
         String probleme,
         String solution1, String joueur1, int score1,
         String solution2, String joueur2, int score2,
         long temps1, long temps2) {

     String prompt = "Tu es un juge expert de hackathon. Compare ces deux solutions.\n\n"
         + "Problème posé :\n" + probleme + "\n\n"
         + "=== SOLUTION DE " + joueur1 + " (Score soumis : " + score1 + "/100) ===\n"
         + solution1 + "\n\n"
         + "=== SOLUTION DE " + joueur2 + " (Score soumis : " + score2 + "/100) ===\n"
         + solution2 + "\n\n"
         + "Analyse comparative OBLIGATOIRE :\n\n"
         + "COMPLEXITE " + joueur1 + "\n"
         + "Complexité temporelle et spatiale de la solution 1.\n\n"
         + "COMPLEXITE " + joueur2 + "\n"
         + "Complexité temporelle et spatiale de la solution 2.\n\n"
         + "OPTIMISATION\n"
         + "Quelle solution est la plus optimisée ? Pourquoi ?\n\n"
         + "VERDICT FINAL\n"
         + "Qui gagne sur la base de : correction + complexité + optimisation ?\n"
         + "Écris clairement : GAGNANT : " + joueur1 + " OU GAGNANT : " + joueur2
         + " OU EGALITE\n\n"
         + "TEMPS\n"
         + joueur1 + " a mis " + (temps1/60) + "min " + (temps1%60) + "s\n"
         + joueur2 + " a mis " + (temps2/60) + "min " + (temps2%60) + "s\n"
         + "En cas d'égalité, le plus rapide gagne.";

     return appelGroq(prompt, 1500);
 }
}