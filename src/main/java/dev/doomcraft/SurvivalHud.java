package dev.doomcraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** One bottom-anchored Doom console. Every value is live Minecraft or native Doom state. */
public final class SurvivalHud {
 private static final int W=640,H=72,INK=0xffd4ceb4,AMBER=0xffffc760;
 public static boolean active(){var mc=MinecraftClient.getInstance();return SurvivalMode.ENABLED&&NightCombat.active&&mc.player!=null&&!mc.player.isSpectator();}
 private static void text(DrawContext d,MinecraftClient mc,String s,int x,int y,int c){d.drawText(mc.textRenderer,s,x,y,c,true);}
 private static void well(DrawContext d,int x,int y,int w,int h){
  d.fill(x,y,x+w,y+h,0xff141512);d.fill(x+1,y+1,x+w-1,y+h-1,0xff373a33);
  d.fill(x,y+h-1,x+w,y+h,0xff77796b);d.fill(x+w-1,y,x+w,y+h,0xff77796b);
 }
 private static void patch(DrawContext d,String n,int x,int y){d.draw();int[] m=HudPatches.DATA.get(n);if(m!=null)d.drawTexture(Identifier.of("doomcraft","textures/hud/"+n+".png"),x,y,0,0,m[0],m[1],m[0],m[1]);}
 private static void number(DrawContext d,int n,int right,int y){String v=""+Math.max(0,n);for(int i=v.length()-1;i>=0;i--){right-=13;patch(d,"sttnum"+v.charAt(i),right,y);}}
 private static void stat(DrawContext d,MinecraftClient mc,String label,int value,int x){
  number(d,value,x+61,13);text(d,mc,label,x+9,37,INK);
 }
 private static void meter(DrawContext d,int x,int y,int w,float f,int color){d.fill(x,y,x+w,y+3,0xff181914);d.fill(x,y,x+Math.round(w*Math.max(0,Math.min(1,f))),y+2,color);}
 private static void item(DrawContext d,MinecraftClient mc,ItemStack stack,int x,int y){d.drawItem(stack,x,y);d.drawItemInSlot(mc.textRenderer,stack,x,y);}
 public static void render(DrawContext d,MinecraftClient mc){
  if(!active()||NightCombat.state.length<12)return;
  var p=mc.player;double[] s=NightCombat.state;int w=mc.getWindow().getScaledWidth(),h=mc.getWindow().getScaledHeight();float scale=w/(float)W;
  int top=h-Math.round(H*scale);boolean gun=NightCombat.isGun(p.getMainHandStack());
  if(gun&&mc.options.getPerspective().isFirstPerson()){
   float weaponScale=Math.min(w/320f,h/200f);
   DoomCraftClient.drawWeaponAt(d,mc,(int)s[2],(int)s[3],s[4],s[5],weaponScale,top);
   if(s[6]>=0)DoomCraftClient.drawWeaponAt(d,mc,(int)s[6],(int)s[7],s[4],s[5],weaponScale,top);
  }
  d.getMatrices().push();d.getMatrices().translate(0,top,0);d.getMatrices().scale(scale,scale,1);
  d.draw();d.drawTexture(Identifier.of("doomcraft","textures/hud/survival_console.png"),0,0,0,0,W,H,W,H);
  if(gun)number(d,(int)s[1],70,13);else text(d,mc,"--",37,19,INK);
  text(d,mc,"AMMO",22,37,INK);
  stat(d,mc,"HEALTH",Math.round(p.getHealth()*5),88);
  if(p.getAbsorptionAmount()>0)text(d,mc,"+"+Math.round(p.getAbsorptionAmount()*5),115,50,AMBER);
  // Doom's ARMS matrix becomes the actual nine selectable inventory slots.
  for(int i=0;i<9;i++){
   int x=185+(i%3)*30,y=5+(i/3)*20;well(d,x,y,28,20);
   if(p.getInventory().selectedSlot==i){d.drawBorder(x,y,28,20,AMBER);d.fill(x+1,y+1,x+7,y+19,0xff75633b);}
   text(d,mc,""+(i+1),x+1,y+2,p.getInventory().selectedSlot==i?AMBER:0xff979c89);
   item(d,mc,p.getInventory().getStack(i),x+9,y+2);
  }
  int pain=Math.max(0,Math.min(4,(int)((20-p.getHealth())/4)));
  String face=p.isAlive()?"stfst"+pain+((int)s[8]/35%3):"stfdead0";if(p.hurtTime>0)face="stfouch"+pain;
  well(d,291,4,56,64);d.getMatrices().push();d.getMatrices().translate(295,6,0);d.getMatrices().scale(2,2,1);patch(d,face,0,0);d.getMatrices().pop();
  stat(d,mc,"ARMOR",p.getArmor()*5,354);stat(d,mc,"FOOD",p.getHungerManager().getFoodLevel()*5,438);
  int air=Math.max(0,Math.round(p.getAir()*100f/p.getMaxAir()));
  text(d,mc,"AIR "+Math.min(100,air)+"%",443,51,air<25?0xffff6a54:INK);meter(d,443,63,62,air/100f,0xff85b1b1);
  text(d,mc,"BULL "+(int)s[10]+"/200",522,7,AMBER);text(d,mc,"SHEL "+(int)s[11]+"/50",522,19,AMBER);
  text(d,mc,"XP "+p.experienceLevel,522,32,0xffabc67f);meter(d,558,35,39,p.experienceProgress,0xffabc67f);
  well(d,609,5,24,24);item(d,mc,p.getOffHandStack(),613,9);text(d,mc,"OFF",612,32,INK);
  if(p.getVehicle() instanceof LivingEntity mount){text(d,mc,"RIDE "+Math.round(mount.getHealth())+"/"+Math.round(mount.getMaxHealth()),356,51,INK);meter(d,357,63,67,p.getMountJumpStrength(),AMBER);}
  else {text(d,mc,"READY",370,51,INK);meter(d,357,63,67,p.getAttackCooldownProgress(0),AMBER);}
  // Effect icons/durations remain in the console; inventory exposes full names and all effects.
  int i=0;for(var e:p.getStatusEffects()){
   if(!e.shouldShowIcon())continue;if(i>=4){text(d,mc,"+",625,52,AMBER);break;}
   int x=522+i*24;d.draw();d.drawSprite(x,46,0,16,16,mc.getStatusEffectSpriteManager().getSprite(e.getEffectType()));
   String duration=e.isInfinite()?"~":""+Math.max(0,e.getDuration()/20);text(d,mc,duration,x,63,INK);i++;
  }
  if(i==0)text(d,mc,"STATUS OK",522,52,0xffabc67f);
  String held=p.getMainHandStack().isEmpty()?"EMPTY HAND":p.getMainHandStack().getName().getString().toUpperCase(java.util.Locale.ROOT);
  well(d,7,55,165,15);text(d,mc,mc.textRenderer.trimToWidth(held,159),10,59,INK);
  d.draw();d.getMatrices().pop();
  if(mc.options.getPerspective().isFirstPerson()&&mc.currentScreen==null){
   int c=(int)s[8]-NightCombat.hitTick<5&&NightCombat.hitTick>0?AMBER:INK;
   d.fill(w/2-4,h/2,w/2-1,h/2+1,c);d.fill(w/2+2,h/2,w/2+5,h/2+1,c);d.fill(w/2,h/2-4,w/2+1,h/2-1,c);d.fill(w/2,h/2+2,w/2+1,h/2+5,c);
  }
 }
}
