package com.codify.app.model;

import jakarta.persistence.*;

// Table qui stocke chaque défi entre 2 utilisateurs
@Entity
@Table(name = "defis")
public class Defi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Joueur 1 qui crée le défi
    private String joueur1;

    // Joueur 2 qui rejoint le défi
    private String joueur2;

    // Niveau du défi
    private String niveau;

    // Langage choisi
    private String langage;

    // Problème à résoudre (généré par l'IA)
    @Column(columnDefinition = "TEXT")
    private String probleme;

    // Statut : "attente", "en_cours", "termine"
    private String statut;

    // Durée en minutes (5, 10, 15, 30)
    private int dureeMinutes;

    // Gagnant (username du gagnant)
    private String gagnant;

    // Score joueur 1
    private int scoreJoueur1;

    // Score joueur 2
    private int scoreJoueur2;

    // Temps de résolution joueur 1 en secondes
    private long tempsJoueur1;

    // Temps de résolution joueur 2 en secondes
    private long tempsJoueur2;
 // Timestamp du début du défi (pour synchroniser les timers des 2 joueurs)
    private long timestampDebut = 0;

    public Defi() {}

    public Long getId() { return id; }

    public String getJoueur1() { return joueur1; }
    public void setJoueur1(String j) { this.joueur1 = j; }

    public String getJoueur2() { return joueur2; }
    public void setJoueur2(String j) { this.joueur2 = j; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String n) { this.niveau = n; }

    public String getLangage() { return langage; }
    public void setLangage(String l) { this.langage = l; }

    public String getProbleme() { return probleme; }
    public void setProbleme(String p) { this.probleme = p; }

    public String getStatut() { return statut; }
    public void setStatut(String s) { this.statut = s; }

    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int d) { this.dureeMinutes = d; }

    public String getGagnant() { return gagnant; }
    public void setGagnant(String g) { this.gagnant = g; }

    public int getScoreJoueur1() { return scoreJoueur1; }
    public void setScoreJoueur1(int s) { this.scoreJoueur1 = s; }

    public int getScoreJoueur2() { return scoreJoueur2; }
    public void setScoreJoueur2(int s) { this.scoreJoueur2 = s; }

    public long getTempsJoueur1() { return tempsJoueur1; }
    public void setTempsJoueur1(long t) { this.tempsJoueur1 = t; }

    public long getTempsJoueur2() { return tempsJoueur2; }
    public void setTempsJoueur2(long t) { this.tempsJoueur2 = t; }
    
    public long getTimestampDebut() { return timestampDebut; }
    public void setTimestampDebut(long t) { this.timestampDebut = t; }
}