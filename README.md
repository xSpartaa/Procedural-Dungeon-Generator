# Procedural Dungeon Generator

![Java](https://img.shields.io/badge/Language-Java-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

Un générateur de donjons aléatoires développé en **Java**, utilisant une approche hybride combinant le **partitionnement binaire d'espace (BSP)** et des algorithmes de placement de salles personnalisés. Ce projet est idéal pour les jeux de type Roguelike ou Action-RPG.

## 🚀 Fonctionnalités

* **Génération Aléatoire** : Chaque donjon est unique grâce à une initialisation basée sur une graine aléatoire.
* **Algorithme BSP (Binary Space Partitioning)** : Découpage intelligent de l'espace pour garantir une répartition homogène des salles.
* **Système de Couloirs** : Génération automatique de jonctions entre les salles pour assurer que le donjon est entièrement explorable sans culs-de-sac involontaires.
* **Gestion des Types de Salles** : Support pour différents types de salles (Salles de départ, Boss, Trésors, etc.).
* **Visualisation Temps Réel** : Inclut un panneau de rendu (`DungeonPanel`) pour visualiser la structure générée.

## 🛠️ Structure du Projet

Le projet est organisé en plusieurs packages clés :

* **`dungeon`** : Contient le cœur logique (Générateur, structures de données des salles et des nœuds).
* **`main`** : Gère l'interface utilisateur (Swing) et le point d'entrée de l'application.

### Classes Principales
* `DungeonGenerator` : L'algorithme principal gérant la création des salles et des couloirs.
* `BSPNode` : Gère la division récursive de l'espace de jeu.
* `Room` & `Corridor` : Représentent les entités physiques du donjon.

## 💻 Installation et Utilisation

### Prérequis
* Java JDK 8 ou supérieur.

### Compilation
Depuis la racine du projet :
```bash
javac src/main/Main.java -d out

