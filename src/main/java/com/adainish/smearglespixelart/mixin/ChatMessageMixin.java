package com.adainish.smearglespixelart.mixin;

import com.adainish.smearglespixelart.MiniMessageText;
import com.adainish.smearglespixelart.PermissionNodes;
import com.adainish.smearglespixelart.SmearglesPixelArtMod;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatMessageMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(SignedMessage message, CallbackInfo ci) {
        if (player == null || !SmearglesPixelArtMod.getManager().shouldBlockChat(player.getUuid(), Permissions.check(player, PermissionNodes.CHAT_BYPASS))) {
            return;
        }

        player.sendMessage(
            MiniMessageText.deserialize(player.getServer(), "<red>Registered Smeargle players cannot use public chat during the event. Use</red> <yellow>/guess <pokemon></yellow><red>.</red>"),
            false
        );
        ci.cancel();
    }
}
