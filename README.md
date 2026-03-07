# WhitelistMessage – Minecraft Paper Plugin

Ein Paper-Plugin, das Spielern, die nicht auf der Whitelist stehen, eine benutzerdefinierte Nachricht zeigt.

## Download

Die fertige `.jar`-Datei kann unter **[Releases](../../releases)** heruntergeladen werden.

> Einfach auf die neueste Release klicken → unter „Assets" die `WhitelistMessage-X.X.X.jar` herunterladen.

## Installation

1. `WhitelistMessage-X.X.X.jar` in den `plugins/`-Ordner deines Paper-Servers kopieren.
2. In der `server.properties` `white-list=true` setzen.
3. Server starten (oder `/reload confirm` ausführen).

Sobald ein Spieler versucht beizutreten, der **nicht** auf der Whitelist steht, sieht er:

> **Fick dich fett, versuch nicht auf den Server drauf zu kommen!**

## Selbst bauen

Voraussetzung: Java 17+ und Maven

```bash
git clone https://github.com/Fellinara/Eigener-pvpServer.git
cd Eigener-pvpServer
mvn package
# → target/WhitelistMessage-1.0.0.jar
```

## CI-Status

![Build](../../actions/workflows/build.yml/badge.svg)
