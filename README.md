# Eigener-pvpServer
Minecraft Plugin

---

## 🗡️ Void Sword – Ressourcen Pack

Dieses Repository enthält ein Minecraft Ressourcen Pack mit einem coolen **Void Sword** (Leere-Schwert), das das Diamond-Schwert ersetzt.

### ✨ Features
- **Void Sword Textur** – Ein magisches, lila/blau leuchtendes Schwert mit weißem Energiekern
- Goldene Parierstange mit rotem Edelstein
- Dunkler Holzgriff mit Reflexions-Highlight
- Kompatibel mit **Minecraft Java Edition 1.20 / 1.20.4** (`pack_format: 15`)

### 📦 Installation

1. Lade den Ordner `ResourcePack/` herunter (oder als ZIP zippen)
2. Kopiere die ZIP-Datei nach:
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
| Bereich        | Farbe                  |
|----------------|------------------------|
| Klinge – Kern  | `#F0DCFF` Weißlich-lila |
| Klinge – Mitte | `#8214DC` Lila          |
| Klinge – Rand  | `#8228DC` Mittellila    |
| Leuchten       | `#C882FF` Helles Lila   |
| Parierstange   | `#FFC832` Gold          |
| Edelstein      | `#FF3250` Rot           |
| Griff          | `#643C28` Dunkelbraun   |
