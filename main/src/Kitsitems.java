package nor.tools

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NTools extends JavaPlugin implements Listener {

    private BossBar bossBar;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && event.getRightClicked().getName().equalsIgnoreCase("KitMasterGamer")) {
            startBossBar(player);
            event.setCancelled(true); // Prevents the player from interacting with KitMasterGamer for other purposes
        }
    }

    private void startBossBar(Player player) {
        bossBar = Bukkit.createBossBar("Searching...", BarColor.PINK, BarStyle.SOLID);
        bossBar.setProgress(0);
        bossBar.addPlayer(player);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            double progress = bossBar.getProgress() + 1.0 / (8 * 20); // Increase progress every tick over 8 seconds
            bossBar.setProgress(Math.min(progress, 1.0));

            if (progress >= 1.0) {
                bossBar.removeAll();
                giveItem(player);
            }
        }, 0L, 1L);
    }

    private void giveItem(Player player) {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        sword.addEnchantment(Enchantment.KNOCKBACK, -1); // Adding knockback -1
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Kits old sword");
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "durability"), PersistentDataType.INTEGER, -1);
            Date date = getRandomDate();
            meta.setLore(Collections.singletonList("Last used " + date.toString()));
            sword.setItemMeta(meta);
            player.getInventory().addItem(sword);
        }
    }

    private Date getRandomDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.APRIL, 8); // Start date
        long startMillis = calendar.getTimeInMillis();
        calendar.set(2023, Calendar.NOVEMBER, 25); // End date
        long endMillis = calendar.getTimeInMillis();
        long randomMillisSinceEpoch = ThreadLocalRandom.current().nextLong(startMillis, endMillis);
        return new Date(randomMillisSinceEpoch);
    }
}
