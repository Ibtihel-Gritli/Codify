# Codify (Version 0)

**Application web intelligente d'apprentissage de la programmation, assistée par intelligence artificielle.**

> **Statut :** Version 0, en cours d'amélioration.

Codify permet d'analyser, corriger, optimiser, transformer et comparer du code source, avec des explications pédagogiques adaptées au niveau de chaque utilisateur. Un assistant IA interactif (chatbot) accompagne l'apprenant pour comprendre son code, identifier ses erreurs et améliorer ses solutions.

**Objectif :** offrir un environnement interactif où l'on apprend, on comprend ses erreurs et on progresse efficacement en programmation.

**Fonctionnalités principales**
- Éditeur de code multi-langages avec exécution.
- Analyse intelligente du code, avec correction et optimisation.
- Transformation et comparaison entre différents langages de programmation.
- Exercices personnalisés selon le niveau de l'utilisateur.
- Défis et hackathons chronométrés.
- Système de points, classement et progression.
- Explications détaillées pour développer la logique algorithmique.

> Projet de fin d'année (PFA), Licence en Génie Logiciel et Systèmes d'Information (GLSI), Faculté des Sciences de Monastir (FSM), année universitaire 2025-2026.
> Encadrant : M. Mahmoud Adnen.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3)
![Groq](https://img.shields.io/badge/IA-Groq%20%7C%20Llama%203.3%2070B-black)

---

## Table des matières

1. [Pourquoi Codify](#pourquoi-codify)
2. [Fonctionnalités](#fonctionnalités)
3. [Technologies](#technologies)
4. [Architecture](#architecture)
5. [Utilisation](#utilisation)
6. [Méthodologie](#méthodologie)
7. [Limites actuelles et perspectives](#limites-actuelles-et-perspectives)
8. [Auteure](#auteure)

---

## Pourquoi Codify

Apprendre à programmer est difficile quand on ne comprend pas ses erreurs. Les plateformes comme LeetCode ou HackerRank répondent seulement « réussi » ou « échoué », sans expliquer pourquoi. Codify répond à ce manque.

| Fonctionnalité | LeetCode | HackerRank | Codify |
|---|:---:|:---:|:---:|
| Exécution de code dans le navigateur | Limitée | Limitée | Oui |
| Correction des erreurs par IA | Non | Non | Oui |
| Explications adaptées au niveau | Non | Non | Oui (3 niveaux) |
| Transformation de code entre langages | Non | Non | Oui (Python, Java, C) |
| Exercices personnalisés par niveau | Oui | Oui | Oui, générés par IA |
| Classement et gamification | Oui | Oui | Oui, avec récompenses |
| Défis à deux joueurs | Non | Limité | Oui, en temps réel |
| Recommandation de langage par IA | Non | Non | Oui |

---

## Fonctionnalités

### Comptes et niveaux
- Inscription et connexion, avec choix du niveau : **Débutant**, **Intermédiaire** ou **Avancé**.
- Tout le contenu (explications de l'IA, exercices, tests) s'adapte au niveau choisi.
- Tableau de bord personnel : points, niveau, exercices complétés, classement et historique détaillé (titre, niveau, points, temps, complexité, résultat).

### Éditeur de code
- Éditeur intégré avec trois langages : **Python**, **Java** et **C**.
- Terminal de résultat intégré.
- Analyse automatique du code après chaque exécution.

### Assistant IA pédagogique
- **Analyse** en 5 sections : explication du code, erreurs détectées (avec numéro de ligne), code corrigé commenté, méthode alternative, conseils personnalisés.
- **Optimisation** : une version plus efficace du code, avec explications.
- **Transformation** de code d'un langage à un autre, avec les différences de syntaxe, de logique et de performance.
- **Chat IA** pour poser des questions libres sur son code.

### Exercices et progression
- Génération de 6 exercices adaptés au niveau et au langage choisis.
- Chronomètre pour chaque exercice et extraction de la complexité algorithmique de la solution.
- Points selon la qualité de la solution : complète, partielle (moitié des points) ou incorrecte (0 point). Un bonus de rapidité multiplie les points par 1,5.
- **Test de validation** après 20 exercices : 5 questions chronométrées (30, 20 ou 15 minutes selon le niveau). Avec 4 bonnes réponses sur 5, l'utilisateur passe au niveau supérieur.

### Hackathons en temps réel
- Deux joueurs s'affrontent sur le même problème algorithmique généré par l'IA.
- Communication instantanée avec **WebSocket (STOMP sur SockJS)**.
- Chronomètres synchronisés grâce à un horodatage unique fourni par le serveur.
- Scores de l'adversaire visibles en direct.
- Évaluation comparative des deux solutions par l'IA (complexité, optimisation, lisibilité). Le gagnant reçoit 50 points.

### Administration
- Espace administrateur sécurisé par session côté serveur.
- Consultation des trois classements (Débutant, Intermédiaire, Avancé), du bilan des hackathons et gestion des comptes utilisateurs.

---

## Technologies

| Couche | Technologie | Rôle |
|---|---|---|
| Backend | Java 17, Spring Boot 3.2.0 | Routes HTTP, sessions, logique métier |
| Accès aux données | Spring Data JPA | Communication avec MySQL |
| Temps réel | Spring WebSocket | Hackathons (STOMP sur SockJS) |
| Frontend | Thymeleaf, Bootstrap 5.3.0, JavaScript (ES6) | Pages et interactions |
| Base de données | MySQL 8.0 | Utilisateurs, historique, hackathons |
| Intelligence artificielle | API Groq (`llama-3.3-70b-versatile`) | Analyse, correction, optimisation, transformation, exercices |

**Outils de développement :** GitHub, Spring Tools Suite 4, MySQL Workbench, Postman.

---

## Architecture

### Architecture logique (MVC)
- **Modèle** : entités JPA (`User`, `HistoriqueExercice`, `Defi`), services métier et repositories.
- **Vue** : pages Thymeleaf et Bootstrap, et code JavaScript pour les interactions et les appels aux API.
- **Contrôleur** : contrôleurs Spring Boot qui renvoient des pages HTML ou du JSON (API REST).

### Architecture physique (3 tiers)
```
Navigateur  ──HTTP / WebSocket──▶  Serveur Spring Boot (port 9090)  ──JPA──▶  MySQL (codify_db)
                                            │
                                            └──HTTPS──▶  API Groq (IA)
```

### Base de données
Trois tables : `users`, `historique_exercices` et `defis`.
Un utilisateur possède zéro ou plusieurs entrées d'historique (supprimées avec lui). Un hackathon (`Defi`) implique un ou deux utilisateurs.

---

## Utilisation

1. Créez un compte et choisissez votre niveau.
2. Ouvrez l'éditeur, choisissez un langage, écrivez votre code et cliquez sur **Exécuter**. L'assistant IA analyse le code et affiche ses conseils.
3. Utilisez **Transformer** pour convertir votre code vers un autre langage, ou le **chat** pour poser une question.
4. Allez dans **Exercices** pour générer une série de 6 exercices et gagner des points.
5. Après 20 exercices, passez le **test de niveau** pour monter de niveau.
6. Dans **Défis**, créez ou rejoignez un hackathon pour affronter un autre utilisateur.

---

## Méthodologie

Le projet suit le framework **Scrum**, avec un seul développeur qui tient les deux rôles (Scrum Master et équipe de développement). Il comprend **21 user stories**, réparties en **5 sprints** et **3 releases**.

| Release | Sprints | Contenu |
|---|---|---|
| Release 1 | Sprint 1 et 2 | Gestion des utilisateurs, authentification, éditeur de code et exécution |
| Release 2 | Sprint 3 et 4 | Analyse IA et correction, transformation de code et chat IA |
| Release 3 | Sprint 5 | Exercices, test de niveau, hackathons, gamification et panneau administrateur |

Le rapport complet (analyse des besoins, diagrammes UML de cas d'utilisation, de classes et de séquence, interfaces) est disponible dans le dossier `docs/`.

---

## Limites actuelles et perspectives

**Limite connue :** pour Java et C, la compilation est **simulée**. Seul le code Python est exécuté ligne par ligne.

**Pistes d'amélioration :**
- Intégrer un vrai compilateur en ligne (API Judge0).
- Développer une application mobile (React Native).
- Ajouter un système de badges et de récompenses.
- Proposer un mode équipe pour les hackathons.
- Prendre en charge d'autres langages (C++, JavaScript, PHP).

---

## Auteure

**Ibtihel Gritli**, étudiante en Licence GLSI à la Faculté des Sciences de Monastir.

- Portfolio : [ibtihel-gritli.github.io/Ibtihel-Gritl](https://ibtihel-gritli.github.io/Ibtihel-Gritl/)
- GitHub : [Ibtihel-Gritli](https://github.com/Ibtihel-Gritli)
- LinkedIn : [ibtihelle-gritli](https://www.linkedin.com/in/ibtihelle-gritli)
- Email : ibtihellegritli@gmail.com
