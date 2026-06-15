package com.codify.app.model;

import jakarta.persistence.*;

// Table qui stocke chaque exercice résolu par chaque utilisateur
@Entity
@Table(name = "historique_exercices")
public class HistoriqueExercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers l'utilisateur
    private Long userId;

    // Titre de l'exercice
    private String titreExercice;

    // Niveau au moment de la résolution
    private String niveau;

    // Points gagnés (0, moitié ou complet)
    private int pointsGagnes;

    // Temps en secondes
    private long tempsSecondes;

    // Temps formaté (ex: "2min 30s")
    private String tempsFormate;

    // Complexité algorithmique (ex: "O(n)")
    private String complexite;

    // Résultat : "correct", "partiel", "incorrect"
    private String resultat;

    public HistoriqueExercice() {}

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitreExercice() { return titreExercice; }
    public void setTitreExercice(String t) { this.titreExercice = t; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public int getPointsGagnes() { return pointsGagnes; }
    public void setPointsGagnes(int p) { this.pointsGagnes = p; }

    public long getTempsSecondes() { return tempsSecondes; }
    public void setTempsSecondes(long t) { this.tempsSecondes = t; }

    public String getTempsFormate() { return tempsFormate; }
    public void setTempsFormate(String t) { this.tempsFormate = t; }

    public String getComplexite() { return complexite; }
    public void setComplexite(String c) { this.complexite = c; }

    public String getResultat() { return resultat; }
    public void setResultat(String r) { this.resultat = r; }
}