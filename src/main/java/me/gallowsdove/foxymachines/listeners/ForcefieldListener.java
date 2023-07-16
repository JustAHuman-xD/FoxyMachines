package me.gallowsdove.foxymachines.listeners;

import io.github.mooy1.infinitylib.common.Scheduler;
import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import me.gallowsdove.foxymachines.implementation.machines.ForcefieldDome;
import me.gallowsdove.foxymachines.utils.SimpleLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ForcefieldListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerBreak(@Nonnull BlockBreakEvent e) {
        handleBlockChange(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onExplosionBreak(@Nonnull BlockExplodeEvent e) {
        handleBlockChange(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onBurnBreak(@Nonnull BlockBurnEvent e) {
        handleBlockChange(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onLeavesDecay(@Nonnull LeavesDecayEvent e) {
        handleBlockChange(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onFadeBreak(@Nonnull BlockFadeEvent e) {
        handleBlockChange(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onBlockDropEvent(@Nonnull EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof FallingBlock)) {
            return;
        }

        handleBlockChange(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onBlocksBreakByExplosiveToolEvent(@Nonnull ExplosiveToolBreakBlocksEvent e) {
        e.getAdditionalBlocks().forEach(this::handleBlockChange);
    }

    private void handleBlockChange(Block b) {
        if (ForcefieldDome.getForcefieldBlocks().remove(new BlockPosition(b))) {
            Scheduler.run(() -> b.setType(Material.BARRIER));
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerTeleport(@Nonnull PlayerTeleportEvent e) {
        // Only care about vanilla teleports
        if (e.getCause() != TeleportCause.ENDER_PEARL && e.getCause() != TeleportCause.CHORUS_FRUIT) {
            return;
        }

        Location l = e.getTo();

        // If for some reason the location is null
        if (l == null) {
            return;
        }

        for (SimpleLocation loc : ForcefieldDome.getDomeLocations()) {
            if (e.getPlayer().getWorld() != Bukkit.getServer().getWorld(UUID.fromString(loc.getWorldUUID()))) {
                continue;
            }

            int xDif = (int) (l.getX() - loc.getX());
            int yDif = (int) (l.getY() - loc.getY());
            int zDif = (int) (l.getZ() - loc.getZ());
            if (Math.floor(Math.sqrt((xDif * xDif) + (yDif * yDif) + (zDif * zDif))) <= 32) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "You can't teleport to a dome!");
                break;
            }
        }
    }
}
