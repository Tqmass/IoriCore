package us.iorilabs.staffreg;

import java.util.concurrent.CompletableFuture;

public interface DiscordPort {
    CompletableFuture<String> sendDirectMessage(DiscordMessageRequest request);

    CompletableFuture<String> sendChannelMessage(DiscordMessageRequest request);

    CompletableFuture<Void> editMessage(String channelId, String messageId, DiscordMessageRequest request);
}
