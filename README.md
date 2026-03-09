# Risiko – Minecraft PvP Finale Plugin

Ein Minecraft Paper 1.21 Plugin für das Risiko-Finale: **Fichten Königreich** ⚔ **Dschungel Königreich**.

## Features

### 🔴 Visuelle Herzen (Action Bar)
- Jeder Spieler hat **visuelle Herzen** die in der **Action Bar** als oranges `❤` angezeigt werden
- Beim Tod durch einen anderen Spieler verliert man ein Herz
- Leere Herzen werden als `♡` grau dargestellt
- Könige erhalten eine **goldene Krone** `♔` und **2 Extra-Herzen**
- Das **letzte Herz eines Königs** kann nur von einem anderen König genommen werden

### 🏰 Zwei Königreiche
| Königreich | Farbe | Emoji |
|---|---|---|
| **Fichten Königreich** | Gold 🟡 | 🌲 |
| **Dschungel Königreich** | Grün 🟢 | 🌿 |

### ⚔ Finale-Modus
1. Beide Teams werden zu ihren Spawn-Punkten teleportiert
2. Die Weltgrenze beginnt zu schrumpfen
3. Wer keine Herzen mehr hat wird **gebannt**
4. Das letzte Team gewinnt

### 💀 Kampf-Logout-System
- Spieler die sich während eines Kampfes ausloggen, hinterlassen einen **ArmorStand-Dummy**
- Der Dummy kann von Gegnern getötet werden → Herzenverlust / Ban
- Nach **15 Sekunden** verschwindet der Dummy (Spieler überlebt)

---

## Befehle

| Befehl | Beschreibung |
|---|---|
| `/risiko team <spieler> <fichten\|dschungel>` | Spieler einem Königreich zuweisen |
| `/risiko king <spieler>` | Spieler zum König seines Königreichs machen |
| `/risiko addherz <spieler> [anzahl]` | Herzen hinzufügen |
| `/risiko removeherz <spieler> [anzahl]` | Herzen entfernen |
| `/risiko unban <spieler>` | Spieler entbannen |
| `/risiko start` | Finale starten |
| `/risiko stop` | Finale stoppen |
| `/risiko setspawn <fichten\|dschungel>` | Spawn-Punkt setzen (eigene Position) |
| `/risiko info [spieler]` | Spieler- oder Finale-Informationen |
| `/risiko list` | Alle Spieler und Teams anzeigen |
| `/risiko reload` | Konfiguration neu laden |

**Permission:** `risiko.admin` (Standard: OP)

---

## Installation

1. Benötigt: **Paper 1.21** und **Java 21**
2. Plugin-JAR in den `plugins/`-Ordner kopieren
3. Server starten → `config.yml` wird erstellt
4. Spawn-Punkte mit `/risiko setspawn fichten` und `/risiko setspawn dschungel` setzen
5. Spieler mit `/risiko team` einem Königreich zuweisen
6. Finale mit `/risiko start` beginnen

## Build

```bash
mvn clean package
```

Das JAR wird unter `target/Risiko-1.0.0.jar` erstellt.

**Anforderungen:** Java 21, Apache Maven 3.6+

---

## Konfiguration (`config.yml`)

```yaml
default-hearts: 3         # Standard-Herzen pro Spieler
king-extra-hearts: 2      # Zusätzliche Herzen für Könige
combat-tag-seconds: 30    # Kampf-Tag-Dauer in Sekunden
combat-logout-stand-seconds: 15  # Dummy-Lebensdauer
border-start-size: 500.0  # Startgröße der Weltgrenze (Durchmesser)
border-end-size: 50.0     # Endgröße der Weltgrenze
border-shrink-time: 600   # Schrumpf-Dauer in Sekunden
```
