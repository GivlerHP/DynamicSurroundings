package org.blockartistry.mod.DynSurround.mixins.early;

import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces Minecraft's credits music while retaining the vanilla music
 * ticker's start, stop, and screen transition behavior.
 */
@Mixin(MusicTicker.MusicType.class)
public abstract class MixinMusicType {

    private static final ResourceLocation DSURROUND_CREDITS = new ResourceLocation("dsurround:music.theend");

    @Inject(method = "getMusicTickerLocation", at = @At("HEAD"), cancellable = true)
    private void dsurround$getCreditsMusic(CallbackInfoReturnable<ResourceLocation> cir) {
        if ((Object) this == MusicTicker.MusicType.CREDITS) {
            cir.setReturnValue(DSURROUND_CREDITS);
        }
    }
}
