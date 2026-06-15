package com.codify.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class ExerciceService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final String groqUrl = "https://api.groq.com/openai/v1/chat/completions";

    // Méthode commune pour appeler Groq
    private String appelGroq(String prompt) {
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
            body.put("max_tokens", 2000);
            body.put("temperature", 0.5);

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

    // Générer une série de 3 exercices selon le niveau
    public String genererSerie(String niveau, String langage) {
        String style = getStyleNiveau(niveau);

        String prompt = style + "\n\n"
            + "Génère exactement 6 VRAIS problèmes de programmation en "
            + langage.toUpperCase() + " pour le niveau " + niveau + ".\n\n"
            + "IMPORTANT :\n"
            + "- Inspire-toi de LeetCode, HackerRank, Codeforces, Codewars\n"
            + "- Problèmes DIFFÉRENTS à chaque génération (utilise random seed)\n"
            + "- Pour DÉBUTANT : problèmes simples mais RÉELS (manipulation strings, arrays, math)\n"
            + "- Pour INTERMÉDIAIRE : structures de données, algorithmes de tri/recherche\n"
            + "- Pour AVANCÉ : graphes, DP, arbres, algorithmes complexes\n"
            + "- Chaque problème doit avoir des exemples d'entrée/sortie CONCRETS\n\n"
            + "Format EXACT pour chaque problème :\n\n"
            + "EXERCICE 1\n"
            + "Titre : [titre du problème]\n"
            + "Difficulté : [Easy/Medium/Hard selon le niveau]\n"
            + "Problème : [description claire et précise]\n"
            + "Exemple 1 : Entrée: [valeur] → Sortie: [valeur]\n"
            + "Exemple 2 : Entrée: [valeur] → Sortie: [valeur]\n"
            + "Contraintes : [contraintes du problème]\n\n"
            + "EXERCICE 2\n[même format]\n\n"
            + "EXERCICE 3\n[même format]\n\n"
            + "EXERCICE 4\n[même format]\n\n"
            + "EXERCICE 5\n[même format]\n\n"
            + "EXERCICE 6\n[même format]";

        return appelGroq(prompt);
    }

    // Corriger la réponse de l'utilisateur
    public String corrigerReponse(String exercice, String reponse,
                                   String niveau, String langage) {
        String style = getStyleNiveau(niveau);

        String prompt = style + "\n\n"
            + "Exercice posé :\n" + exercice + "\n\n"
            + "Réponse de l'utilisateur :\n```" + langage + "\n"
            + reponse + "\n```\n\n"
            + "Évalue cette réponse selon ce plan :\n\n"
            + "RESULTAT\n"
            + "La réponse est-elle correcte ? Oui/Non/Partielle\n\n"
            + "EXPLICATION\n"
            + "Explique pourquoi la réponse est correcte ou incorrecte.\n"
            + "Adapte l'explication au niveau " + niveau + ".\n\n"
            + "SOLUTION CORRECTE\n"
            + "Donne la solution complète et correcte en " + langage + ".\n\n"
            + "CONSEIL\n"
            + "Un conseil pour progresser adapté au niveau " + niveau + ".";

        return appelGroq(prompt);
    }

    // Générer un test de validation (5 questions)
    public String genererTest(String niveau, String langage) {
        String style = getStyleNiveau(niveau);

        String prompt = style + "\n\n"
            + "Génère un test de validation de 5 questions en "
            + langage.toUpperCase() + " pour le niveau " + niveau + ".\n\n"
            + "Ce test détermine si l'utilisateur peut passer au niveau supérieur.\n\n"
            + "Format EXACT :\n\n"
            + "TEST DE VALIDATION - NIVEAU " + niveau.toUpperCase() + "\n\n"
            + "QUESTION 1\n"
            + "[Question ou exercice à résoudre]\n\n"
            + "QUESTION 2\n"
            + "[Question ou exercice]\n\n"
            + "QUESTION 3\n"
            + "[Question ou exercice]\n\n"
            + "QUESTION 4\n"
            + "[Question ou exercice]\n\n"
            + "QUESTION 5\n"
            + "[Question ou exercice]";

        return appelGroq(prompt);
    }

    // Évaluer le test de validation
    public String evaluerTest(String test, String reponse, String niveau) {
        String prompt = "Tu es un évaluateur strict mais bienveillant.\n\n"
            + "Test posé :\n" + test + "\n\n"
            + "Réponses de l'utilisateur :\n" + reponse + "\n\n"
            + "Évalue ces réponses et dis clairement :\n\n"
            + "SCORE\n"
            + "[X/5 questions correctes]\n\n"
            + "VERDICT\n"
            + "Si 4 ou 5/5 → écris 'RÉUSSI : Félicitations ! Vous passez au niveau supérieur.'\n"
            + "Si moins de 4/5 → écris 'ÉCHOUÉ : Continuez à pratiquer au niveau " + niveau + ".'\n\n"
            + "DETAILS\n"
            + "Explique brièvement pour chaque question si c'est correct ou non.\n\n"
            + "CONSEIL FINAL\n"
            + "Un conseil personnalisé pour l'utilisateur.";

        return appelGroq(prompt);
    }

    // Style de l'IA selon le niveau
    private String getStyleNiveau(String niveau) {
        switch (niveau.toLowerCase()) {
            case "debutant": case "débutant":
                return "Tu es un professeur patient pour débutants. "
                     + "Utilise des exercices simples avec variables, boucles et fonctions. "
                     + "Sois très encourageant.";
            case "intermediaire": case "intermédiaire":
                return "Tu es un assistant pour programmeurs intermédiaires. "
                     + "Utilise des exercices sur structures de données, algorithmes O(n). "
                     + "Sois précis et pédagogique.";
            default:
                return "Tu es un expert pour programmeurs avancés. "
                     + "Utilise des exercices complexes : arbres, graphes, DP, optimisation. "
                     + "Sois technique et rigoureux.";
        }
    }
}