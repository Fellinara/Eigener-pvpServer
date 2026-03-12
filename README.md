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
| **Anti-Cheat** | `/anticheat status|enable|disable|check|violations|reset` |
| **Ränge** | `/rank create|delete|setprefix|addperm|removeperm|assign|remove|list|info|player` |
| **Bans** | `/klassenplugin ban <Spieler>` (IP-Ban), `/klassenplugin unban <IP>` |

---

## 🛡️ Anti-Cheat System

Das eingebaute Anti-Cheat System erkennt folgende Verstöße:

| Check | Beschreibung | Konfiguration |
|-------|-------------|---------------|
| **XRay** | Zu viele wertvolle Erze in zu kurzer Zeit | `anticheat.xray.*` |
| **Speed** | Bewegung schneller als erlaubt | `anticheat.speed.max-blocks-per-second` |
| **Fly** | Fliegen ohne Flugmodus | `anticheat.fly.max-air-ticks` |
| **Reach** | Angriffe aus zu großer Entfernung | `anticheat.reach.max-reach` |
| **KillAura** | Zu viele Treffer pro Sekunde | `anticheat.killaura.max-hits-per-second` |

**Aktionen bei Verstößen** (konfigurierbar): `warn` / `kick` / `ban`

Admins mit der Berechtigung `klassenplugin.anticheat.alert` werden bei jedem Verstoß benachrichtigt.
Ops mit `klassenplugin.anticheat.bypass` werden nicht überprüft.

---

## 👑 Rang-System

Erstelle eigene Ränge und weise Spielern Berechtigungen zu:

```
/rank create admin
/rank setprefix admin &4[Admin] 
/rank addperm admin klassenplugin.admin
/rank addperm admin klassenplugin.heal
/rank addperm admin minecraft.command.gamemode
/rank assign Spielername admin
```

Ränge werden in `plugins/KlassenPlugin/ranks.yml` gespeichert und beim Einloggen automatisch angewendet.

---

## 🔨 IP-Ban

Ein Spieler der online ist, kann per IP sofort gebannt werden:

```
/klassenplugin ban Spielername
# oder Alias:
/kp ban Spielername
```

IP-Ban aufheben:
```
/klassenplugin unban 192.168.1.1
```

---

## 🏷️ Neues Release erstellen

```bash
git tag v1.0.1
git push origin v1.0.1
```

GitHub Actions baut dann automatisch die JAR und erstellt ein neues Release.
