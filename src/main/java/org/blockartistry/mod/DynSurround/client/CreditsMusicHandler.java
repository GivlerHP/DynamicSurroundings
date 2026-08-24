package org.blockartistry.mod.DynSurround.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Owns the credits music independently of MusicTicker. GuiWinGame pauses an
 * integrated server, which also prevents the vanilla music ticker from
 * updating, so the track must be started when the screen is opened.
 */
@SideOnly(Side.CLIENT)
public final class CreditsMusicHandler {

    private static final ResourceLocation CREDITS = new ResourceLocation("dsurround:music.theend");
    private static ISound activeSound;

    private CreditsMusicHandler() {
    }

    public static void initialize() {
        MinecraftForge.EVENT_BUS.register(new CreditsMusicHandler());
    }

    @SubscribeEvent
    public void onGuiOpen(final GuiOpenEvent event) {
        if (event.gui instanceof GuiWinGame) {
            play();
        } else {
            stop();
        }
    }

    public static void ensurePlaying() {
        if (activeSound == null) {
            play();
        }
    }

    private static void play() {
        final Minecraft minecraft = Minecraft.getMinecraft();

        // Stop End/boss music before starting the credits track. This also
        // clears MusicTicker's underlying sound from the sound engine.
        minecraft.getSoundHandler().stopSounds();
        activeSound = PositionedSoundRecord.func_147673_a(CREDITS);
        minecraft.getSoundHandler().playSound(activeSound);
    }

    private static void stop() {
        if (activeSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(activeSound);
            activeSound = null;
        }
    }
}
