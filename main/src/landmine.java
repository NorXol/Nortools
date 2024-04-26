package nor.tools

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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import net.minecraft.network.protocol.game.PacketPlayOutWorldParticles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Nortools extends JavaPlugin implements Listener {
    private Map<Location, UUID> landmines = new HashMap<>();
    private boolean adminModeEnabled = true;

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

            if (adminModeEnabled || player.hasPermission("ntools.admin")) {
                ItemMeta meta = landmineStick.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.RED + "Landmine Stick");
                    landmineStick.setItemMeta(meta);
                }

                player.getInventory().addItem(landmineStick);
                player.sendMessage(ChatColor.GREEN + "You have received a landmine stick.");
            } else {
                player.sendMessage(ChatColor.RED + "Admin mode is currently disabled. You cannot receive landmine sticks.");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("ntools") && args.length > 0 && sender instanceof Player) {
            Player player = (Player) sender;
            if (args[0].equalsIgnoreCase("troll") && args.length > 1 && args[1].equalsIgnoreCase("landmines")) {
                if (args.length > 2 && args[2].equalsIgnoreCase("enable")) {
                    adminModeEnabled = true;
                    player.sendMessage(ChatColor.GREEN + "Admin mode enabled. Landmine sticks can now be obtained.");
                } else if (args.length > 2 && args[2].equalsIgnoreCase("disable")) {
                    adminModeEnabled = false;
                    player.sendMessage(ChatColor.RED + "Admin mode disabled. Landmine sticks cannot be obtained.");
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid command usage. Usage: /ntools troll landmines (enable/disable)");
                }
                return true;
            }
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
            player.sendMessage(ChatColor.GREEN + "Landmine placed at " + location.toString());

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

            applyGlowingOutline(location, 12); // Apply glowing outline within a 12-block radius
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
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Landmine disarmed.");
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot disarm landmines placed by another player.");
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
                player.sendMessage(ChatColor.GREEN + "All your landmines disarmed.");
            }
        }
    }

    // New method to apply glowing outline to landmines within a radius
    private void applyGlowingOutline(Location center, int radius) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = player.getLocation();
            if (playerLocation.distanceSquared(center) <= radius * radius) {
                for (Location loc : landmines.keySet()) {
                    if (loc.getWorld() == playerLocation.getWorld()) {
                        Block block = loc.getBlock();
                        sendParticlePacket(player, block.getX(), block.getY(), block.getZ());
                    }
                }
            }
        }
    }

    // Method to send particle packet to players
    private void sendParticlePacket(Player player, double x, double y, double z) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                net.minecraft.world.phys.Vec3D.c(x + 0.5, y + 0.5, z + 0.5), // Particle position
                1, // Particle count
                0, // Offset X
                0, // Offset Y
                0, // Offset Z
                0, // Particle speed
                0, // Particle data
                1); // Force particle to all players within viewing distance
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }
            }
