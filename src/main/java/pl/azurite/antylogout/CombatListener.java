package pl.azurite.antylogout;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

public final class CombatListener implements Listener {

    private final AntyLogoutPlugin plugin;

    public CombatListener(AntyLogoutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null || attacker.equals(victim)) return;

        plugin.tag(victim);
        plugin.tag(attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isTagged(player)) return;

        String text = event.getMessage().substring(1).trim();
        String label = text.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        if (plugin.isAllowed(label)) return;

        event.setCancelled(true);
        player.sendMessage(plugin.msg("komenda-zablokowana"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isTagged(player)) return;
        plugin.untag(player);
        if (Bukkit.isStopping()) return;

        player.setHealth(0.0);
        Bukkit.broadcast(plugin.msg("wyjscie", "{gracz}", player.getName()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.untag(event.getPlayer());
    }
}
