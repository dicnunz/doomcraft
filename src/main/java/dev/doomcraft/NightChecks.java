package dev.doomcraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.*;
import java.util.*;
/** Test fixtures only, run in a fresh isolated world; restored in finally. */
final class NightChecks {
 static volatile boolean finished,passed;
 static void tick(MinecraftClient mc,int ticks){
  if(ticks==100)mc.getServer().execute(NightChecks::verify);
  if(ticks==110){mc.options.getGuiScale().setValue(2);mc.onResolutionChanged();}
  if(ticks==125)capture(mc,"night-hud-gui2.png");
  if(ticks==130){mc.options.getGuiScale().setValue(3);mc.onResolutionChanged();}
  if(ticks==145)capture(mc,"night-hud-gui3.png");
  if(ticks>=155&&finished){System.out.println("NIGHT_CHECKS_COMPLETE passed="+passed);mc.scheduleStop();}
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("NIGHT_CHECK_PASS "+message);}
 static void verify(){
  var w=NightCombat.world;var p=NightCombat.player;BlockPos base=p.getBlockPos().add(35,12,0);
  Map<BlockPos,BlockState> originals=new HashMap<>();ZombieEntity mob=new ZombieEntity(EntityType.ZOMBIE,w);
  try{
   mob.setAiDisabled(true);mob.setNoGravity(true);mob.setPosition(base.getX()+.5,base.getY(),base.getZ()+6.5);w.spawnEntity(mob);
   for(int z=0;z<9;z++)for(int y=0;y<3;y++){BlockPos b=base.add(0,y,z);originals.put(b,w.getBlockState(b));w.setBlockState(b,Blocks.AIR.getDefaultState(),3);}
   Vec3d eye=new Vec3d(base.getX()+.5,base.getY()+1.1,base.getZ()+.5),end=eye.add(0,0,10);
   check(NightCombat.trace(eye,end).entity()==mob,"open line hits actual vanilla entity bounds");
   w.setBlockState(base.add(0,1,3),Blocks.STONE.getDefaultState(),3);
   check(NightCombat.trace(eye,end).entity()==null,"solid Minecraft terrain stops native shot ray before mob");
   w.setBlockState(base.add(0,1,3),Blocks.AIR.getDefaultState(),3);
   check(NightCombat.trace(eye,end).entity()==mob,"removing terrain immediately reopens shot path");
   w.setBlockState(base.add(0,0,3),Blocks.STONE_SLAB.getDefaultState(),3);
   Vec3d high=new Vec3d(base.getX()+.5,base.getY()+.75,base.getZ()+.5),low=high.add(0,-.5,0);
   check(NightCombat.trace(high,high.add(0,0,10)).entity()==mob,"ray above half slab passes through empty portion of voxel");
   check(NightCombat.trace(low,low.add(0,0,10)).entity()==null,"ray through lower slab is blocked by real collision shape");
   w.setBlockState(base.add(0,0,3),Blocks.AIR.getDefaultState(),3);
   check(NightCombat.trace(low,low.add(0,0,10)).entity()==mob,"edited slab collision updates without native arena snapshot");
   passed=true;
  }catch(Throwable e){e.printStackTrace();}finally{mob.discard();originals.forEach((b,s)->w.setBlockState(b,s,3));finished=true;}
 }
 static void capture(MinecraftClient mc,String n){ScreenshotRecorder.saveScreenshot(mc.runDirectory,n,mc.getFramebuffer(),t->{});}
}
