package pl.azurite.antylogout;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AntyLogoutPlugin extends JavaPlugin {

    private final Map<UUID, Long> tagged = new HashMap<>();
    private final Set<String> allowed = new HashSet<>();
    private long durationMs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
    }

    private void loadSettings() {
        reloadConfig();
        durationMs = Math.max(1, getConfig().getInt("czas-sekundy", 30)) * 1000L;
        allowed.clear();
        for (String cmd : getConfig().getStringList("dozwolone-komendy")) {
            allowed.add(cmd.toLowerCase(Locale.ROOT));
        }
    }

    public Component msg(String key, String... replacements) {
        String text = getConfig().getString("wiadomosci." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public boolean isAllowed(String label) {
        return allowed.contains(label);
    }

    public boolean isTagged(Player player) {
        Long expires = tagged.get(player.getUniqueId());
        if (expires == null) return false;
        if (expires <= System.currentTimeMillis()) {
            tagged.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void tag(Player player) {
        if (player.hasPermission("azurite.antylogout.bypass")) return;
        boolean wasTagged = isTagged(player);
        tagged.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
        if (!wasTagged) {
            player.sendMessage(msg("start", "{czas}", String.valueOf(durationMs / 1000)));
        }
    }

    public void untag(Player player) {
        tagged.remove(player.getUniqueId());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = tagged.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            long left = entry.getValue() - now;
            if (left <= 0) {
                it.remove();
                player.sendMessage(msg("koniec"));
                player.sendActionBar(Component.empty());
            } else {
                long seconds = (left + 999) / 1000;
                player.sendActionBar(msg("actionbar", "{sekundy}", String.valueOf(seconds)));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage(msg("reload"));
            return true;
        }
        return false;
    }
}
