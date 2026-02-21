# 🎮 DuellPlugin - Das Ultimative PvP-Erlebnis

Ein professionelles Minecraft 1.21.1 Duell-Plugin mit 1v1 PvP, Bot-Kämpfe, Kits, Arenen und ELO-Rating.

## ✨ Features

- **1v1 Duell-System** - Fordere andere Spieler heraus
- **Bot-Kämpfe** - Trainiere gegen KI-Bots mit 100 Schwierigkeitsleveln
- **6 Kits** - Schwertkämpfer, Bogenschütze, Tank, Berserker, Alchemist, Ritter
- **Arena-System** - Erstelle und verwalte mehrere Kampfarenen
- **Lobby-System** - Professionelle Lobby mit interaktiven Items
- **ELO-Rating** - Kompetitives Ranking-System
- **Statistiken** - Siege, Niederlagen, K/D, Kill-Streaks
- **Professionelle Join/Leave-Nachrichten**

## 🔧 Installation

1. Java 21 installieren
2. Plugin bauen: `mvn clean package`
3. Die JAR-Datei aus `target/` in den `plugins/`-Ordner des Servers kopieren
4. Server starten

## 📋 Befehle

| Befehl | Beschreibung | Berechtigung |
|--------|-------------|-------------|
| `/duell` | Duell-Menü öffnen | `duell.use` |
| `/duell fordern <Spieler>` | Spieler herausfordern | `duell.use` |
| `/duell annehmen` | Duell annehmen | `duell.use` |
| `/duell ablehnen` | Duell ablehnen | `duell.use` |
| `/bot [level]` | Bot-Kampf starten (1-100) | `duell.bot` |
| `/kit [name]` | Kit auswählen | `duell.kit` |
| `/stats [spieler]` | Statistiken anzeigen | `duell.stats` |
| `/arena create/delete/setspawn/list` | Arena-Verwaltung | `duell.admin` |
| `/lobby` | Zur Lobby teleportieren | `duell.use` |
| `/setlobby` | Lobby-Spawn setzen | `duell.admin` |

## 🎒 Lobby-Items

| Slot | Item | Funktion |
|------|------|----------|
| 0 | Diamantschwert | Duell-Menü öffnen |
| 1 | Zombie-Kopf | Bot-Menü öffnen |
| 2 | Truhe | Kit-Auswahl |
| 3 | Grasblock | Arena-Auswahl |
| 4 | Papier | Statistiken |
| 7 | Enderauge | Spieler verstecken/zeigen |
| 8 | Kompass | Lobby-Navigator |

## ⚔ Kits

- **Schwertkämpfer** - Klassischer Nahkampf mit Schwert und Eisenrüstung
- **Bogenschütze** - Fernkampf-Spezialist mit Bogen und Pfeilen
- **Tank** - Maximale Diamant-Rüstung und Ausdauer
- **Berserker** - Maximaler Schaden mit Netherit-Axt, wenig Rüstung
- **Alchemist** - Tränke und goldene Ausrüstung
- **Ritter** - Ausgewogener Kämpfer mit Schild und Armbrust

## 🤖 Bot-System (100 Level)

| Level | Rang | Schwierigkeit | Ausrüstung |
|-------|------|--------------|------------|
| 1-19 | Anfänger | Einfach | Leder |
| 20-39 | Lehrling | Mittel | Kettenhemd |
| 40-59 | Kämpfer | Schwer | Eisen |
| 60-79 | Elite | Sehr Schwer | Diamant |
| 80-100 | Meister/Legende | Extrem | Netherit |

## 🏆 ELO-System

- Start-ELO: 1000
- Berechnung nach Standard-ELO-Formel (K-Faktor: 32)
- Ränge: Eisen → Bronze → Silber → Gold → Diamant → Meister → Großmeister → Legende

## 🔨 Bauen

```bash
mvn clean package
```

Die fertige Plugin-JAR befindet sich dann unter `target/DuellPlugin-1.0.0.jar`.

