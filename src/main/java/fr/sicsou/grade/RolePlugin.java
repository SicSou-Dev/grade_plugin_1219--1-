package fr.sicsou.grade;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

public class RolePlugin extends JavaPlugin implements Listener {

    public enum Role {
        JOUEUR, MODERATEUR, FONDATEUR, YOUTUBEUR, VIP
    }

    private final Map<String, Role> playerRoles = new HashMap<>();
    private final Map<Role, String> prefixes = new HashMap<>();
    private final Map<Role, ChatColor> colors = new HashMap<>();

    private Scoreboard scoreboard;
    private Role defaultRole;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        setupScoreboard();
        getLogger().info("Grade plugin enabled!");
    }

    private void loadConfigValues() {
        String defaultRank = getConfig().getString("default-rank", "JOUEUR").toUpperCase();
        try { defaultRole = Role.valueOf(defaultRank); }
        catch (Exception e) { defaultRole = Role.JOUEUR; }

        ConfigurationSection section = getConfig().getConfigurationSection("grades");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Role role = Role.valueOf(key);
                    String prefix = section.getString(key + ".prefix", "&7[" + key + "] ");
                    String colorName = section.getString(key + ".color", "WHITE");
                    prefixes.put(role, ChatColor.translateAlternateColorCodes('&', prefix));

                    try { colors.put(role, ChatColor.valueOf(colorName)); }
                    catch (Exception ignored) { colors.put(role, ChatColor.WHITE); }

                } catch (Exception ignored) {}
            }
        }
    }

    private void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getMainScoreboard();

        for (Role role : Role.values()) {
            Team team = scoreboard.getTeam(role.name().toLowerCase());
            if (team == null) team = scoreboard.registerNewTeam(role.name().toLowerCase());
            team.setPrefix(prefixes.get(role));
            team.setColor(colors.get(role));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!playerRoles.containsKey(p.getName()))
            playerRoles.put(p.getName(), defaultRole);

        applyTeam(p, playerRoles.get(p.getName()));
        p.setScoreboard(scoreboard);
    }

    private void applyTeam(Player player, Role role) {
        for (Role r : Role.values()) {
            Team t = scoreboard.getTeam(r.name().toLowerCase());
            if (t != null) t.removeEntry(player.getName());
        }

        Team team = scoreboard.getTeam(role.name().toLowerCase());
        if (team != null) team.addEntry(player.getName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("grade")) {
            if (args.length != 2) {
                sender.sendMessage(getMessage("messages.usage-grade"));
                return true;
            }

            String gradeName = args[0].toUpperCase();
            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(getMessage("messages.player-not-found"));
                return true;
            }

            try {
                Role role = Role.valueOf(gradeName);

                playerRoles.put(target.getName(), role);
                applyTeam(target, role);

                sender.sendMessage(
                        getMessage("messages.rank-set")
                                .replace("%player%", target.getName())
                                .replace("%rank%", role.name())
                );

                target.sendMessage(
                        getMessage("messages.rank-received")
                                .replace("%rank%", role.name())
                );

            } catch (Exception e) {
                sender.sendMessage(getMessage("messages.invalid-rank"));
            }

            return true;
        }

        return false;
    }

    private String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString(path, "&cMessage introuvable.")
        );
    }
}
