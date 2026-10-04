package dev.doomcraft.mixin;
import dev.doomcraft.NightCombat;
import dev.doomcraft.SurvivalHud;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public class NightHudMixin {
 @Inject(method={"renderHotbar","renderStatusBars","renderExperienceBar","renderMountHealth","renderCrosshair"},at=@At("HEAD"),cancellable=true)
 private void doomcraft$classicStatus(CallbackInfo ci){if(SurvivalHud.active()||(NightCombat.active&&!dev.doomcraft.SurvivalMode.ENABLED))ci.cancel();}
 @Inject(method={"renderExperienceLevel","renderHeldItemTooltip","renderStatusEffectOverlay","renderMountJumpBar"},at=@At("HEAD"),cancellable=true)
 private void doomcraft$survivalConsole(CallbackInfo ci){if(SurvivalHud.active())ci.cancel();}

 // Transient vanilla messages stay readable above the console, including dismount prompts.
 @Inject(method={"renderOverlayMessage","renderChat"},at=@At("HEAD"))
 private void doomcraft$raiseMessages(net.minecraft.client.gui.DrawContext d,net.minecraft.client.render.RenderTickCounter tick,CallbackInfo ci){
  if(SurvivalHud.active()){d.getMatrices().push();d.getMatrices().translate(0,Math.min(0,40-net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth()*72f/640f),0);}
 }
 @Inject(method={"renderOverlayMessage","renderChat"},at=@At("RETURN"))
 private void doomcraft$restoreMessages(net.minecraft.client.gui.DrawContext d,net.minecraft.client.render.RenderTickCounter tick,CallbackInfo ci){if(SurvivalHud.active())d.getMatrices().pop();}
}
