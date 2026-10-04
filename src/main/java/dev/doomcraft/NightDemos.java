package dev.doomcraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.*;
/** Opt-in scripted normal client inputs, with vanilla mobs staged between takes. */
public final class NightDemos {
 private static int ticks,scene=-1,frame,settle,postTicks;private static volatile boolean staged;private static BlockPos origin;private static java.util.UUID locked;private static boolean pendingStage;
 private static final java.util.Set<java.util.UUID> stagedMobs=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private static final boolean ENABLED=Boolean.getBoolean("doomcraft.nightDemos");
 public static void tick(MinecraftClient mc){
  if(!NightCombat.active||mc.player==null)return;ticks++;
  if(ticks==1){mc.options.getGamma().setValue(1.0);mc.options.getGuiScale().setValue(3);mc.options.getFov().setValue(78);mc.onResolutionChanged();}
  if(ticks==100)capture(mc,"night-first.png");
  if(Boolean.getBoolean("doomcraft.nightVerify")){NightChecks.tick(mc,ticks);return;}
  if(!ENABLED)return;
  if(mc.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen)mc.setScreen(null);
  if(scene>=3){NightChecks.tick(mc,++postTicks);return;}
  mc.inGameHud.getChatHud().clear(false);mc.getToastManager().clear();
  if(ticks<80)return;
  if(scene<0){scene=0;stage(mc);return;}
  if(pendingStage){if(mc.player.isAlive()){pendingStage=false;stage(mc);}return;}
  if(!staged)return;
  mc.player.getInventory().selectedSlot=scene==0?0:1;
  if(settle++<(scene==0?65:25))return;
  if(frame==0)System.out.println("NIGHT_SCENE_START scene="+scene+" position="+mc.player.getPos());
  mc.player.getInventory().selectedSlot=scene==0?0:1;
  HostileEntity target=null;double nearest=1e9;
  for(var e:mc.world.getEntities())if(e instanceof HostileEntity h&&e.isAlive()&&stagedMobs.contains(e.getUuid())&&mc.player.canSee(e)&&e.squaredDistanceTo(mc.player)<nearest){nearest=e.squaredDistanceTo(mc.player);target=h;}
  if(locked!=null){for(var e:mc.world.getEntities())if(e instanceof HostileEntity h&&e.isAlive()&&e.getUuid().equals(locked)&&mc.player.canSee(e)){target=h;break;}}
  locked=target==null?null:target.getUuid();
  if(target!=null){double dx=target.getX()-mc.player.getX(),dz=target.getZ()-mc.player.getZ();
   float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));float pitch=(float)Math.toDegrees(Math.atan2(mc.player.getEyeY()-target.getY()-target.getHeight()*.65,Math.hypot(dx,dz)));
   mc.player.setYaw(mc.player.getYaw()+MathHelper.wrapDegrees(yaw-mc.player.getYaw())*.3f);mc.player.setPitch(mc.player.getPitch()+(pitch-mc.player.getPitch())*.3f);
  }
  int fireStart=scene==2?15:5;
  mc.options.useKey.setPressed(frame>fireStart&&target!=null);
  mc.options.backKey.setPressed(scene==2&&frame>20&&frame<135);
  mc.options.leftKey.setPressed(scene==1&&frame>65&&frame<95);
  mc.options.forwardKey.setPressed(scene==0&&frame>145&&frame<180);
  if(frame%1==0)capture(mc,String.format("night-%d-%04d.png",scene,frame));
  if(frame%10==0){String line=String.format(java.util.Locale.ROOT,"{\"scene\":%d,\"frame\":%d,\"hp\":%.2f,\"ammo\":%.0f,\"weapon\":%.0f,\"hits\":%d,\"kills\":%d,\"damage\":%.2f,\"hurt\":%.2f,\"blocked\":%d,\"x\":%.3f,\"y\":%.3f,\"z\":%.3f}%n",scene,frame,mc.player.getHealth(),NightCombat.state[1],NightCombat.state[9],NightCombat.pelletsHit,NightCombat.kills,NightCombat.damageDealt,NightCombat.damageReceived,NightCombat.blockedShots,mc.player.getX(),mc.player.getY(),mc.player.getZ());
   try{Files.writeString(Path.of(mc.runDirectory.getAbsolutePath(),"night-telemetry.jsonl"),line,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}
  }
  if(++frame>=220){mc.options.useKey.setPressed(false);mc.options.backKey.setPressed(false);mc.options.forwardKey.setPressed(false);mc.options.leftKey.setPressed(false);
   System.out.println("NIGHT_SCENE_DONE scene="+scene+" shots="+NightCombat.shotsFired+" hits="+NightCombat.pelletsHit+" kills="+NightCombat.kills+" damage="+NightCombat.damageDealt+" hurt="+NightCombat.damageReceived+" blocks="+NightCombat.blockedShots);
   scene++;if(scene<3)stage(mc);
  }
 }
 private static void stage(MinecraftClient mc){
  if(!mc.player.isAlive()){pendingStage=true;mc.player.requestRespawn();mc.setScreen(null);return;}
  staged=false;frame=settle=0;locked=null;stagedMobs.clear();int take=scene;
  mc.getServer().execute(()->{
   var p=NightCombat.player;var w=NightCombat.world;if(origin==null)origin=p.getBlockPos();
   for(var e:w.getEntitiesByClass(HostileEntity.class,p.getBoundingBox().expand(64),e->true))e.discard();
   w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,p.getServer());
   p.teleport(w,origin.getX()+.5,origin.getY(),origin.getZ()+.5,0,0);
   p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource(),"doomnight");
   if(take==0){spawn(EntityType.ZOMBIE,-1,20);spawn(EntityType.ZOMBIE,3,28);}
   if(take==1){spawn(EntityType.CREEPER,0,15);spawn(EntityType.SKELETON,5,20);spawn(EntityType.ZOMBIE,-4,20);}
   if(take==2){spawn(EntityType.ZOMBIE,0,4);spawn(EntityType.ZOMBIE,2,7);spawn(EntityType.SKELETON,-4,16);}
   staged=true;
  });
 }
 private static void spawn(EntityType<? extends HostileEntity> type,int dx,int dz){
  var w=NightCombat.world;var pos=NightCombat.surface(w,origin.getX()+dx,origin.getZ()+dz);var e=type.spawn(w,pos,SpawnReason.COMMAND);if(e!=null){if(e instanceof net.minecraft.entity.mob.ZombieEntity z){z.setBaby(false);var vehicle=z.getVehicle();if(vehicle!=null){z.stopRiding();vehicle.discard();}}stagedMobs.add(e.getUuid());e.setPersistent();e.setTarget(NightCombat.player);}
 }
 private static void capture(MinecraftClient mc,String name){ScreenshotRecorder.saveScreenshot(mc.runDirectory,name,mc.getFramebuffer(),t->{});}
}
