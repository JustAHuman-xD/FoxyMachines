package me.gallowsdove.foxymachines.utils;

import org.bukkit.block.Block;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class EmptySphereBlocks {
    private EmptySphereBlocks() {}

    public static List<Block> get(@Nonnull Block target, int radius) {
        List<Block> blocks = new ArrayList<>();
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.floor(Math.sqrt(x * x + y * y + z * z)) == radius) {
                        final Block b = target.getWorld().getBlockAt(x + target.getX(), y + target.getY(), z + target.getZ());
                        blocks.add(b);
                    }
                }
            }
        }
        return blocks;
    }
}
