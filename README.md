# KlassenPlugin — Eigener PvP Server

[![Build](https://github.com/Fellinara/Eigener-pvpServer/actions/workflows/build.yml/badge.svg)](https://github.com/Fellinara/Eigener-pvpServer/actions/workflows/build.yml)
[![Release](https://github.com/Fellinara/Eigener-pvpServer/actions/workflows/release.yml/badge.svg)](https://github.com/Fellinara/Eigener-pvpServer/releases/latest)

Paper 1.21 Minecraft Plugin für einen eigenen PvP-Server.

---

## ⬇️ Herunterladen

### Stabile Version (empfohlen)
Gehe zur [**Releases-Seite**](https://github.com/Fellinara/Eigener-pvpServer/releases/latest)
und lade die neueste `KlassenPlugin-vX.X.X.jar` herunter.

### Entwicklungsversion (neuester Build)
1. Öffne den Tab [**Actions → Build**](https://github.com/Fellinara/Eigener-pvpServer/actions/workflows/build.yml)
2. Klicke auf den neuesten erfolgreichen Workflow-Run
3. Lade unter **Artifacts** die Datei `KlassenPlugin` herunter

---

## 🔧 Installation

1. Stelle sicher, dass du **Paper 1.21.4+** mit **Java 21** verwendest
2. Kopiere die JAR-Datei in den `plugins/`-Ordner deines Servers
3. Starte den Server (oder führe `/reload confirm` aus)
4. Passe die Konfiguration in `plugins/KlassenPlugin/config.yml` an

---

## ✨ Features

| Kategorie | Befehle |
|-----------|---------|
| Lobby & Spawn | `/lobby`, `/setlobby`, `/spawn`, `/setspawn` |
| Homes | `/home [name]`, `/sethome [name]`, `/delhome <name>`, `/homes` |
| Zurück | `/back` — kehrt zur letzten Position zurück (auch nach dem Tod) |
| Warps | `/warp <name>`, `/setwarp <name>`, `/delwarp <name>`, `/warps` |
| TPA | `/tpa <Spieler>`, `/tpaccept`, `/tpdeny`, `/tpcancel` |
| Kits (Klassen) | `/kit <name>`, `/kits`, `/createkit <name>`, `/delkit <name>` |
| Admin | `/heal [Spieler]`, `/feed [Spieler]`, `/fly [Spieler]` |
| Nachrichten | `/msg <Spieler> <Text>`, `/r <Text>` |
| Plugin | `/plugintoggle <enable|disable>` |

---

## 🏷️ Neues Release erstellen

```bash
git tag v1.0.1
git push origin v1.0.1
```

GitHub Actions baut dann automatisch die JAR und erstellt ein neues Release.
