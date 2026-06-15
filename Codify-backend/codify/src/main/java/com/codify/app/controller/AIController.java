package com.codify.app.controller;

import com.codify.app.service.AIService;
import org.springframework.web.bind.annotation.*;

// Controller IA : reçoit les requêtes de ide.js et appelle AIService
@RestController
@RequestMapping("/api/ai")
public class AIController {

    // AIService fait le vrai travail (appel à Groq API)
    private final AIService aiService;

    // Injection via constructeur 
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    // ── Sprint 3 : Analyser le code ──
    // Appelé automatiquement après chaque exécution
    // et aussi pour les questions manuelles du Chat IA
    @PostMapping("/analyser")
    public String analyser(
            @RequestParam String code,
            @RequestParam String niveau,
            @RequestParam(defaultValue = "python") String langage) {
        return aiService.analyserCode(code, niveau, langage);
    }

    // ── Optimiser le code ──
    // Appelé automatiquement après chaque exécution
    // Propose une version plus simple/performante selon le niveau
    @PostMapping("/optimiser")
    public String optimiser(
            @RequestParam String code,
            @RequestParam String niveau,
            @RequestParam(defaultValue = "python") String langage) {
        return aiService.optimiserCode(code, niveau, langage);
    }

    // ── Sprint 4 : Transformer le code ──
    // Appelé quand l'utilisateur clique "Transformer"
    // Transforme de "source" vers "cible" et explique les différences
    @PostMapping("/transformer")
    public String transformer(
            @RequestParam String code,
            @RequestParam String source,  // Langage original (ex: python)
            @RequestParam String cible,   // Langage cible (ex: java)
            @RequestParam(defaultValue = "debutant") String niveau) {
        return aiService.transformerEtComparer(code, source, cible, niveau);
    }
}