let niveauUtilisateur = "debutant";

window.onload = function () {
    fetch("/api/user/info")
        .then(res => res.json())
        .then(data => {
            if (data.level) {
                niveauUtilisateur = data.level;
                const badge = document.getElementById("badge-niveau");
                if (badge) {
                    const niveaux = {
                        "debutant": "Débutant", "débutant": "Débutant",
                        "intermediaire": "Intermédiaire", "intermédiaire": "Intermédiaire",
                        "avance": "Avancé", "avancé": "Avancé"
                    };
                    badge.innerText = niveaux[data.level] || data.level;
                }
            }
        })
        .catch(() => {});
};

// ── Exécuter (Sprint 2 + 3) ──
function executer() {
    const code = document.getElementById("codeSource").value;
    const langage = document.getElementById("langage").value;

    if (!code.trim()) {
        document.getElementById("resultat").innerText = "⚠️ Écrivez du code avant d'exécuter !";
        return;
    }

    document.getElementById("resultat").innerText = "⏳ Exécution en cours...";
    afficherChargement("messagesIA", "🔍 Analyse en cours...", "info");
    afficherChargement("messagesOptimiser", "⚡ Optimisation en cours...", "info");

    const params = new URLSearchParams();
    params.append("code", code);
    params.append("langage", langage);
    params.append("niveau", niveauUtilisateur);

    fetch("/api/code/executer", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById("resultat").innerText = data.resultat || "Aucun résultat.";
        appelAnalyse(code, langage);
        appelOptimiser(code, langage);
    })
    .catch(() => {
        document.getElementById("resultat").innerText = "❌ Erreur de connexion au serveur.";
    });
}

// ── Analyse automatique (Sprint 3) ──
function appelAnalyse(code, langage) {
    const params = new URLSearchParams();
    params.append("code", code);
    params.append("niveau", niveauUtilisateur);
    params.append("langage", langage);

    fetch("/api/ai/analyser", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.text())
    .then(data => {
        const zone = document.getElementById("messagesIA");
        zone.innerHTML = "";
        afficherReponseCouleurs(data, zone);
    })
    .catch(() => {
        document.getElementById("messagesIA").innerHTML = creerCarte("❌ Erreur IA", "", "error");
    });
}

// ── Optimisation automatique ──
function appelOptimiser(code, langage) {
    const params = new URLSearchParams();
    params.append("code", code);
    params.append("niveau", niveauUtilisateur);
    params.append("langage", langage);

    fetch("/api/ai/optimiser", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.text())
    .then(data => {
        const zone = document.getElementById("messagesOptimiser");
        zone.innerHTML = "";
        afficherReponseCouleurs(data, zone);
    })
    .catch(() => {
        document.getElementById("messagesOptimiser").innerHTML = creerCarte("❌ Erreur optimisation", "", "error");
    });
}

// ── Transformer (Sprint 4) ──
function transformer() {
    const code = document.getElementById("codeSource").value;
    const langageSource = document.getElementById("langage").value;
    const langageCible = document.getElementById("langageCible").value;

    if (!code.trim()) {
        document.getElementById("resultat").innerText = "⚠️ Écrivez du code avant de transformer !";
        return;
    }
    if (langageSource === langageCible) {
        document.getElementById("resultat").innerText = "⚠️ Choisissez un langage cible différent !";
        return;
    }

    document.getElementById("resultat").innerText = "⏳ Transformation en cours...";
    afficherChargement("messagesTransformer", "🔄 Transformation " + langageSource.toUpperCase() + " → " + langageCible.toUpperCase() + "...", "info");

    // Basculer vers onglet Transformer
    document.querySelectorAll('.onglet-ia-btn').forEach(b => b.classList.remove('actif'));
    document.querySelectorAll('.section-ia').forEach(s => s.classList.remove('actif'));
    document.getElementById('ia-transformer').classList.add('actif');
    document.querySelectorAll('.onglet-ia-btn')[2].classList.add('actif');

    const params = new URLSearchParams();
    params.append("code", code);
    params.append("source", langageSource);
    params.append("cible", langageCible);
    params.append("niveau", niveauUtilisateur);

    fetch("/api/ai/transformer", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.text())
    .then(data => {
        // Extraire le code pour la zone résultat
        let codeTransforme = "";
        let dansBloc = false;
        for (let ligne of data.split("\n")) {
            if (ligne.trim().startsWith("```") && !dansBloc) { dansBloc = true; continue; }
            if (ligne.trim().startsWith("```") && dansBloc) { dansBloc = false; continue; }
            if (dansBloc) codeTransforme += ligne + "\n";
        }
        document.getElementById("resultat").innerText = codeTransforme.trim()
            || "✅ Voir l'explication dans le panneau Transformer.";

        // Afficher avec couleurs dans l'onglet Transformer
        const zone = document.getElementById("messagesTransformer");
        zone.innerHTML = "";
        afficherReponseCouleurs(data, zone);
    })
    .catch(() => {
        document.getElementById("resultat").innerText = "❌ Erreur de transformation.";
    });
}

// ── Chat IA ──
function demanderIA() {
    const question = document.getElementById("questionIA").value.trim();
    const code = document.getElementById("codeSource").value;
    const langage = document.getElementById("langage").value;

    if (!question) return;

    document.getElementById("questionIA").value = "";

    // Basculer vers onglet Chat
    document.querySelectorAll('.onglet-ia-btn').forEach(b => b.classList.remove('actif'));
    document.querySelectorAll('.section-ia').forEach(s => s.classList.remove('actif'));
    document.getElementById('ia-chat').classList.add('actif');
    document.querySelectorAll('.onglet-ia-btn')[3].classList.add('actif');

    const zoneChat = document.getElementById("messagesChat");

    // Message utilisateur
    const msgUser = document.createElement("div");
    msgUser.className = "carte-ia user";
    msgUser.innerHTML = '<div class="carte-ia-titre"><i class="bi bi-person"></i> Vous</div>'
                      + '<div class="carte-ia-desc">' + question + '</div>';
    zoneChat.appendChild(msgUser);

    // Message attente
    const msgAttente = document.createElement("div");
    msgAttente.className = "carte-ia info";
    msgAttente.innerHTML = '<div class="carte-ia-titre"><i class="bi bi-robot"></i> IA réfléchit...</div>';
    zoneChat.appendChild(msgAttente);
    zoneChat.scrollTop = zoneChat.scrollHeight;

    const codeAvecQuestion = code + "\n\n# Question : " + question;
    const params = new URLSearchParams();
    params.append("code", codeAvecQuestion);
    params.append("niveau", niveauUtilisateur);
    params.append("langage", langage);

    fetch("/api/ai/analyser", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.text())
    .then(data => {
        msgAttente.className = "carte-ia success";
        msgAttente.innerHTML = '<div class="carte-ia-titre"><i class="bi bi-robot"></i> Assistant IA</div>'
                             + '<div class="carte-ia-desc" style="white-space:pre-wrap">' + data + '</div>';
        zoneChat.scrollTop = zoneChat.scrollHeight;
    })
    .catch(() => {
        msgAttente.className = "carte-ia error";
        msgAttente.innerHTML = '<div class="carte-ia-titre">❌ Erreur de connexion</div>';
    });
}

// ── Afficher réponse avec couleurs ──
function afficherReponseCouleurs(texte, zone) {
    const lignes = texte.split("\n");
    let sectionActuelle = "";
    let contenuSection = "";

    const sections = [];
    lignes.forEach(ligne => {
        // Détecter les titres de sections
        if (ligne.match(/^#{1,3}\s+\d+\./) || ligne.match(/^\*\*\d+\./) ||
            ligne.match(/^[1-9]\.\s+[A-Z]/) || ligne.match(/^[A-Z\s]{5,}$/)) {
            if (contenuSection.trim()) {
                sections.push({ titre: sectionActuelle, contenu: contenuSection.trim() });
            }
            sectionActuelle = ligne.replace(/#{1,3}|\*\*/g, "").trim();
            contenuSection = "";
        } else {
            contenuSection += ligne + "\n";
        }
    });
    if (contenuSection.trim()) {
        sections.push({ titre: sectionActuelle, contenu: contenuSection.trim() });
    }

    if (sections.length === 0) {
        // Pas de sections détectées → afficher tout en une carte
        const carte = document.createElement("div");
        carte.className = "carte-ia success";
        carte.innerHTML = '<div class="carte-ia-desc" style="white-space:pre-wrap">' + texte + '</div>';
        zone.appendChild(carte);
        return;
    }

    sections.forEach(section => {
        const titre = section.titre.toUpperCase();
        let type = "success";
        let icone = "bi-check-circle";

        if (titre.includes("ERREUR") || titre.includes("ERROR") || titre.includes("DETECTE")) {
            type = "error"; icone = "bi-x-circle";
        } else if (titre.includes("CORRECT") || titre.includes("CORRIGE") || titre.includes("CODE CORRIGE")) {
            type = "success"; icone = "bi-check-circle";
        } else if (titre.includes("OPTIMIS") || titre.includes("ALTERNATIV") || titre.includes("MIEUX")) {
            type = "optimise"; icone = "bi-lightning";
        } else if (titre.includes("CONSEIL") || titre.includes("TRANSFORM") || titre.includes("DIFFERENCE")) {
            type = "conseil"; icone = "bi-lightbulb";
        } else if (titre.includes("EXPLICATION") || titre.includes("PROBLEME")) {
            type = "info"; icone = "bi-info-circle";
        }

        const carte = document.createElement("div");
        carte.className = "carte-ia " + type;
        let titreHtml = section.titre
            ? '<div class="carte-ia-titre"><i class="bi ' + icone + '"></i> ' + section.titre + '</div>'
            : '';
        carte.innerHTML = titreHtml
                        + '<div class="carte-ia-desc" style="white-space:pre-wrap">'
                        + section.contenu + '</div>';
        zone.appendChild(carte);
    });
}

// ── Afficher chargement ──
function afficherChargement(zoneId, message, type) {
    const zone = document.getElementById(zoneId);
    zone.innerHTML = '<div class="carte-ia ' + type + '">'
                   + '<div class="carte-ia-titre"><i class="bi bi-hourglass-split"></i> ' + message + '</div>'
                   + '</div>';
}

// ── Créer carte HTML ──
function creerCarte(titre, desc, type) {
    return '<div class="carte-ia ' + type + '">'
         + '<div class="carte-ia-titre">' + titre + '</div>'
         + (desc ? '<div class="carte-ia-desc">' + desc + '</div>' : '')
         + '</div>';
}