# Eigener-pvpServer
Minecraft Plugin

---

## 🗡️ Void Sword – Ressourcen Pack

Dieses Repository enthält ein Minecraft Ressourcen Pack mit einem coolen **Void Sword** (Leere-Schwert), das das Diamond-Schwert ersetzt.

### ✨ Features
- **Void Sword Textur** – Dunkle Obsidianklinge mit leuchtend violetten Kristallen entlang der Klinge
- Breite leuchtende Parierstange mit violettem Edelstein
- Gerippter dunkler Griff (Wicklung)
- Leuchtender violetter Knauf-Edelstein am Griffende
- Kompatibel mit **Minecraft Java Edition 1.20 / 1.20.4** (`pack_format: 15`)

![Void Sword Preview](https://github.com/user-attachments/assets/2453cd64-4f92-46a0-ba18-9ea197c75894)

### 📥 Download

**Methode 1 – Direkt aus dem Repository (empfohlen):**

**👉 [VoidSword-ResourcePack.zip herunterladen](https://github.com/Fellinara/Eigener-pvpServer/raw/main/VoidSword-ResourcePack.zip)**

> Dieser Link lädt die ZIP-Datei direkt aus dem Repository herunter – kein GitHub-Account nötig.

---

**Methode 2 – Als GitHub Actions Artifact:**

1. Gehe zu **[Actions](https://github.com/Fellinara/Eigener-pvpServer/actions)** in diesem Repository
2. Klicke auf den neuesten **"Build & Release ResourcePack"** Workflow-Run
3. Scrolle nach unten zum Abschnitt **"Artifacts"**
4. Klicke auf **`VoidSword-ResourcePack`** → die ZIP wird heruntergeladen

> Für Artifacts wird ein GitHub-Account benötigt.

### 📦 Installation

1. Lade **[VoidSword-ResourcePack.zip](https://github.com/Fellinara/Eigener-pvpServer/raw/main/VoidSword-ResourcePack.zip)** herunter
2. Kopiere die ZIP-Datei **unentpackt** in deinen Minecraft-Ressourcenpakete-Ordner:
   - **Windows:** `%AppData%\.minecraft\resourcepacks\`
   - **macOS:** `~/Library/Application Support/minecraft/resourcepacks/`
   - **Linux:** `~/.minecraft/resourcepacks/`
3. Starte Minecraft → **Optionen → Ressourcenpakete** → Paket aktivieren

### 📁 Struktur
```
ResourcePack/
├── pack.mcmeta                              # Pack-Metadaten (Format + Beschreibung)
├── pack.png                                 # Pack-Vorschaubild
└── assets/
    └── minecraft/
        ├── textures/
        │   └── item/
        │       └── diamond_sword.png        # Void-Schwert Textur (16×16 RGBA)
        └── models/
            └── item/
                └── diamond_sword.json       # Item-Modell (Display-Einstellungen)
```

### 🎨 Farb-Palette des Void Swords
| Bereich              | Hex       | Farbe                        |
|----------------------|-----------|------------------------------|
| Klinge – heißer Kern | `#F0C8FF` | Weißlich-Lila (Leuchtkern)   |
| Klinge – Kristall    | `#C83CFF` | Helles Violett               |
| Klinge – Leuchten    | `#960AC8` | Tiefes Lila                  |
| Klinge – Obsidian    | `#160A2E` | Fast Schwarz                 |
| Parierstange         | `#960AC8` | Violetter Kristall           |
| Griff                | `#12070E` | Sehr Dunkel                  |
| Knauf-Edelstein      | `#F0C8FF` | Weißlich-Lila Edelstein      |
