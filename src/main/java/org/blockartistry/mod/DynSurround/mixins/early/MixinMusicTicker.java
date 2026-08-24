package org.blockartistry.mod.DynSurround.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import org.blockartistry.mod.DynSurround.client.CreditsMusicHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts the credits track immediately. Vanilla 1.7.10 can otherwise assign a
 * random delay of up to roughly Integer.MAX_VALUE / 2 ticks when switching
 * from the End music to the credits music.
 */
@Mixin(MusicTicker.class)
public abstract class MixinMusicTicker {

    @Shadow
    @Final
    private Minecraft field_147677_b;

    @Shadow
    private int field_147676_d;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void dsurround$playCreditsImmediately(CallbackInfo ci) {
        if (this.field_147677_b.func_147109_W() != MusicTicker.MusicType.CREDITS) {
            return;
        }

        CreditsMusicHandler.ensurePlaying();
        this.field_147676_d = Integer.MAX_VALUE;
        ci.cancel();
    }
}
