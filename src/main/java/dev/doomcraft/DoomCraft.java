package dev.doomcraft;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import java.nio.file.Path;

public final class DoomCraft implements ModInitializer {
 public static final Item PISTOL=Registry.register(Registries.ITEM,Identifier.of("doomcraft","pistol"),new Item(new Item.Settings().maxCount(1)){
  @Override public TypedActionResult<ItemStack> use(World w,PlayerEntity p,Hand h){if(SurvivalMode.ENABLED&&h!=Hand.MAIN_HAND)return TypedActionResult.pass(p.getStackInHand(h));p.setCurrentHand(h);return TypedActionResult.consume(p.getStackInHand(h));}
  @Override public int getMaxUseTime(ItemStack stack,net.minecraft.entity.LivingEntity user){return 72000;}
 });
 public static final EntityType<Demon> DEMON=Registry.register(Registries.ENTITY_TYPE,Identifier.of("doomcraft","demon"),EntityType.Builder.create(Demon::new,SpawnGroup.MISC).dimensions(1.25f,1.75f).maxTrackingRange(64).build("doomcraft:demon"));
 public static volatile double[] state=new double[0];
 public static volatile boolean active;
 private static ServerPlayerEntity player;
 private static ServerWorld world;
 private static Demon proxy;
 private static int accumulator;
 public static volatile int brokenBlocks,meleeEvents;
 public static volatile float receivedDamage;
 public static final int FLOOR=80;
 public static final class Demon extends PathAwareEntity {
  public Demon(EntityType<? extends PathAwareEntity> type,World world){super(type,world);setAiDisabled(true);setNoGravity(true);}
  @Override protected void initGoals(){}
  @Override protected void applyDamage(DamageSource source,float amount){
   if(!getWorld().isClient&&active&&this==proxy&&amount>0){NativeBridge.hit(Math.max(1,Math.round(amount*5)));meleeEvents++;System.out.println("DOOMCRAFT_MC_HIT amount="+amount+" demonHealth="+NativeBridge.snapshot()[9]);}
  }
  @Override public void takeKnockback(double strength,double x,double z){}
  static DefaultAttributeContainer.Builder attributes(){return createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,1024).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0);}
 }
 @Override public void onInitialize(){
  NightCombat.install();
  FabricDefaultAttributeRegistry.register(DEMON,Demon.attributes());
  CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(CommandManager.literal("doomcraft").requires(s->s.hasPermissionLevel(2)).executes(c->{
   ServerPlayerEntity p=c.getSource().getPlayerOrThrow();
   Path allowed=Path.of(System.getProperty("doomcraft.runRoot","DOOMCRAFT_DISABLED")).toAbsolutePath().normalize().resolve("saves");
   Path save=p.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
   if(!p.getServer().isSingleplayer()||!save.startsWith(allowed)){c.getSource().sendError(Text.literal("Use the isolated DoomCraft development instance and a new lab world."));return 0;}
   if(NightCombat.ENABLED||SurvivalMode.ENABLED){c.getSource().sendError(Text.literal("Use Arena.command for the preserved arena."));return 0;}
   start(p);return Command.SINGLE_SUCCESS;
  })));
  ServerTickEvents.END_SERVER_TICK.register(s->{if(active)step();});
  ServerLifecycleEvents.SERVER_STOPPING.register(s->{active=false;state=new double[0];player=null;world=null;proxy=null;});
 }
 private static void start(ServerPlayerEntity p){
  active=false;if(proxy!=null)proxy.discard();
  player=p;world=p.getServerWorld();
  for(int x=-1;x<=24;x++)for(int z=-1;z<=24;z++)for(int y=FLOOR-1;y<=FLOOR+6;y++){
   boolean rim=x==-1||z==-1||x==24||z==24;
   world.setBlockState(new BlockPos(x,y,z),y==FLOOR-1||rim?Blocks.BEDROCK.getDefaultState():Blocks.AIR.getDefaultState(),3);
  }
  // A removable clay shield. Destroy it with Doom bullets or a Minecraft pickaxe.
  for(int x=10;x<=13;x++)for(int y=0;y<3;y++)world.setBlockState(new BlockPos(x,FLOOR+y,11),Blocks.RED_TERRACOTTA.getDefaultState(),3);
  world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,p.getServer());
  world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,p.getServer());world.setTimeOfDay(6000);
  p.changeGameMode(GameMode.SURVIVAL);p.teleport(world,12.5,FLOOR,5.5,0,0);p.setHealth(20);p.getHungerManager().setFoodLevel(20);
  p.getInventory().clear();p.getInventory().setStack(0,new ItemStack(PISTOL));p.getInventory().setStack(1,new ItemStack(Items.IRON_SWORD));p.getInventory().setStack(2,new ItemStack(Items.DIAMOND_PICKAXE));p.getInventory().setStack(3,new ItemStack(Items.COBBLESTONE,64));
  NativeBridge.init();accumulator=0;brokenBlocks=0;meleeEvents=0;receivedDamage=0;state=NativeBridge.snapshot();
  proxy=new Demon(DEMON,world);proxy.setPersistent();proxy.setInvisible(true);proxy.setPosition(12,FLOOR,18);world.spawnEntity(proxy);
  active=true;p.sendMessage(Text.literal("Doom C bridge / Freedoom art • Hold right-click: pistol • Sword: Minecraft combat • Mine/build cover • /doomcraft resets arena"),false);
 }
 private static void step(){
  if(player==null||player.isRemoved()||!player.isAlive()||player.getServerWorld()!=world){active=false;state=new double[0];if(proxy!=null)proxy.discard();return;}
  // Bound the arena so the native world can never read unrepresented terrain.
  if(player.getX()<.4||player.getX()>23.6||player.getZ()<.4||player.getZ()>23.6||player.getY()<FLOOR||player.getY()>FLOOR+4)player.teleport(world,12.5,FLOOR,5.5,player.getYaw(),player.getPitch());
  byte[] cells=new byte[24*24*6];
  for(int y=0;y<6;y++)for(int z=0;z<24;z++)for(int x=0;x<24;x++){
   BlockPos b=new BlockPos(x,FLOOR+y,z);cells[(y*24+z)*24+x]=(byte)(world.getBlockState(b).getCollisionShape(world,b).isEmpty()?0:1);
  }
  NativeBridge.grid(cells);
  int health=Math.max(1,Math.round(player.getHealth()*5));
  NativeBridge.pose(player.getX(),player.getZ(),player.getY()-FLOOR,player.getYaw(),player.getPitch(),health,player.isUsingItem()&&player.getActiveItem().isOf(PISTOL));
  accumulator+=35;
  while(accumulator>=20){NativeBridge.tick();accumulator-=20;}
  double[] next=NativeBridge.snapshot();
  int loss=health-(int)next[0];if(loss>0){float before=player.getHealth();player.damage(world.getDamageSources().mobAttack(proxy),loss/5f);receivedDamage+=before-player.getHealth();}
  for(int index:NativeBridge.blocks()){
   BlockPos b=new BlockPos(index%24,FLOOR+index/(24*24),(index/24)%24);
   if(world.getBlockState(b).isOf(Blocks.RED_TERRACOTTA)){world.breakBlock(b,false,player);brokenBlocks++;}
  }
  if(next.length>=18){proxy.setPosition(next[10],FLOOR+next[12],next[11]);if(next[9]<=0)proxy.discard();}
  state=next;
 }
}
