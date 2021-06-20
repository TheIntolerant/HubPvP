package me.quared.hubpvp.listeners;

import me.quared.hubpvp.HubPvP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Listeners implements Listener {
    
    public HashMap<Player, Integer> pvpTime = new HashMap<>();
    public HashMap<Player, BukkitRunnable> pvpTask = new HashMap<>();
    public ArrayList<Player> pvp = new ArrayList<>();
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        int slot = HubPvP.getPlugin().getConfig().getInt("slot");
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        try {
            swordMeta.spigot().setUnbreakable(true);
        } catch (NoSuchMethodError ignored) {
            try {
                ItemMeta.class.getMethod("setUnbreakable", Boolean.class).invoke(swordMeta, true);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored2) {
            
            }
        }
        swordMeta.setDisplayName(HubPvP.getPlugin().format(HubPvP.getPlugin().getConfig().getString("name")));
        swordMeta.setLore(Collections.singletonList(HubPvP.getPlugin().format("&7Hold to PvP!")));
        sword.setItemMeta(swordMeta);
        p.getInventory().setItem(slot - 1, sword);
    }
    
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            Player vic = (Player)e.getEntity();
            Player dam = (Player)e.getDamager();
            String world = vic.getLocation().getWorld().getName();
    
            for (String s : HubPvP.getPlugin().getConfig().getStringList("disabled-worlds")) {
                if (s.equalsIgnoreCase(world)) {
                    e.setCancelled(true);
                }
            }
            
            if (!this.pvp.contains(vic) || !this.pvp.contains(dam)) {
                e.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        HubPvP plugin = HubPvP.getPlugin();
        if (e.getEntity() instanceof Player || e.getEntity().getKiller() instanceof Player) {
            Player p = e.getEntity();
            Player killer = p.getKiller();
            p.setHealth(20.0D);
            this.pvp.remove(p);
            this.pvpTime.remove(p);
            this.pvpTask.remove(p);
            p.teleport(p.getLocation().add(0.0D, 1.0D, 0.0D));
            
            p.getInventory().setHeldItemSlot(0);
            p.sendMessage(plugin.format(plugin.getConfig().getString("killed-message")).replace("%killer%", killer.getDisplayName()));
            killer.sendMessage(
                    plugin.format(plugin.getConfig().getString("killed-other-message")).replace("%killed%", p.getDisplayName()));
            p.getInventory().setHelmet(new ItemStack(Material.AIR));
            p.getInventory().setChestplate(new ItemStack(Material.AIR));
            p.getInventory().setLeggings(new ItemStack(Material.AIR));
            p.getInventory().setBoots(new ItemStack(Material.AIR));
            e.setDeathMessage(null);
        }
    }
    
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
        final Player p = e.getPlayer();
        int slot = e.getNewSlot();
        int pvpSlot = HubPvP.getPlugin().getConfig().getInt("slot") - 1;
        if (slot == pvpSlot) {
            if (!this.pvp.contains(p) && !this.pvpTime.containsKey(p) && !this.pvpTask.containsKey(p)) {
                p.sendMessage(HubPvP.getPlugin().format(HubPvP.getPlugin().getConfig().getString("pvp-enabled-message")));
                this.pvpTime.put(p, HubPvP.getPlugin().getConfig().getInt("cooldown"));
                this.pvp.add(p);
                p.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                p.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                p.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
        } else if (this.pvp.contains(p) && this.pvpTime.containsKey(p)) {
            this.pvpTask.put(p, new BukkitRunnable() {
                public void run() {
                    if (!Listeners.this.pvpTime.containsKey(p)) {
                        this.cancel();
                    } else {
                        Listeners.this.pvpTime.put(p, Listeners.this.pvpTime.get(p) - 1);
                        if (Listeners.this.pvpTime.get(p) == 0) {
                            Listeners.this.pvpTime.remove(p);
                            Listeners.this.pvpTask.remove(p);
                            Listeners.this.pvp.remove(p);
                            p.sendMessage(HubPvP.getPlugin().format(HubPvP.getPlugin().getConfig().getString("pvp-disabled-message")));
                            p.getInventory().setArmorContents(new ItemStack[4]);
                            this.cancel();
                        } else {
                            p.sendMessage(HubPvP.getPlugin().format(HubPvP.getPlugin().getConfig().getString("pvp-disabling-message").replaceAll("%time%", Integer.toString(Listeners.this.pvpTime.get(p)))));
                        }
                    }
                    
                }
            });
            (this.pvpTask.get(p)).runTaskTimer(HubPvP.getPlugin(), 0L, 20L);
        }
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        if (item.getType().equals(Material.DIAMOND_SWORD) && item.hasItemMeta()
                && ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(HubPvP.getPlugin().getConfig().getString("name")))) {
            e.setCancelled(true);
        }
        
        if (e.getSlotType() == InventoryType.SlotType.ARMOR) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (item.getType().equals(Material.DIAMOND_SWORD)
                && item.hasItemMeta()
                && ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(HubPvP.getPlugin().getConfig().getString("name")))) {
            e.setCancelled(true);
        }
    }
    
}
