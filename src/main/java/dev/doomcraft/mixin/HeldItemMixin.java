package dev.doomcraft.mixin;
import dev.doomcraft.DoomCraft;
import dev.doomcraft.NightCombat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HeldItemRenderer.class)
public class HeldItemMixin {
 @Inject(method="renderFirstPersonItem",at=@At("HEAD"),cancellable=true)
 private void doomcraft$hideNativePistolHand(CallbackInfo ci){
  var p=MinecraftClient.getInstance().player;
  if(p!=null&&((DoomCraft.active&&p.getMainHandStack().isOf(DoomCraft.PISTOL))||(NightCombat.active&&NightCombat.isGun(p.getMainHandStack()))))ci.cancel();
 }
}
