package com.codify.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(unique = true)
    private String email;

    private String password;
    private String level;

    // Points totaux
    private int points = 0;

    // Nombre d'exercices complétés au niveau actuel
    private int exercicesCompletes = 0;

    // Nombre total de séries complétées
    private int seriesCompletes = 0;

    // Temps total passé en secondes
    private long tempsTotal = 0;

    // Test disponible (true si 20 exercices complétés)
    private boolean testDisponible = false;

    public User() {}

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getExercicesCompletes() { return exercicesCompletes; }
    public void setExercicesCompletes(int exercicesCompletes) {
        this.exercicesCompletes = exercicesCompletes;
    }

    public int getSeriesCompletes() { return seriesCompletes; }
    public void setSeriesCompletes(int seriesCompletes) {
        this.seriesCompletes = seriesCompletes;
    }

    public long getTempsTotal() { return tempsTotal; }
    public void setTempsTotal(long tempsTotal) { this.tempsTotal = tempsTotal; }

    public boolean isTestDisponible() { return testDisponible; }
    public void setTestDisponible(boolean testDisponible) {
        this.testDisponible = testDisponible;
    }
}