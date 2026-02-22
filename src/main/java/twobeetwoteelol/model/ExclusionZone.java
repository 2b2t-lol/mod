package twobeetwoteelol.model;

import net.minecraft.util.math.BlockPos;
import twobeetwoteelol.utils.Coordinates;

public record ExclusionZone(int x, int z, int radius) {
    public boolean matches(BlockPos signPos) {
        return Coordinates.inRadius(x, z, radius, signPos);
    }
}
