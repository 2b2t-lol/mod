package twobeetwoteelol.utils;

import net.minecraft.util.math.BlockPos;
import twobeetwoteelol.model.ExclusionZone;

import java.util.List;

public final class Coordinates {
    public static boolean isExcludedZone(List<ExclusionZone> zones, BlockPos signPos) {
        for (ExclusionZone zone : zones) {
            if (zone.matches(signPos)) {
                return true;
            }
        }

        return false;
    }

    public static boolean inRadius(int x, int z, int radius, BlockPos pos) {
        long dx = pos.getX() - x;
        long dz = pos.getZ() - z;
        long distanceSq = dx * dx + dz * dz;
        long radiusSq = (long) radius * radius;

        return distanceSq <= radiusSq;
    }
}
