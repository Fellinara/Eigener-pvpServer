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
| **Bans** | `/ban <Spieler> [Grund]`, `/unban <Spieler|IP>`, `/klassenplugin ban <Spieler>` (IP-Ban) |
| **Economy** | `/balance [Spieler]`, `/pay <Spieler> <Betrag>` |
| **Shop** | `/shop list|buy|sell|info|admin` |
| **Auktionshaus** | `/ah sell|list|buy|request|fulfill|cancel|search|meine` |
| **Changelog** | `/changelog` — wöchentliche Preisboosts |
| **Bündnis** | `/ally invite|accept|deny|info|leave` |

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
| **Scaffold** | Zu schnelles Blockplatzieren | `anticheat.scaffold.max-blocks-per-second` |
| **NoFall** | Fallschaden-Umgehung | `anticheat.nofall.*` |
| **PacketMove** | Unmögliche Bewegungsdistanz in einem Paket *(ProtocolLib)* | `anticheat.packet.packetmove.*` |
| **PacketReach** | Entitäts-Interaktion aus zu großer Entfernung *(ProtocolLib)* | `anticheat.packet.packetreach.*` |
| **PacketDig** | Block-Abbau aus zu großer Entfernung *(ProtocolLib)* | `anticheat.packet.packetdig.*` |
| **PacketFlood** | Zu viele Pakete pro Sekunde *(ProtocolLib)* | `anticheat.packet.packetflood.*` |
| **FreeCam** | Spieler sendet keine Bewegungspakete, ist aber aktiv *(ProtocolLib)* | `anticheat.packet.freecam.*` |
| **Hack-Client** | Wurst, Meteor, Impact u.a. via Client-Brand/Kanal-Erkennung *(ProtocolLib)* | `anticheat.hack-client.*` |

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

## 🔨 Ban-System

Spieler mit optionalem Grund bannen (styled Ban-Screen):

```
/ban <Spieler> [Grund]
/ban SpielerName Cheating
```

Spieler/IP entbannen:
```
/unban <Spieler>
/unban 192.168.1.1
```

Legacy IP-Ban (über Alias `/kp`):
```
/klassenplugin ban Spielername
/klassenplugin unban 192.168.1.1
```

---

## 💰 Economy & Shop

### Ingame-Währung
Jeder Spieler startet mit 500 Coins. Geld wird durch den Shop und das Auktionshaus verdient.

```
/balance           # Eigenen Kontostand ansehen
/balance Steve     # Kontostand eines anderen Spielers (Admin)
/pay Steve 100     # 100 Coins an Steve überweisen
```

### Dynamische Inflation
Das Wirtschaftssystem passt Preise automatisch an:
- **Inflation > 100%** → Preise steigen (zu viel Geld im Umlauf)
- **Inflation < 100%** → Preise fallen (wenig Geld im Umlauf)
- Zielwert: 1000 Coins pro Spieler (konfigurierbar)

### Shop
```
/shop list          # Alle Artikel anzeigen (mit Inflation-Anzeige)
/shop buy DIAMOND 5 # 5 Diamanten kaufen
/shop sell IRON_INGOT 32  # 32 Eisenbarren verkaufen
/shop sell hand     # Item in der Hand verkaufen
/shop info DIAMOND  # Preisinformationen
```

---

## 🏪 Auktionshaus (`/ah`)

Spieler können Items anbieten und anfordern:

```
# Item aus der Hand verkaufen
/ah sell 100        # Item in der Hand für 100 Coins anbieten

# Angebote kaufen
/ah list            # Alle Angebote anzeigen
/ah buy 5           # Angebot #5 kaufen

# Items anfordern
/ah request DIAMOND 3 150   # 3 Diamanten für 150 Coins suchen
/ah fulfill 7               # Anfrage #7 erfüllen (Items abliefern)

# Verwaltung
/ah cancel 5        # Eigenes Angebot #5 abbrechen
/ah search DIAMOND  # Nach Diamanten suchen
/ah meine           # Eigene Angebote anzeigen
```

---

## 📋 Wöchentlicher Changelog

Jede Woche werden zufällige Items mit einem Verkaufsbonus belegt. Die Boosts der Vorwoche werden zurückgesetzt.

```
/changelog          # Aktuelle Boosts und Wirtschaftsinfo anzeigen
/changelog forceroll  # (Admin) Neue Woche manuell starten
```

---

## 🤝 Bündnis-System

Jeder Spieler kann mit genau **einer** anderen Person ein Bündnis eingehen.

```
/ally invite Steve  # Steve zu einem Bündnis einladen
/ally accept        # Bündnisanfrage annehmen
/ally deny          # Bündnisanfrage ablehnen
/ally info          # Aktuellen Verbündeten anzeigen
/ally leave         # Bündnis auflösen
```

**Bündnis-Schutz**: Verbündete können sich nicht gegenseitig angreifen (konfigurierbar).

---

## 🏷️ Neues Release erstellen

```bash
git tag v1.0.1
git push origin v1.0.1
```

GitHub Actions baut dann automatisch die JAR und erstellt ein neues Release.
