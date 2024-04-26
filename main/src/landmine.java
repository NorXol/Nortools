package not.tools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Nortools extends JavaPlugin implements Listener {
    private Map<Location, UUID> landmines = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("landmine") && sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack landmineStick = new ItemStack(Material.STICK);
            landmineStick.setCustomModelData(1); // Just an example for setting custom model data
            player.getInventory().addItem(landmineStick);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (item.getCustomModelData() == 1) {
            Location location = event.getClickedBlock().getLocation();
            landmines.put(location, player.getUniqueId());
            player.sendMessage("Landmine placed at " + location.toString());

            // Schedule detonation after player steps off
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (landmines.containsKey(location)) {
                        landmines.remove(location);
                        location.getWorld().createExplosion(location, 6, true, true);
                    }
                }
            }.runTaskLater(this, 20 * 2); // 2 seconds delay, adjust as needed

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerShiftLeftClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking()) {
            Location location = event.getClickedBlock().getLocation();
            if (landmines.containsKey(location)) {
                UUID ownerUUID = landmines.get(location);
                if (ownerUUID.equals(event.getPlayer().getUniqueId())) {
                    landmines.remove(location);
                    event.getPlayer().sendMessage("Landmine disarmed.");
                } else {
                    event.getPlayer().sendMessage("You cannot disarm landmines placed by another player.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerShiftDrop(PlayerInteractEvent event) {
        if (event.getAction() == Action.DROP && event.getPlayer().isSneaking()) {
            Player player = event.getPlayer();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            if (offHandItem != null && offHandItem.getType() == Material.STICK && offHandItem.hasItemMeta() && offHandItem.getItemMeta().hasCustomModelData() && offHandItem.getCustomModelData() == 1) {
                UUID playerUUID = player.getUniqueId();
                landmines.entrySet().removeIf(entry -> entry.getValue().equals(playerUUID));
                player.sendMessage("All your landmines disarmed.");
            }
        }
    }
          }
