package dev.doomcraft;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import java.nio.file.*;
import java.util.Locale;

/** Capture-only scene/input choreography. Native gameplay code is not changed. */
public final class DeveloperDemos {
 public static final boolean ENABLED=Boolean.getBoolean("doomcraft.devDemos");
 private static final String[] NAMES={"cover","pistol","melee"};
 private static int scene=-1,tick,frame,endAt=-1,hitAt=-1,firstDamage=-1,mineIndex=0;
 private static float initialDamage;
 private static double minCoveredZ=99;
 private static boolean complete;
 private static final BlockPos[] OPENING={new BlockPos(11,81,12),new BlockPos(12,81,12),new BlockPos(11,80,12),new BlockPos(12,80,12)};
 private static final Path LOG=Path.of("developer-demos-take5.log");
 public static void tick(MinecraftClient mc){
  if(!ENABLED||complete||mc.player==null||mc.getServer()==null)return;
  if(scene<0){
   mc.options.getGuiScale().setValue(3);mc.onResolutionChanged();
   mc.options.getFov().setValue(72);mc.options.getBobView().setValue(false);
   mc.options.getChatVisibility().setValue(ChatVisibility.FULL);
   mc.getTutorialManager().setStep(TutorialStep.NONE);
   next(mc);return;
  }
  tick++;
  mc.getToastManager().clear();mc.inGameHud.getChatHud().clear(false);
  if(tick==-32)stage(mc);
  if(tick<0)return;
  double[] s=DoomCraft.state;
  if(!DoomCraft.active||s.length<18){log("FAIL inactive scene="+scene+" tick="+tick);complete=true;mc.scheduleStop();return;}
  if(tick==0){initialDamage=DoomCraft.receivedDamage;log("BEGIN "+NAMES[scene]);}
  if(scene==0)cover(mc,s);
  if(scene==1)pistol(mc,s);
  if(scene==2)melee(mc,s);
  record(mc,s);
  if(endAt>=0&&tick>=endAt||tick>=280){
   mc.options.useKey.setPressed(false);mc.interactionManager.cancelBlockBreaking();
   log("END "+NAMES[scene]+" frames="+frame+" demon="+s[9]+" ammo="+s[1]+" destroyed="+DoomCraft.brokenBlocks+" damage="+DoomCraft.receivedDamage+" melee="+DoomCraft.meleeEvents+" minCoveredZ="+minCoveredZ+" opening="+opening(mc));
   if(scene==2||Boolean.getBoolean("doomcraft.coverOnly")){complete=true;mc.scheduleStop();}else next(mc);
  }
 }
 private static void next(MinecraftClient mc){
  scene++;tick=-40;frame=0;endAt=-1;hitAt=-1;firstDamage=-1;mineIndex=0;minCoveredZ=99;
  mc.options.useKey.setPressed(false);mc.options.attackKey.setPressed(false);mc.getServer().execute(()->{var p=mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());mc.getServer().getCommandManager().executeWithPrefix(p.getCommandSource(),"doomcraft");});
 }
 private static void stage(MinecraftClient mc){
  int which=scene;
  mc.getServer().execute(()->{
   var p=mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());var w=p.getServerWorld();
   w.getGameRules().get(GameRules.NATURAL_REGENERATION).set(false,mc.getServer());
   w.getGameRules().get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false,mc.getServer());
   for(int x=0;x<24;x++)for(int z=0;z<24;z++){
    for(int y=80;y<86;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState(),2);
    w.setBlockState(new BlockPos(x,79,z),(x%4==0||z%4==0?Blocks.SMOOTH_STONE:Blocks.POLISHED_ANDESITE).getDefaultState(),2);
   }
   // Two-block corridor makes a blocked native actor clearly observable through glass.
   if(which<2){
    for(int z=4;z<=21;z++)for(int y=80;y<83;y++)for(int x:new int[]{10,13})w.setBlockState(new BlockPos(x,y,z),Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(),2);
    for(int x=10;x<=13;x++)for(int y=80;y<83;y++)for(int z:new int[]{4,19})w.setBlockState(new BlockPos(x,y,z),Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(),2);
    for(int x=11;x<=12;x++)for(int y=80;y<(which==0?83:82);y++)w.setBlockState(new BlockPos(x,y,12),(which==0?Blocks.GLASS:Blocks.RED_TERRACOTTA).getDefaultState(),2);
   }
   p.teleport(w,12,80,which==0?10.4:which==1?6:12,0,0);p.setHealth(20);
  });
  mc.player.getInventory().selectedSlot=which==0?2:which==1?0:1;
 }
 private static int opening(MinecraftClient mc){int n=0;for(var b:OPENING)if(mc.world.isAir(b))n++;return n;}
 private static void cover(MinecraftClient mc,double[] s){
  if(tick<50){mc.player.setYaw(0);mc.player.setPitch(0);minCoveredZ=Math.min(minCoveredZ,s[11]);}
  if(tick>=50&&mineIndex<4){
   BlockPos b=OPENING[mineIndex];aim(mc,b.getX()+.5,b.getY()+.5,b.getZ()+.5);
   if(mc.world.isAir(b)){mc.options.attackKey.setPressed(false);mineIndex++;}
   else mc.options.attackKey.setPressed(true);
  }
  if(mineIndex==4){mc.options.attackKey.setPressed(false);mc.interactionManager.cancelBlockBreaking();aim(mc,s[10],80+s[12]+1.3,s[11]);}
  if(mineIndex==4&&opening(mc)==4&&s[11]<12&&DoomCraft.receivedDamage>initialDamage&&endAt<0)endAt=tick+32;
 }
 private static void pistol(MinecraftClient mc,double[] s){
  aim(mc,s[10],80+s[12]+1.0,s[11]);
  if(tick==20)mc.options.useKey.setPressed(true);
  if(s[9]<=0&&endAt<0){mc.options.useKey.setPressed(false);endAt=tick+35;}
  if(s[9]<=0)aim(mc,s[10],80.4,s[11]);
 }
 private static void melee(MinecraftClient mc,double[] s){
  aim(mc,s[10],80+s[12]+(s[9]<=0?.35:1.25),s[11]);
  if(DoomCraft.receivedDamage>initialDamage&&firstDamage<0){firstDamage=tick;hitAt=tick+20;}
  if(hitAt>=0&&(tick==hitAt||tick==hitAt+25||tick==hitAt+50)){
   for(var e:mc.world.getEntities())if(e instanceof DoomCraft.Demon&&e.isAlive()&&mc.player.distanceTo(e)<3.1){
    mc.interactionManager.attackEntity(mc.player,e);mc.player.swingHand(Hand.MAIN_HAND);break;
   }
  }
  if(s[9]<=0&&endAt<0)endAt=tick+35;
 }
 private static void aim(MinecraftClient mc,double x,double y,double z){
  double dx=x-mc.player.getX(),dz=z-mc.player.getZ();
  mc.player.setYaw((float)Math.toDegrees(Math.atan2(-dx,dz)));
  mc.player.setPitch((float)Math.toDegrees(Math.atan2(mc.player.getEyeY()-y,Math.hypot(dx,dz))));
 }
 private static void record(MinecraftClient mc,double[] s){
  String filename=String.format(Locale.ROOT,"dev5-%s-%04d.png",NAMES[scene],frame++);
  ScreenshotRecorder.saveScreenshot(mc.runDirectory,filename,mc.getFramebuffer(),text->{});
  log(String.format(Locale.ROOT,"FRAME %s %d tick=%d doomTick=%.0f playerHP=%.2f impHP=%.0f ammo=%.0f impX=%.3f impZ=%.3f removed=%d incoming=%.2f melee=%d opening=%d",NAMES[scene],frame-1,tick,s[8],mc.player.getHealth(),s[9],s[1],s[10],s[11],DoomCraft.brokenBlocks,DoomCraft.receivedDamage,DoomCraft.meleeEvents,opening(mc)));
 }
 private static void log(String text){
  try{Files.writeString(LOG,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}
  if(!text.startsWith("FRAME"))System.out.println("DEVELOPER_DEMOS "+text);
 }
 public static void hud(DrawContext draw,MinecraftClient mc,double[] s){
  if(scene<0)return;
  String title=scene==0?"01 / MINECRAFT COVER, NATIVE DOOM COLLISION":scene==1?"02 / DOOM GUNFIRE CHANGES MINECRAFT":"03 / DAMAGE CROSSES BOTH WAYS";
  String phase=scene==0?(tick<50?"Solid glass blocks movement and line of sight":mineIndex<4?"Mining a real 2 x 2 opening with a Minecraft pickaxe":opening(mc)==4?"Cover removed: the demon can reach the player":"Opening the barrier"):
    scene==1?(s[9]<=0?"Native imp death state reached":DoomCraft.brokenBlocks>0?"Real blocks removed; native Doom damage lands":"Original Doom pistol timing, spread and ammunition"):
    (s[9]<=0?"Minecraft sword hits drove native Doom health to zero":firstDamage>=0?"Doom hit registered in Minecraft; now the sword answers":"Waiting for a native Doom attack: watch player health");
  int width=mc.getWindow().getScaledWidth();
  draw.fill(6,6,width-6,57,0xc010141a);
  draw.drawTextWithShadow(mc.textRenderer,title,12,11,0xffffd16a);
  draw.drawTextWithShadow(mc.textRenderer,phase,12,25,0xfff2f4f6);
  String metrics=String.format(Locale.ROOT,"PLAYER %.1f / 20    DOOM IMP %d / 60    AMMO %d    BLOCKS HIT %d",mc.player.getHealth(),Math.max(0,(int)s[9]),(int)s[1],DoomCraft.brokenBlocks);
  draw.drawTextWithShadow(mc.textRenderer,metrics,12,41,0xff99dcff);
  draw.drawTextWithShadow(mc.textRenderer,"LIVE SCRIPTED CAPTURE / embedded Doom C + Fabric / Freedoom art",10,mc.getWindow().getScaledHeight()-53,0xffc2c8d0);
 }
}
