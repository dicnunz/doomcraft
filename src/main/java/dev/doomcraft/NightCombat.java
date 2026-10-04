package dev.doomcraft;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.Path;
import java.util.*;

/** Minecraft owns all terrain, movement, mobs and health. Upstream Doom owns weapons. */
public final class NightCombat {
 public static final boolean ENABLED=Boolean.getBoolean("doomcraft.night");
 public static final Item SHOTGUN=Registry.register(Registries.ITEM,Identifier.of("doomcraft","shotgun"),new Item(new Item.Settings().maxCount(1)){
  @Override public TypedActionResult<ItemStack> use(World w,PlayerEntity p,Hand h){if(SurvivalMode.ENABLED&&h!=Hand.MAIN_HAND)return TypedActionResult.pass(p.getStackInHand(h));p.setCurrentHand(h);return TypedActionResult.consume(p.getStackInHand(h));}
  @Override public int getMaxUseTime(ItemStack s,LivingEntity u){return 72000;}
 });
 private static final SoundEvent PISTOL_SOUND=sound("pistol"),SHOTGUN_SOUND=sound("shotgun");
 private static SoundEvent sound(String n){return Registry.register(Registries.SOUND_EVENT,Identifier.of("doomcraft",n),SoundEvent.of(Identifier.of("doomcraft",n)));}
 public static volatile boolean active;
 public static volatile double[] state=new double[0];
 public static volatile int shotsFired,pelletsHit,blockedShots,kills,hitTick;
 public static volatile float damageDealt,damageReceived;
 static ServerPlayerEntity player;static ServerWorld world;
 private static boolean placed;
 private static int accumulator;private static float previousHealth;
 public static boolean isGun(ItemStack s){return s.isOf(DoomCraft.PISTOL)||s.isOf(SHOTGUN);}
 public static void install(){
  SurvivalMode.install();
  CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(CommandManager.literal("doomnight").requires(s->s.hasPermissionLevel(2)).executes(c->{
   var p=c.getSource().getPlayerOrThrow();
   Path allowed=Path.of(System.getProperty("doomcraft.runRoot","DISABLED")).toAbsolutePath().normalize().resolve("saves");
   Path save=p.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
   if(!ENABLED||!p.getServer().isSingleplayer()||!save.startsWith(allowed)){c.getSource().sendError(Text.literal("Start the isolated night-world launcher."));return 0;}
   start(p);return 1;
  })));
  ServerTickEvents.END_SERVER_TICK.register(s->{if(active){tick();if(SurvivalMode.ENABLED)SurvivalMode.store();}});
  net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->{if(active&&old==player){player=p;world=p.getServerWorld();previousHealth=p.getHealth();}});
  ServerLifecycleEvents.SERVER_STOPPING.register(s->{active=false;state=new double[0];player=null;world=null;placed=false;});
 }
 static void attachSurvival(ServerPlayerEntity p,int bullets,int shells){
  player=p;world=p.getServerWorld();DoomCraft.active=false;
  NativeBridge.initWeapons();NativeBridge.setAmmo(bullets,shells);state=NativeBridge.weaponStatus();
  accumulator=0;previousHealth=p.getHealth();active=true;
 }
 private static void start(ServerPlayerEntity p){
  DoomCraft.active=false;player=p;world=p.getServerWorld();
  world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,p.getServer());
  world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true,p.getServer());
  world.setTimeOfDay(18000);world.setWeather(120000,0,false,false);
  if(!placed){placed=true;BlockPos spawn=world.getSeed()==1234?surface(world,-703,261):clearing(world,p.getBlockPos());if(spawn.getY()<=world.getBottomY()||world.getBlockState(spawn.down()).isAir())throw new IllegalStateException("No safe generated surface found");p.teleport(world,spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0);world.setSpawnPos(spawn,0);System.out.println("NIGHT_SPAWN "+spawn);}
  p.changeGameMode(GameMode.SURVIVAL);p.setHealth(20);p.getHungerManager().setFoodLevel(20);
  p.getInventory().setStack(0,new ItemStack(DoomCraft.PISTOL));p.getInventory().setStack(1,new ItemStack(SHOTGUN));
  p.getInventory().setStack(2,new ItemStack(Items.DIAMOND_PICKAXE));p.getInventory().setStack(3,new ItemStack(Items.TORCH,64));p.getInventory().setStack(4,new ItemStack(Items.BREAD,32));
  NativeBridge.initWeapons();state=NativeBridge.weaponStatus();accumulator=shotsFired=pelletsHit=blockedShots=kills=0;damageDealt=damageReceived=0;previousHealth=20;active=true;
  p.sendMessage(Text.literal("DOOMCRAFT  •  1 Pistol / 2 Shotgun  •  Hold right mouse to fire  •  /doomnight restocks"),false);
 }
 static BlockPos surface(ServerWorld w,int x,int z){w.getChunk(x>>4,z>>4);return w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));}
 private static BlockPos clearing(ServerWorld w,BlockPos origin){
  var source=w.getChunkManager().getChunkGenerator().getBiomeSource();
  var sampler=w.getChunkManager().getNoiseConfig().getMultiNoiseSampler();
  BlockPos plains=null;double bestBiomeScore=-1e9;
  for(int dx=-1600;dx<=1600;dx+=64)for(int dz=-1600;dz<=1600;dz+=64){
   int x=origin.getX()+dx,z=origin.getZ()+dz;
   if(!source.getBiome(x>>2,16,z>>2,sampler).matchesKey(net.minecraft.world.biome.BiomeKeys.PLAINS))continue;
   double score=0;
   for(int ox=-64;ox<=64;ox+=32)for(int oz=-64;oz<=64;oz+=32)
    if(source.getBiome((x+ox)>>2,16,(z+oz)>>2,sampler).matchesKey(net.minecraft.world.biome.BiomeKeys.PLAINS))score+=10;
   score-=Math.hypot(dx,dz)*.02;
   if(score>bestBiomeScore){bestBiomeScore=score;plains=new BlockPos(x,64,z);}
  }
  if(plains!=null)origin=plains;System.out.println("NIGHT_PLAINS_SEARCH "+plains);
  BlockPos best=surface(w,origin.getX(),origin.getZ());double bestScore=-1e9;
  for(int x=origin.getX()-64;x<=origin.getX()+64;x+=16)for(int z=origin.getZ()-64;z<=origin.getZ()+64;z+=16){
   BlockPos c=surface(w,x,z);if(!w.getBiome(c).matchesKey(net.minecraft.world.biome.BiomeKeys.PLAINS))continue;double score=-c.getSquaredDistance(origin)*.0005;
   if(w.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)!=c.getY()||!w.getBlockState(c.down()).isOf(net.minecraft.block.Blocks.GRASS_BLOCK))continue;
   for(int dx=-12;dx<=12;dx+=6)for(int dz=-12;dz<=12;dz+=6){BlockPos q=surface(w,x+dx,z+dz);
    score+=w.getBlockState(q.down()).isOf(net.minecraft.block.Blocks.GRASS_BLOCK)&&w.getTopY(Heightmap.Type.WORLD_SURFACE,q.getX(),q.getZ())==q.getY()?5:-8;
    score-=Math.abs(q.getY()-c.getY())*2;
   }
   if(score>bestScore){bestScore=score;best=c;}
  }
  System.out.println("NIGHT_BIOME "+w.getBiome(best).getKey()+" score="+bestScore);return best;
 }
 record Trace(net.minecraft.util.hit.BlockHitResult block,LivingEntity entity){}
 static Trace trace(Vec3d start,Vec3d end){
  var block=world.raycast(new RaycastContext(start,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,player));
  double nearest=start.squaredDistanceTo(block.getPos());LivingEntity target=null;
  for(var entity:world.getEntitiesByClass(LivingEntity.class,new Box(start,end).expand(1),e->e!=player&&e.isAlive()&&!e.isSpectator())){
   var point=entity.getBoundingBox().raycast(start,end);
   if(point.isPresent()){double distance=start.squaredDistanceTo(point.get());if(distance<nearest){nearest=distance;target=entity;}}
  }
  return new Trace(block,target);
 }
 private static void tick(){
  if(player==null||player.isRemoved()||!player.isAlive())return;
  if(player.getServerWorld()!=world){world=player.getServerWorld();}
  float hp=player.getHealth();if(hp<previousHealth)damageReceived+=previousHealth-hp;
  var held=player.getMainHandStack();boolean gun=isGun(held);
  if(gun)NativeBridge.selectWeapon(held.isOf(SHOTGUN)?2:1);
  NativeBridge.pose(12,5,0,player.getYaw(),player.getPitch(),Math.max(1,Math.round(hp*5)),gun&&player.isUsingItem()&&player.getActiveHand()==Hand.MAIN_HAND&&isGun(player.getActiveItem()));
  accumulator+=35;while(accumulator>=20){NativeBridge.tick();accumulator-=20;}
  double[] events=NativeBridge.shots();state=NativeBridge.weaponStatus();
  Map<LivingEntity,Float> hits=new HashMap<>();
  if(events.length>0){shotsFired++;world.playSound(null,player.getX(),player.getY(),player.getZ(),state[9]==2?SHOTGUN_SOUND:PISTOL_SOUND,SoundCategory.PLAYERS,1f,1f);}
  for(int i=0;i+4<events.length;i+=5){
   Vec3d start=player.getEyePos(),direction=new Vec3d(events[i],events[i+2],events[i+1]).normalize();
   Vec3d end=start.add(direction.multiply(events[i+4]));
   var trace=trace(start,end);var block=trace.block();LivingEntity target=trace.entity();
   if(target!=null){hits.merge(target,(float)events[i+3]/5f,Float::sum);pelletsHit++;}
   else if(block.getType()==HitResult.Type.BLOCK){blockedShots++;world.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE,block.getPos().x,block.getPos().y,block.getPos().z,2,.035,.035,.035,0);}
  }
  // A shotgun burst is one Minecraft damage transaction, so invulnerability frames don't discard pellets.
  hits.forEach((e,d)->{float before=e.getHealth();if(e.damage(world.getDamageSources().playerAttack(player),d)){damageDealt+=before-e.getHealth();hitTick=(int)state[8];if(!e.isAlive())kills++;}});
  previousHealth=player.getHealth();
 }
}
