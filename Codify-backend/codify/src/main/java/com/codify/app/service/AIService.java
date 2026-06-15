package com.codify.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

// Service IA : communique avec l'API Groq pour analyser le code
@Service
public class AIService {

    // Lire la clé API depuis application.properties
    // groq.api.key=gsk_...
    @Value("${groq.api.key}")
    private String groqApiKey;

    // URL de l'API Groq (compatible avec le format OpenAI)
    private final String groqUrl = "https://api.groq.com/openai/v1/chat/completions";

    // ══════════════════════════════════════
    // Méthode principale : envoyer une requête à Groq
    // Réutilisée par analyser, optimiser et transformer
    // ══════════════════════════════════════
    private String appelGroq(String prompt, int maxTokens) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Préparer les headers HTTP (authentification + type JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + groqApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construire le message à envoyer à l'IA
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");      // Role : utilisateur
            message.put("content", prompt);   // Contenu : notre prompt

            // Construire le corps de la requête JSON
            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.3-70b-versatile"); // Modèle Groq
            body.put("messages", List.of(message));        // Liste des messages
            body.put("max_tokens", maxTokens);             // Limite de réponse
            body.put("temperature", 0.3);                  // 0=précis, 1=créatif

            // Envoyer la requête POST à Groq
            HttpEntity<Map<String, Object>> requete = new HttpEntity<>(body, headers);
            ResponseEntity<String> reponse = restTemplate.postForEntity(
                groqUrl, requete, String.class
            );

            // Extraire le texte de la réponse JSON
            // Structure JSON : {"choices":[{"message":{"content":"..."}}]}
            ObjectMapper mapper = new ObjectMapper();
            JsonNode racine = mapper.readTree(reponse.getBody());
            return racine.path("choices").get(0)
                         .path("message").path("content").asText();

        } catch (Exception e) {
            return "⚠️ Erreur IA : " + e.getMessage();
        }
    }

    // ── Sprint 3 : Analyser le code ──
    public String analyserCode(String code, String niveau, String langage) {
        String prompt = construirePrompt(code, niveau, langage);
        return appelGroq(prompt, 1500);
    }

    // ── Optimiser le code ──
    public String optimiserCode(String code, String niveau, String langage) {
        String prompt = construirePromptOptimisation(code, niveau, langage);
        return appelGroq(prompt, 1500);
    }

    // ── Sprint 4 : Transformer le code ──
    public String transformerEtComparer(String code, String source,
                                         String cible, String niveau) {
        String prompt = construirePromptTransformation(code, source, cible, niveau);
        return appelGroq(prompt, 2000);
    }

    // ══════════════════════════════════════
    // Prompts : instructions envoyées à l'IA
    // ══════════════════════════════════════

    // Prompt d'analyse : adapté selon le niveau
    private String construirePrompt(String code, String niveau, String langage) {
        // Définir le "rôle" de l'IA selon le niveau
        String tonIA;
        String styleErreurs;
        String styleConseils;

        switch (niveau.toUpperCase()) {
            case "DEBUTANT": case "DÉBUTANT":
                tonIA = "Tu es un professeur très patient pour débutants. "
                      + "INTERDIT : bibliothèques externes, list comprehension. "
                      + "AUTORISÉ : variables, print, if/else, for, while. "
                      + "Sois très encourageant.";
                styleErreurs = "Explique l'erreur comme à un enfant. "
                              + "Format : Ligne X — Erreur — Explication simple";
                styleConseils = "3 conseils très simples. Termine par une phrase de motivation.";
                break;

            case "INTERMEDIAIRE": case "INTERMÉDIAIRE":
                tonIA = "Tu es un assistant pour programmeurs intermédiaires. "
                      + "Tu peux utiliser statistics, math, sorted, enumerate. "
                      + "Parle de complexité O(n) simplement.";
                styleErreurs = "Explique l'impact de l'erreur. "
                              + "Format : Ligne X — Type — Impact — Correction";
                styleConseils = "3 conseils sur bonnes pratiques et structure.";
                break;

            default: // AVANCE
                tonIA = "Tu es un expert pour programmeurs avancés. "
                      + "Analyse complexité, design patterns, optimisations mémoire.";
                styleErreurs = "Liste erreurs avec numéro de ligne et impact performance. "
                              + "Format : Ligne X — Type — Impact — Correction optimale";
                styleConseils = "3 conseils avancés : design patterns, profiling, architecture.";
        }

        // Construire le prompt final
        return tonIA + "\n\n"
             + "Langage : " + langage.toUpperCase() + " | Niveau : " + niveau + "\n\n"
             + "Code soumis :\n```" + langage + "\n" + code + "\n```\n\n"
             + "Réponds selon ce plan :\n\n"
             + "1. EXPLICATION DU CODE\n"
             + "   Explique ce que fait ce code selon le niveau.\n\n"
             + "2. ERREURS DETECTEES\n"
             + "   " + styleErreurs + "\n"
             + "   Si aucune erreur : écris 'Aucune erreur détectée'\n\n"
             + "3. CODE CORRIGE\n"
             + "   Code complet corrigé adapté au niveau.\n\n"
             + "4. METHODE ALTERNATIVE\n"
             + "   Version plus simple ou plus performante selon le niveau.\n\n"
             + "5. CONSEILS\n"
             + "   " + styleConseils;
    }

    // Prompt d'optimisation
    private String construirePromptOptimisation(String code, String niveau, String langage) {
        // Règles selon le niveau
        String regles;
        switch (niveau.toUpperCase()) {
            case "DEBUTANT": case "DÉBUTANT":
                regles = "Utilise UNIQUEMENT les bases. PAS de bibliothèques. "
                       + "Explique chaque ligne simplement.";
                break;
            case "INTERMEDIAIRE": case "INTERMÉDIAIRE":
                regles = "Tu peux utiliser les bibliothèques standard. "
                       + "Explique la complexité O(n) simplement.";
                break;
            default:
                regles = "Utilise les concepts avancés. "
                       + "Analyse et compare la complexité algorithmique.";
        }

        return "Tu es un assistant qui optimise le code pour le niveau " + niveau + ".\n"
             + regles + "\n\n"
             + "Langage : " + langage.toUpperCase() + "\n\n"
             + "Code original :\n```" + langage + "\n" + code + "\n```\n\n"
             + "Réponds selon ce plan :\n\n"
             + "PROBLEME\n"
             + "Explique en 2 lignes ce que fait ce code et son problème.\n\n"
             + "CODE OPTIMISE\n"
             + "Fournis le code optimisé complet adapté au niveau " + niveau + ".\n\n"
             + "POURQUOI C EST MIEUX\n"
             + "Explique simplement pourquoi cette version est meilleure.\n\n"
             + "CONSEIL FINAL\n"
             + "Un conseil court adapté au niveau " + niveau + ".";
    }

    // Prompt de transformation
    private String construirePromptTransformation(String code, String source,
                                                    String cible, String niveau) {
        // Ton selon le niveau
        String ton;
        switch (niveau.toUpperCase()) {
            case "DEBUTANT": case "DÉBUTANT":
                ton = "Explique chaque différence avec des mots très simples et des analogies.";
                break;
            case "INTERMEDIAIRE": case "INTERMÉDIAIRE":
                ton = "Explique les différences de syntaxe et de logique clairement.";
                break;
            default:
                ton = "Analyse les différences de performance, mémoire et paradigmes.";
        }

        return "Tu transformes du code " + source.toUpperCase()
             + " vers " + cible.toUpperCase()
             + " pour un utilisateur de niveau " + niveau + ".\n"
             + ton + "\n\n"
             + "Code original en " + source.toUpperCase() + " :\n"
             + "```" + source + "\n" + code + "\n```\n\n"
             + "Réponds selon ce plan :\n\n"
             + "1. CODE TRANSFORME EN " + cible.toUpperCase() + "\n"
             + "   Transforme EXACTEMENT ce code. Garde la même logique.\n\n"
             + "2. DIFFERENCES DE SYNTAXE\n"
             + "   Format : " + source.toUpperCase() + " utilise X → "
             + cible.toUpperCase() + " utilise Y\n\n"
             + "3. DIFFERENCES DE LOGIQUE\n"
             + "   Gestion mémoire, typage, structure du programme.\n\n"
             + "4. QUEL LANGAGE EST LE PLUS ADAPTE ?\n"
             + "   Pour CE problème, quel langage est le mieux et pourquoi ?\n\n"
             + "5. CONSEILS\n"
             + "   2 conseils pour comprendre cette transformation.";
    }
}