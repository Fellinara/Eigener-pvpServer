package de.duellplugin.commands;

import de.duellplugin.DuellPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class TrollCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = Arrays.asList(
            "blitz", "feuer", "blind", "slow", "nausea", "bounce", "unsichtbar", "rüstung", "freeze"
    );

    private final DuellPlugin plugin;

    public TrollCommand(DuellPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!sender.isOp() && !sender.hasPermission("duell.troll")) {
            sender.sendMessage(prefix + "§cDu hast keine Berechtigung!");
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(prefix + "§cSpieler §6" + args[0] + " §cnicht gefunden!");
            return true;
        }

        if (target.isOp() && !(sender instanceof Player sp && sp.isOp())) {
            sender.sendMessage(prefix + "§cDu kannst keinen Admin trollen!");
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "blitz" -> {
                target.getWorld().strikeLightningEffect(target.getLocation());
                target.sendMessage("§c⚡ Ein Blitz trifft in deiner Nähe ein!");
                sender.sendMessage(prefix + "§a⚡ Blitz auf §6" + target.getName() + " §agezaubert!");
            }
            case "feuer" -> {
                target.setFireTicks(200);
                target.sendMessage("§c🔥 Du brennst!");
                sender.sendMessage(prefix + "§a🔥 §6" + target.getName() + " §abrennt jetzt!");
            }
            case "blind" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, false, false));
                target.sendMessage("§8Du bist kurzzeitig blind!");
                sender.sendMessage(prefix + "§a§6" + target.getName() + " §aist jetzt kurz blind!");
            }
            case "slow" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3, false, false));
                target.sendMessage("§9Du kannst dich kaum bewegen!");
                sender.sendMessage(prefix + "§a§6" + target.getName() + " §awird verlangsamt!");
            }
            case "nausea" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 1, false, false));
                target.sendMessage("§2Dir wird schwindlig...");
                sender.sendMessage(prefix + "§a§6" + target.getName() + " §abekommt Übelkeit!");
            }
            case "bounce" -> {
                // Repeatedly launch the player up for a few seconds
                new BukkitRunnable() {
                    int times = 5;
                    @Override
                    public void run() {
                        if (!target.isOnline() || times-- <= 0) { cancel(); return; }
                        target.setVelocity(new Vector(0, 2.0, 0));
                    }
                }.runTaskTimer(plugin, 0L, 15L);
                target.sendMessage("§b🚀 Du wirst hochgeschleudert!");
                sender.sendMessage(prefix + "§a§6" + target.getName() + " §awird hochgeschleudert!");
            }
            case "unsichtbar" -> {
                // Make the player appear invisible to themselves via potion (they can't see their own hand)
                target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 300, 0, false, false));
                target.sendMessage("§7Du bist unsichtbar geworden...");
                sender.sendMessage(prefix + "§a§6" + target.getName() + " §awird für 15s unsichtbar!");
            }
            case "rüstung", "ruestung" -> {
                target.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
                target.sendMessage("§cDeine Rüstung ist verschwunden!");
                sender.sendMessage(prefix + "§aDie Rüstung von §6" + target.getName() + " §awurde entfernt!");
            }
            case "freeze" -> {
                // Freeze by applying very strong slowness + mining fatigue
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 127, false, false));
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 128, false, false));
                target.sendMessage("§bDu bist eingefroren!");
                sender.sendMessage(prefix + "§a§6" + target.getName() + " §awurde für 5s eingefroren!");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l━━━ Troll-Befehle ━━━");
        sender.sendMessage("§e/troll <Spieler> blitz §7- Blitzeinschlag");
        sender.sendMessage("§e/troll <Spieler> feuer §7- In Brand setzen");
        sender.sendMessage("§e/troll <Spieler> blind §7- Kurz blind machen");
        sender.sendMessage("§e/troll <Spieler> slow §7- Verlangsamen");
        sender.sendMessage("§e/troll <Spieler> nausea §7- Übelkeit");
        sender.sendMessage("§e/troll <Spieler> bounce §7- Hochschleudern");
        sender.sendMessage("§e/troll <Spieler> unsichtbar §7- Unsichtbar machen");
        sender.sendMessage("§e/troll <Spieler> rüstung §7- Rüstung entfernen");
        sender.sendMessage("§e/troll <Spieler> freeze §7- Einfrieren");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return ACTIONS.stream()
                    .filter(a -> a.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
