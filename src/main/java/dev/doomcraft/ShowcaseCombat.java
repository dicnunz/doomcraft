package dev.doomcraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.util.math.*;
import java.util.*;
/** Opt-in demo staging only. The survival weapons, mob AI and player damage are unmodified. */
final class ShowcaseCombat {
 private static int scene=-1,tick;private static volatile boolean ready;
 private static final Set<UUID> mobs=java.util.concurrent.ConcurrentHashMap.newKeySet();
 static boolean tick(MinecraftClient mc){
  if(scene<0){scene=0;stage(mc);return false;}
  if(!ready)return false;
  tick++;
  if(tick==1)System.out.println("SHOWCASE_COMBAT scene="+scene+" time="+System.currentTimeMillis());
  if(tick<45)return false;
  if(tick==45){org.lwjgl.glfw.GLFW.glfwFocusWindow(mc.getWindow().getHandle());mc.mouse.lockCursor();}
  mc.player.getInventory().selectedSlot=scene==0?0:1;
  HostileEntity target=null;double distance=1e9;
  for(var e:mc.world.getEntities())if(e instanceof HostileEntity h&&h.isAlive()&&mobs.contains(h.getUuid())&&mc.player.canSee(h)&&h.squaredDistanceTo(mc.player)<distance){target=h;distance=h.squaredDistanceTo(mc.player);}
  if(target!=null){Vec3d d=target.getPos().add(0,target.getHeight()*.65,0).subtract(mc.player.getEyePos());float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));mc.player.setYaw(mc.player.getYaw()+MathHelper.wrapDegrees(yaw-mc.player.getYaw())*.32f);mc.player.setPitch(mc.player.getPitch()+(pitch-mc.player.getPitch())*.32f);}
  mc.options.useKey.setPressed(tick>60&&target!=null);
  mc.options.backKey.setPressed(scene>0&&target!=null&&distance<45);
  mc.options.leftKey.setPressed(scene==1&&tick>85&&tick<105);
  if(tick>320||(tick>130&&target==null)){
   mc.options.useKey.setPressed(false);mc.options.backKey.setPressed(false);mc.options.leftKey.setPressed(false);
   System.out.println("SHOWCASE_COMBAT_DONE scene="+scene+" kills="+NightCombat.kills+" damage="+NightCombat.damageDealt+" time="+System.currentTimeMillis());
   if(++scene>=3)return true;stage(mc);
  }
  return false;
 }
 private static void stage(MinecraftClient mc){
  ready=false;tick=0;mobs.clear();int take=scene;
  mc.getServer().execute(()->{var p=NightCombat.player;var w=p.getServerWorld();
   for(var e:w.getEntitiesByClass(HostileEntity.class,p.getBoundingBox().expand(60),e->true))e.discard();
   var q=NightCombat.surface(w,-703,261);p.teleport(w,q.getX()+.5,q.getY(),q.getZ()+.5,0,0);w.setTimeOfDay(13000);p.setHealth(20);
   if(take==0){spawn(EntityType.ZOMBIE,-1,7);spawn(EntityType.ZOMBIE,3,10);}
   if(take==1){spawn(EntityType.CREEPER,0,9);spawn(EntityType.SKELETON,4,10);}
   if(take==2){spawn(EntityType.ZOMBIE,-1,5);spawn(EntityType.ZOMBIE,2,8);spawn(EntityType.SKELETON,-4,16);}
   ready=true;
  });
 }
 private static void spawn(EntityType<? extends HostileEntity> type,int dx,int dz){var p=NightCombat.player;var w=p.getServerWorld();var e=type.spawn(w,NightCombat.surface(w,-703+dx,261+dz),SpawnReason.COMMAND);if(e!=null){if(e instanceof ZombieEntity z)z.setBaby(false);e.setPersistent();e.setTarget(p);mobs.add(e.getUuid());}}
}
