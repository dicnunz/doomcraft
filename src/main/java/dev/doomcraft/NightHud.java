package dev.doomcraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class NightHud {
 private static void patch(DrawContext d,String name,int x,int y){int[] m=HudPatches.DATA.get(name);if(m!=null)d.drawTexture(Identifier.of("doomcraft","textures/hud/"+name+".png"),x,y,0,0,m[0],m[1],m[0],m[1]);}
 private static void number(DrawContext d,int value,int right,int y){String s=""+Math.max(0,value);for(int i=s.length()-1;i>=0;i--){String name="sttnum"+s.charAt(i);int[] m=HudPatches.DATA.get(name);right-=m[0];patch(d,name,right,y);}}
 private static void small(DrawContext d,int value,int right,int y){String s=""+value;for(int i=s.length()-1;i>=0;i--){right-=4;patch(d,"stysnum"+s.charAt(i),right,y);}}
 public static void render(DrawContext d,MinecraftClient mc){renderAt(d,mc,0,Float.MAX_VALUE,true);}
 public static void renderAt(DrawContext d,MinecraftClient mc,int offset,float maxScale,boolean crosshair){
  if(mc.player==null||NightCombat.state.length<12)return;double[] s=NightCombat.state;
  int w=mc.getWindow().getScaledWidth(),h=mc.getWindow().getScaledHeight();float scale=Math.min(maxScale,Math.min(w/320f,h/180f));int barH=Math.round(32*scale);
  if(NightCombat.isGun(mc.player.getMainHandStack())&&mc.options.getPerspective().isFirstPerson()){
   DoomCraftClient.drawWeaponAt(d,mc,(int)s[2],(int)s[3],s[4],s[5],scale,h-barH-offset);
   if(s[6]>=0)DoomCraftClient.drawWeaponAt(d,mc,(int)s[6],(int)s[7],s[4],s[5],scale,h-barH-offset);
  }
  d.getMatrices().push();d.getMatrices().translate((w-320*scale)/2,h-barH-offset,0);d.getMatrices().scale(scale,scale,1);
  patch(d,"stbar",0,0);patch(d,"starms",104,0);
  number(d,(int)s[1],44,3);number(d,Math.round(mc.player.getHealth()*5),90,3);patch(d,"sttprc",90,3);
  number(d,mc.player.getArmor()*5,221,3);patch(d,"sttprc",221,3);
  int pain=Math.min(4,(int)((20-mc.player.getHealth())/4));String face=mc.player.isAlive()?"stfst"+pain+((int)s[8]/35%3):"stfdead0";
  if(mc.player.hurtTime>0)face="stfouch"+pain;patch(d,face,143,0);
  patch(d,s[9]==1?"stysnum2":"stgnum2",111,4);patch(d,s[9]==2?"stysnum3":"stgnum3",123,4);
  small(d,(int)s[10],288,5);small(d,200,314,5);
  small(d,(int)s[11],288,11);small(d,50,314,11);
  small(d,0,288,17);small(d,0,314,17);small(d,0,288,23);small(d,0,314,23);
  d.getMatrices().pop();
  if(!crosshair)return;
  int c=(int)s[8]-NightCombat.hitTick<5&&NightCombat.hitTick>0?0xffffbb77:0xffddd8bb;
  d.fill(w/2-4,h/2,w/2-1,h/2+1,c);d.fill(w/2+2,h/2,w/2+5,h/2+1,c);
  d.fill(w/2,h/2-4,w/2+1,h/2-1,c);d.fill(w/2,h/2+2,w/2+1,h/2+5,c);
 }
}
