package dev.doomcraft.mixin;
import dev.doomcraft.SurvivalMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractClientPlayerEntity.class)
public class MarineSkinMixin {
 @Inject(method="getSkinTextures",at=@At("RETURN"),cancellable=true)
 private void doomcraft$marine(CallbackInfoReturnable<SkinTextures> result){
  var mc=MinecraftClient.getInstance();var self=(AbstractClientPlayerEntity)(Object)this;
  if(SurvivalMode.ENABLED&&mc.player!=null&&self.getUuid().equals(mc.player.getUuid())){
   var old=result.getReturnValue();result.setReturnValue(new SkinTextures(Identifier.of("doomcraft","textures/entity/marine.png"),null,old.capeTexture(),old.elytraTexture(),SkinTextures.Model.WIDE,false));
  }
 }
}
