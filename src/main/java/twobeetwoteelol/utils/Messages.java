package twobeetwoteelol.utils;

import net.minecraft.client.MinecraftClient;

import java.util.function.BooleanSupplier;

public final class Messages {

    public static void chatError(
        MinecraftClient mc,
        BooleanSupplier isActive,
        MessageSender sender,
        String message,
        Object... args
    ) {
        mc.execute(() -> {
            if (isActive.getAsBoolean()) {
                sender.send(message, args);
            }
        });
    }

    @FunctionalInterface
    public interface MessageSender {
        void send(String message, Object... args);
    }
}
