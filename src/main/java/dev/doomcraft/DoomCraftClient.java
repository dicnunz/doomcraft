package dev.doomcraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import java.util.*;

public final class DoomCraftClient implements ClientModInitializer {
 private static final Map<Integer,int[]> META=new HashMap<>();
 static {for(int[] a:SpriteData.DATA)META.put(a[0]*32+a[1],a);}
 private static Identifier texture(int s,int f){return Identifier.of("doomcraft","textures/sprites/"+s+"_"+f+".png");}
 @Override public void onInitializeClient(){
  LabAutomation.install();
  SurvivalDemo.install();
  EntityRendererRegistry.register(DoomCraft.DEMON,context->new EntityRenderer<DoomCraft.Demon>(context){
   @Override public Identifier getTexture(DoomCraft.Demon e){return texture(0,0);}
   @Override public void render(DoomCraft.Demon e,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light){}
  });
  WorldRenderEvents.AFTER_ENTITIES.register(context->{
   double[] s=DoomCraft.state;if(!DoomCraft.active||s.length<18)return;
   MatrixStack m=context.matrixStack();if(m==null||context.consumers()==null)return;
   var camera=context.camera();var pos=camera.getPos();
   for(int i=10;i+7<s.length;i+=8){
    int sprite=(int)s[i+3],frame=(int)s[i+4];int[] data=META.get(sprite*32+frame);if(data==null)continue;
    m.push();m.translate(s[i]-pos.x,DoomCraft.FLOOR+s[i+2]-pos.y,s[i+1]-pos.z);
    m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-camera.getYaw()));
    float w=data[2]/32f,h=data[3]/32f;
    MatrixStack.Entry entry=m.peek();Matrix4f matrix=entry.getPositionMatrix();
    VertexConsumer v=context.consumers().getBuffer(RenderLayer.getEntityCutoutNoCull(texture(sprite,frame)));
    // Doom patches remain world-space billboards and depth-test against Minecraft blocks.
    vertex(v,entry,matrix,-w/2,0,0,1);vertex(v,entry,matrix,w/2,0,1,1);
    vertex(v,entry,matrix,w/2,h,1,0);vertex(v,entry,matrix,-w/2,h,0,0);
    m.pop();
   }
  });
  HudRenderCallback.EVENT.register((draw,tick)->{
   var mc=MinecraftClient.getInstance();double[] s=DoomCraft.state;
   if(NightCombat.active){if(!mc.options.hudHidden){if(SurvivalMode.ENABLED)SurvivalHud.render(draw,mc);else NightHud.render(draw,mc);}return;}
   if(!DoomCraft.active||s.length<10||mc.player==null)return;
   if(DeveloperDemos.ENABLED)DeveloperDemos.hud(draw,mc,s);
   else draw.drawTextWithShadow(mc.textRenderer,"DOOM C / FREEDOOM   Ammo "+(int)s[1]+"   Demon "+Math.max(0,(int)s[9])+"   35 Hz",8,8,0xffeeeecc);
   if(!DeveloperDemos.ENABLED&&s[9]<=0)draw.drawTextWithShadow(mc.textRenderer,"Demon defeated — /doomcraft to reset",8,22,0xff88ff88);
   if(mc.player.getMainHandStack().isOf(DoomCraft.PISTOL)&&mc.options.getPerspective().isFirstPerson()){
    drawWeapon(draw,mc,(int)s[2],(int)s[3],s[4],s[5]);
    if(s[6]>=0)drawWeapon(draw,mc,(int)s[6],(int)s[7],s[4],s[5]);
   }
  });
 }
 private static void vertex(VertexConsumer v,MatrixStack.Entry e,Matrix4f m,float x,float y,float u,float t){
  v.vertex(m,x,y,0).color(255,255,255,255).texture(u,t).overlay(OverlayTexture.DEFAULT_UV).light(0x00f000f0).normal(e,0,0,1);
 }
 private static void drawWeapon(net.minecraft.client.gui.DrawContext draw,MinecraftClient mc,int sprite,int frame,double sx,double sy){
  int[] d=META.get(sprite*32+frame);if(d==null)return;
  float scale=Math.min(mc.getWindow().getScaledWidth()/320f,mc.getWindow().getScaledHeight()/200f)*(DeveloperDemos.ENABLED?.88f:1.2f);
  drawWeaponAt(draw,mc,sprite,frame,sx,sy,scale,mc.getWindow().getScaledHeight()-25);
 }
 public static void drawWeaponAt(net.minecraft.client.gui.DrawContext draw,MinecraftClient mc,int sprite,int frame,double sx,double sy,float scale,int bottom){
  int[] d=META.get(sprite*32+frame);if(d==null)return;
  int screenW=mc.getWindow().getScaledWidth(),screenH=mc.getWindow().getScaledHeight();
  draw.getMatrices().push();draw.getMatrices().translate(screenW/2f,bottom,0);draw.getMatrices().scale(scale,scale,1);
  // WAD patch offsets are relative to Doom's 320x200 psprite coordinate system.
  draw.drawTexture(texture(sprite,frame),(int)(sx-d[4]-160),(int)(sy-d[5]-200),0,0,d[2],d[3],d[2],d[3]);
  draw.getMatrices().pop();
 }
}
