package me.gallowsdove.foxymachines.implementation.machines;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.github.mooy1.infinitylib.common.Scheduler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import lombok.SneakyThrows;
import me.gallowsdove.foxymachines.FoxyMachines;
import me.gallowsdove.foxymachines.Items;
import me.gallowsdove.foxymachines.utils.EmptySphereBlocks;
import me.gallowsdove.foxymachines.utils.SimpleLocation;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ForcefieldDome extends SlimefunItem implements EnergyNetComponent {

    private static final Set<Material> MATERIALS_TO_REPLACE = Set.of(Material.AIR, Material.CAVE_AIR, Material.WATER, Material.LAVA);
    private static final List<SimpleLocation> DOME_LOCATIONS = new ArrayList<>();
    private static final Set<BlockPosition> FORCEFIELD_BLOCKS = new HashSet<>();
    public static final ForcefieldDome INSTANCE = new ForcefieldDome();
    public static final int ENERGY_CONSUMPTION = 6000;

    public ForcefieldDome() {
        super(Items.MACHINES_ITEM_GROUP, Items.FORCEFIELD_DOME, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[]{
                Items.SWEETENED_SWEET_INGOT, Items.FORCEFIELD_STABILIZER, Items.SWEETENED_SWEET_INGOT,
                Items.FORCEFIELD_STABILIZER, Items.FORCEFIELD_ENGINE, Items.FORCEFIELD_STABILIZER,
                Items.SWEETENED_SWEET_INGOT, Items.FORCEFIELD_STABILIZER, Items.SWEETENED_SWEET_INGOT
        });

        this.addItemHandler(onTick(), onPlace(), onUse(), onBreak());
    }

    @Nonnull
    private BlockTicker onTick() {
        return new BlockTicker() {
            @ParametersAreNonnullByDefault
            public void tick(Block b, SlimefunItem sf, Config data) {
                Location l = b.getLocation();

                String active = BlockStorage.getLocationInfo(b.getLocation(), "active");

                if (getCharge(l) <= ENERGY_CONSUMPTION && active.equals("true")) {
                    setDomeInactive(b);
                }

                if (active.equals("true")) {
                    removeCharge(l, ENERGY_CONSUMPTION);
                }
            }

            public boolean isSynchronized() {
                return true;
            }
        };
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            @SneakyThrows
            public void onPlayerPlace(@Nonnull BlockPlaceEvent e) {
                Block b = e.getBlockPlaced();
                BlockStorage.addBlockInfo(b, "owner", e.getPlayer().getUniqueId().toString());
                BlockStorage.addBlockInfo(b, "active", "false");
                BlockStorage.addBlockInfo(b, "cooldown", "false");
                DOME_LOCATIONS.add(new SimpleLocation(b, "forcefield"));
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak(){
        return new BlockBreakHandler(false, false) {
            @Override
            @SneakyThrows
            public void onPlayerBreak(@Nonnull BlockBreakEvent e, @Nonnull ItemStack item, @Nonnull List<ItemStack> list) {
                Block b = e.getBlock();
                setDomeInactive(b);
                SimpleLocation loc = new SimpleLocation(b, "forcefield");
                if (DOME_LOCATIONS.contains(loc)) {
                    DOME_LOCATIONS.remove(loc);
                    saveDomeLocations();
                }
            }
        };
    }

    @Nonnull
    public BlockUseHandler onUse() {
        return e -> {
            // Remote Controller has Priority
            if (SlimefunUtils.isItemSimilar(e.getPlayer().getInventory().getItemInMainHand(), Items.REMOTE_CONTROLLER, true, false)) {
                return;
            }

            e.cancel();

            if (e.getClickedBlock().isPresent()) {
                switchActive(e.getClickedBlock().get(), e.getPlayer());
            }
        };
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return ENERGY_CONSUMPTION * 4;
    }

    private void setDomeActive(@Nonnull Block b) {
        List<Block> domeBlocks = EmptySphereBlocks.get(b, 32);

        for (Block block : domeBlocks) {
            UUID uuid = UUID.fromString(BlockStorage.getLocationInfo(b.getLocation(), "owner"));
            if (Slimefun.getProtectionManager().hasPermission(Bukkit.getOfflinePlayer(uuid), block, Interaction.BREAK_BLOCK)) {
                if (MATERIALS_TO_REPLACE.contains(block.getType())) {
                    block.setType(Material.BARRIER);
                } else if (block.getType() != Material.BARRIER) {
                    FORCEFIELD_BLOCKS.add(new BlockPosition(block));
                }
            }
        }

        BlockStorage.addBlockInfo(b, "active", "true");
    }

    private void setDomeInactive(@Nonnull Block b) {
        List<Block> domeBlocks = EmptySphereBlocks.get(b, 32);

        for (Block block: domeBlocks) {
            UUID uuid = UUID.fromString(BlockStorage.getLocationInfo(b.getLocation(), "owner"));
            if (Slimefun.getProtectionManager().hasPermission(Bukkit.getOfflinePlayer(uuid), block, Interaction.BREAK_BLOCK)) {
                if (block.getType() == Material.BARRIER) {
                    block.setType(Material.AIR);
                } else {
                    FORCEFIELD_BLOCKS.remove(new BlockPosition(block));
                }
            }
        }

        BlockStorage.addBlockInfo(b, "active", "false");
    }

    public void switchActive(@Nonnull Block b, @Nonnull Player p) {
        if (!BlockStorage.getLocationInfo(b.getLocation(), "cooldown").equals("false")) {
            p.sendMessage(ChatColor.LIGHT_PURPLE + "You must wait at least 10 seconds before activating the dome again.");
            return;
        }

        String active = BlockStorage.getLocationInfo(b.getLocation(), "active");
        if (active.equals("false")) {
            if (getCharge(b.getLocation()) < ENERGY_CONSUMPTION) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "You don't have enough energy.");
                return;
            }

            setDomeActive(b);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "The dome has been activated.");
        } else {
            setDomeInactive(b);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "The dome has been deactivated.");

        }

        BlockStorage.addBlockInfo(b, "cooldown", "true");
        Scheduler.runAsync(200, () -> BlockStorage.addBlockInfo(b, "cooldown", "false"));
    }

    public void setupDomes() {
        for (SimpleLocation loc: DOME_LOCATIONS) {
            World w = Bukkit.getWorld(UUID.fromString(loc.getWorldUUID()));
            if (w == null) {
                DOME_LOCATIONS.remove(loc);
                continue;
            }

            Block b = w.getBlockAt(loc.getX(), loc.getY(), loc.getZ());
            if (BlockStorage.getLocationInfo(b.getLocation(), "active").equals("true")) {
                setDomeActive(b);
            }

            BlockStorage.addBlockInfo(b, "cooldown", "false");
        }
    }

    public static List<SimpleLocation> getDomeLocations() {
        return DOME_LOCATIONS;
    }

    public static Set<BlockPosition> getForcefieldBlocks() {
        return FORCEFIELD_BLOCKS;
    }

    public static File prepareFiles() throws IOException {
        String pluginFolder = FoxyMachines.getInstance().folderPath;

        File file = new File(pluginFolder + File.separator + "domedata");
        File filePath = new File(pluginFolder);

        filePath.mkdirs();
        if (!file.exists()) {
            file.createNewFile();
        }

        return file;
    }

    public static void saveDomeLocations() throws IOException {
        File file = prepareFiles();
        Gson gson = new Gson();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(gson.toJson(DOME_LOCATIONS));
        }
    }

    public static void loadDomeLocations() throws IOException {
        File file = prepareFiles();
        Gson gson = new Gson();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Type type = new TypeToken<ArrayList<SimpleLocation>>() {}.getType();
            String json = reader.readLine();
            DOME_LOCATIONS.addAll(gson.fromJson(json, type));
        }
    }

}

