package dev.doomcraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.world.*;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.item.*;
import net.minecraft.block.Blocks;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;
import java.nio.file.*;

/** Opt-in real-client QA. Staging is limited to the freshly created demo world. */
public final class SurvivalDemo {
 private static final boolean ENABLED=Boolean.getBoolean("doomcraft.survivalDemo");
 private static final boolean SHOWCASE=Boolean.getBoolean("doomcraft.showcase");
 private static final String CAPTURE="final-"+System.currentTimeMillis()+"-";
 private static int phase,t,frames;private static boolean requested,failed;
 private static final String RESUME=System.getProperty("doomcraft.survivalResume");
 private static String worldName=RESUME!=null?RESUME:"DoomCraft Survival "+System.currentTimeMillis();
 private static volatile boolean ready;private static volatile BlockPos log,table,stand;private static UUID enemy;
 private static boolean resumedAfterDeath;
 private static java.util.List<BlockPos> buildSites;
 private static int bullets,shells;private static Vec3d deathPos;private static int progress;
 public static void install(){if(ENABLED)System.out.println("SURVIVAL_CAPTURE_PREFIX "+CAPTURE);if(ENABLED)ClientTickEvents.END_CLIENT_TICK.register(SurvivalDemo::tick);}
 private static void next(int p){System.out.println("SURVIVAL_PHASE "+phase+" -> "+p);phase=p;t=frames=0;requested=false;System.out.println("SHOWCASE_MARK phase="+p+" time="+System.currentTimeMillis());}
 private static void check(boolean b,String m){System.out.println("SURVIVAL_CHECK "+(b?"PASS ":"FAIL ")+m);if(!b){failed=true;throw new IllegalStateException(m);}}
 private static List<Element> elements(ParentElement p){List<Element> result=new ArrayList<>();for(Element e:p.children()){result.add(e);if(e instanceof ParentElement pp)result.addAll(elements(pp));}return result;}
 private static boolean click(MinecraftClient mc,String text){if(mc.currentScreen==null)return false;for(var e:elements(mc.currentScreen))if(e instanceof ButtonWidget b&&b.active&&b.getMessage().getString().equals(text)){b.onPress();return true;}return false;}
 private static void aim(MinecraftClient mc,Vec3d v){Vec3d d=v.subtract(mc.player.getEyePos());mc.player.setYaw((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setPitch((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
 private static void capture(MinecraftClient mc){ScreenshotRecorder.saveScreenshot(mc.runDirectory,CAPTURE+String.format("s%02d-%04d.png",phase,frames++),mc.getFramebuffer(),txt->{});}
 private static void slot(MinecraftClient mc,int id,int button,SlotActionType action){mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,id,button,action,mc.player);}
 private static int find(MinecraftClient mc,Item item){for(var s:mc.player.currentScreenHandler.slots)if(s.getStack().isOf(item))return s.id;return -1;}
 private static int count(MinecraftClient mc,Item item){int n=0;for(int i=0;i<36;i++)if(mc.player.getInventory().getStack(i).isOf(item))n+=mc.player.getInventory().getStack(i).getCount();return n;}
 private static int inventory(MinecraftClient mc,Item item){for(int i=0;i<36;i++)if(mc.player.getInventory().getStack(i).isOf(item))return i;return -1;}
 private static void hold(MinecraftClient mc,Item item,int hotbar){int i=inventory(mc,item);check(i>=0,"item available: "+item);if(i<9)mc.player.getInventory().selectedSlot=i;else {int id=find(mc,item);slot(mc,id,hotbar,SlotActionType.SWAP);mc.player.getInventory().selectedSlot=hotbar;}}
 private static void closeScreen(MinecraftClient mc){if(mc.currentScreen instanceof HandledScreen<?>)mc.player.closeHandledScreen();else mc.setScreen(null);}
 private static void tick(MinecraftClient mc){
  if(failed)return;
  try{run(mc);}catch(Throwable e){e.printStackTrace();failed=true;System.out.println("SURVIVAL_DEMO_FAILED phase="+phase+" tick="+t);mc.options.useKey.setPressed(false);mc.options.attackKey.setPressed(false);mc.scheduleStop();}
 }
 private static void run(MinecraftClient mc){
  if(SHOWCASE&&phase==0&&!Files.exists(Path.of("/tmp/doomcraft-recording-ready")))return;
  t++;
  if(mc.currentScreen!=null&&mc.currentScreen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))mc.setScreen(new TitleScreen());
  mc.options.pauseOnLostFocus=false;
  if(phase!=12&&mc.currentScreen instanceof GameMenuScreen)closeScreen(mc);
  boolean record=phase!=3&&phase!=7&&phase!=13&&phase!=14;
  if(record&&!SHOWCASE&&mc.getOverlay()==null)capture(mc);
  if(phase==0&&t==1){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getHandle(),1280,720);mc.options.getMaxFps().setValue(60);}
  if(phase==0){if(mc.currentScreen instanceof TitleScreen&&mc.getOverlay()==null){if(!requested){t=0;frames=0;requested=true;}if(t>40&&click(mc,"Singleplayer")){if(RESUME!=null){readCheckpoint();next(13);}else next(1);}}return;}
  if(phase==1){if(mc.currentScreen instanceof CreateWorldScreen){next(2);return;}if(mc.currentScreen instanceof SelectWorldScreen&&t>15)click(mc,"Create New World");return;}
  if(phase==2){if(!(mc.currentScreen instanceof CreateWorldScreen c))return;if(!requested){c.getWorldCreator().setWorldName(worldName);c.getWorldCreator().setSeed("1234");c.getWorldCreator().setDifficulty(Difficulty.NORMAL);c.getWorldCreator().setCheatsEnabled(false);c.getWorldCreator().setGameMode(WorldCreator.Mode.SURVIVAL);for(var e:elements(c))if(e instanceof TextFieldWidget f){f.setText(worldName);break;}requested=true;}
   if(t>65&&click(mc,"Create New World"))next(3);return;}
  if(phase==3){if(mc.player!=null&&NightCombat.active&&mc.currentScreen==null){mc.options.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MASTER).setValue(1.0);mc.options.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MUSIC).setValue(0.0);mc.options.getGamma().setValue(1.0);mc.options.getGuiScale().setValue(3);mc.options.getFov().setValue(74);mc.options.getChatVisibility().setValue(net.minecraft.network.message.ChatVisibility.FULL);mc.onResolutionChanged();next(4);}return;}
  if(mc.player==null&&phase!=13&&phase!=14)return;
  if((phase>=4&&phase<=11)||phase>=18){mc.inGameHud.getChatHud().clear(false);mc.getToastManager().clear();}
  if(phase==4){if(t==10){check(mc.player.getMainHandStack().isOf(DoomCraft.PISTOL),"new world receives pistol in ordinary inventory");check(NightCombat.state[10]==60&&NightCombat.state[11]==12,"starter reserves initialized once");check(!mc.player.isCreative(),"normal survival mode");}if(t>60){next(5);mc.setScreen(new InventoryScreen(mc.player));}return;}
  if(phase==5){if(t==45){closeScreen(mc);mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);mc.player.setPitch(-5);}if(t>95){mc.options.setPerspective(Perspective.FIRST_PERSON);next(6);}return;}
  if(phase==6&&SHOWCASE){if(ShowcaseCombat.tick(mc)){check(NightCombat.state[10]<60&&NightCombat.state[11]<12,"both native weapons consume survival reserves");check(NightCombat.kills>=3,"showcase defeats three real Minecraft mobs");next(7);}return;}
  if(phase==6){
   if(!requested){ready=false;requested=true;mc.getServer().execute(()->{var p=NightCombat.player;var w=p.getServerWorld();w.setTimeOfDay(14500);for(var e:w.getEntitiesByClass(HostileEntity.class,p.getBoundingBox().expand(25),e->true))e.discard();
    // Stage one real, adult zombie in a nearby visible patch for repeatable combat.
    BlockPos b=combatSpot();var z=EntityType.ZOMBIE.spawn(w,b,SpawnReason.COMMAND);if(z!=null){z.setBaby(false);z.setTarget(p);enemy=z.getUuid();}ready=true;});}
   if(!ready){if(t>300)check(false,"combat staging completed");return;}mc.player.getInventory().selectedSlot=1;
   LivingEntity target=null;for(var e:mc.world.getEntities())if(e instanceof LivingEntity l&&e.getUuid().equals(enemy)&&e.isAlive())target=l;
   if(target!=null){aim(mc,target.getPos().add(0,1.2,0));mc.options.useKey.setPressed(t>30);}else mc.options.useKey.setPressed(false);
   if(t>180){mc.options.useKey.setPressed(false);check(NightCombat.state[11]<12,"native shotgun consumes saved survival ammo");check(NightCombat.damageDealt>0,"native shots damage a real Minecraft mob");next(7);}return;
  }
  if(phase==7){if(t>400)check(false,"natural mining location search completed");if(!requested){requested=true;ready=false;mc.getServer().execute(()->{try{prepareMining(mc);}catch(Throwable e){e.printStackTrace();failed=true;mc.execute(mc::scheduleStop);}});}if(ready){mc.player.getInventory().selectedSlot=2;aim(mc,Vec3d.ofCenter(log));next(8);}return;}
  if(phase==8){
   if(t==1){org.lwjgl.glfw.GLFW.glfwFocusWindow(mc.getWindow().getHandle());mc.mouse.lockCursor();}
   if(t%40==0)System.out.println("MINING_DIAGNOSTIC t="+t+" log="+log+" state="+mc.world.getBlockState(log)+" pos="+mc.player.getPos()+" target="+mc.crosshairTarget+" locked="+mc.mouse.isCursorLocked()+" using="+mc.player.isUsingItem()+" count="+count(mc,Items.OAK_LOG));
   aim(mc,Vec3d.ofCenter(log));
   if(mc.world.getBlockState(log).isOf(Blocks.OAK_LOG)){mc.options.attackKey.setPressed(true);}
   else {
    mc.options.attackKey.setPressed(false);
    net.minecraft.entity.ItemEntity drop=null;double nearest=100;
    for(var e:mc.world.getEntities())if(e instanceof net.minecraft.entity.ItemEntity it&&it.getStack().isOf(Items.OAK_LOG)&&it.squaredDistanceTo(mc.player)<nearest){drop=it;nearest=it.squaredDistanceTo(mc.player);}
    if(drop!=null){aim(mc,drop.getPos().add(0,mc.player.getStandingEyeHeight(),0));mc.options.jumpKey.setPressed(mc.player.horizontalCollision||drop.getY()>mc.player.getY()+.3);}
    mc.options.forwardKey.setPressed(count(mc,Items.OAK_LOG)<2);
   }
   if(count(mc,Items.OAK_LOG)==1&&mc.world.getBlockState(log).isAir()&&mc.world.getBlockState(log.up()).isOf(Blocks.OAK_LOG)){log=log.up();mc.options.forwardKey.setPressed(false);mc.options.jumpKey.setPressed(false);}
   if(count(mc,Items.OAK_LOG)>=2){mc.options.attackKey.setPressed(false);mc.options.forwardKey.setPressed(false);mc.options.jumpKey.setPressed(false);check(mc.world.getBlockState(log).isAir(),"mined natural oak log removed from world");check(inventory(mc,Items.OAK_LOG)>=0,"mined log picked up through vanilla item pickup");next(9);mc.setScreen(new InventoryScreen(mc.player));}
   else if(t>400)check(false,"mining/pickup completed within capture budget");return;
  }
  if(phase==9){
   if(t==20){int s=find(mc,Items.OAK_LOG);check(s>=9,"log is in inventory");slot(mc,s,0,SlotActionType.PICKUP);slot(mc,1,0,SlotActionType.PICKUP);}
   if(t==35){check(mc.player.currentScreenHandler.getSlot(0).getStack().isOf(Items.OAK_PLANKS),"vanilla log-to-planks recipe result");slot(mc,0,0,SlotActionType.QUICK_MOVE);}
   if(t==55){int s=find(mc,Items.OAK_PLANKS);check(s>=9,"crafted planks in inventory");slot(mc,s,0,SlotActionType.PICKUP);for(int i=1;i<=4;i++)slot(mc,i,1,SlotActionType.PICKUP);}
   if(t==75){check(mc.player.currentScreenHandler.getSlot(0).getStack().isOf(Items.CRAFTING_TABLE),"vanilla 2x2 workbench recipe result");slot(mc,0,0,SlotActionType.QUICK_MOVE);if(!mc.player.currentScreenHandler.getCursorStack().isEmpty())for(var empty:mc.player.currentScreenHandler.slots)if(empty.id>=9&&empty.getStack().isEmpty()){slot(mc,empty.id,0,SlotActionType.PICKUP);break;}}
   if(t>105){check(inventory(mc,Items.CRAFTING_TABLE)>=0,"crafting output delivered to inventory");closeScreen(mc);next(10);}return;
  }
  if(phase==10){
   if(t==1){hold(mc,Items.CRAFTING_TABLE,3);table=null;for(Direction dir:Direction.Type.HORIZONTAL){BlockPos b=mc.player.getBlockPos().offset(dir,2);if(mc.world.getBlockState(b).isAir()&&mc.world.getBlockState(b.down()).isSolidBlock(mc.world,b.down())){table=b;break;}}check(table!=null,"reachable placement surface found");}
   if(table!=null)aim(mc,Vec3d.ofCenter(table));
   if(t==10){BlockPos ground=table.down();aim(mc,Vec3d.ofCenter(ground));mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(ground).add(0,.5,0),Direction.UP,ground,false));}
   if(t==30){check(mc.world.getBlockState(table).isOf(Blocks.CRAFTING_TABLE),"crafted workbench placed with normal block interaction");mc.player.getInventory().selectedSlot=0;bullets=(int)NightCombat.state[10];aim(mc,Vec3d.ofCenter(table));mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(table),Direction.UP,table,false));}
   if(t==45){check(mc.currentScreen instanceof CraftingScreen,"right-click workbench opens crafting while gun held");check(NightCombat.state[10]==bullets,"workbench interaction does not accidentally fire gun");}
   if(t>80){closeScreen(mc);next(21);}return;
  }
  if(phase==21){
   if(t==1){hold(mc,Items.OAK_PLANKS,3);buildSites=new java.util.ArrayList<>();BlockPos c=mc.player.getBlockPos();
    for(BlockPos q:BlockPos.iterate(c.add(-3,-2,-3),c.add(3,1,3))){
     if(mc.player.getBoundingBox().intersects(new Box(q))||q.equals(table)||q.equals(c)||q.equals(c.up())||!mc.world.getBlockState(q).isAir()||!mc.world.getBlockState(q.down()).isSolidBlock(mc.world,q.down()))continue;
     Vec3d point=Vec3d.ofCenter(q.down()).add(0,.499,0);if(point.squaredDistanceTo(mc.player.getEyePos())>16)continue;
     var hit=mc.world.raycast(new RaycastContext(mc.player.getEyePos(),point,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,mc.player));
     if(hit.getBlockPos().equals(q.down())&&hit.getSide()==Direction.UP)buildSites.add(q.toImmutable());
    }
    buildSites.sort(java.util.Comparator.comparingDouble((BlockPos q)->q.getSquaredDistance(c)).reversed());check(buildSites.size()>=3,"three visible reachable building surfaces found");buildSites=new java.util.ArrayList<>(buildSites.subList(0,3));
   }
   int level=Math.min(2,Math.max(0,(t-20)/25));BlockPos target=buildSites.get(level);aim(mc,Vec3d.ofCenter(target.down()).add(0,.5,0));
   if(t==20||t==45||t==70){var support=target.down();var result=mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(support).add(0,.5,0),Direction.UP,support,false));System.out.println("BUILDING_INTERACTION target="+target+" result="+result+" held="+mc.player.getMainHandStack());mc.player.swingHand(Hand.MAIN_HAND);}
   if(t==90){check(buildSites.stream().allMatch(q->mc.world.getBlockState(q).isOf(Blocks.OAK_PLANKS)),"three crafted planks placed through normal building interactions");check(count(mc,Items.OAK_PLANKS)==1,"building consumes three actual crafted blocks");}
   if(t>110)next(11);return;
  }
  if(phase==11){
   if(t==1){mc.getServer().execute(()->{var p=NightCombat.player;p.getInventory().setStack(4,new ItemStack(SurvivalMode.BULLETS));p.getInventory().setStack(5,new ItemStack(Items.BREAD));p.getHungerManager().setFoodLevel(12);});}
   if(t==15){mc.player.setPitch(-65);mc.player.getInventory().selectedSlot=4;bullets=(int)NightCombat.state[10];mc.options.useKey.setPressed(true);}
   if(t==30){mc.options.useKey.setPressed(false);check(NightCombat.state[10]==bullets+20,"ammo pack refills exactly 20 bullets");check(mc.player.getInventory().getStack(4).isEmpty(),"ammo refill consumes the pack");mc.player.getInventory().selectedSlot=5;mc.options.useKey.setPressed(true);}
   if(t==80){mc.options.useKey.setPressed(false);check(mc.player.getHungerManager().getFoodLevel()>12,"normal eating restores visible hunger");check(mc.player.getInventory().getStack(5).isEmpty(),"normal food item consumed");bullets=(int)NightCombat.state[10];shells=(int)NightCombat.state[11];}
   if(t>100){writeCheckpoint(false);mc.openGameMenu(false);next(12);}return;
  }
  if(phase==12){if(t>35&&click(mc,"Save and Quit to Title"))next(13);return;}
  if(phase==13){if(mc.currentScreen instanceof TitleScreen&&t>20)click(mc,"Singleplayer");if(mc.currentScreen instanceof SelectWorldScreen s){for(var e:elements(s))if(e instanceof WorldListWidget list){for(var entry:list.children())if(entry instanceof WorldListWidget.WorldEntry w&&w.getLevelDisplayName().equals(worldName)){list.setSelected(w);if(t>50){w.play();next(14);}return;}}}return;}
  if(phase==14){if(mc.player!=null&&NightCombat.active&&mc.currentScreen==null){check(NightCombat.state[10]==bullets&&NightCombat.state[11]==shells,"ammo survives actual Save/Quit and world reload");if(table==null){BlockPos c=mc.player.getBlockPos();for(BlockPos q:BlockPos.iterate(c.add(-5,-3,-5),c.add(5,3,5)))if(mc.world.getBlockState(q).isOf(Blocks.CRAFTING_TABLE)){table=q.toImmutable();break;}}check(table!=null&&mc.world.getBlockState(table).isOf(Blocks.CRAFTING_TABLE),"placed block survives actual world reload");check(resumedAfterDeath?inventory(mc,DoomCraft.PISTOL)<0:(inventory(mc,DoomCraft.PISTOL)>=0&&inventory(mc,NightCombat.SHOTGUN)>=0),resumedAfterDeath?"restart after death does not duplicate starter weapons":"vanilla inventory survives reload");
    if(resumedAfterDeath){System.out.println("SURVIVAL_DEMO_ALL_CHECKS_PASSED");next(17);}else next(RESUME!=null?18:19);}return;}
  if(phase==18){
   if(t==1)mc.getServer().execute(()->{NightCombat.player.getInventory().offerOrDrop(new ItemStack(Items.IRON_NUGGET));NightCombat.player.getInventory().offerOrDrop(new ItemStack(Items.GUNPOWDER));});
   if(t==10)mc.setScreen(new InventoryScreen(mc.player));
   if(t==20){int a=find(mc,Items.IRON_NUGGET);check(a>=9,"ammo ingredient available");slot(mc,a,0,SlotActionType.PICKUP);slot(mc,1,0,SlotActionType.PICKUP);int b=find(mc,Items.GUNPOWDER);slot(mc,b,0,SlotActionType.PICKUP);slot(mc,2,0,SlotActionType.PICKUP);}
   if(t==40){check(mc.player.currentScreenHandler.getSlot(0).getStack().isOf(SurvivalMode.BULLETS),"renewable bullet-pack recipe works in actual crafting UI");slot(mc,0,0,SlotActionType.QUICK_MOVE);}
   if(t==60){closeScreen(mc);hold(mc,SurvivalMode.BULLETS,4);mc.player.setPitch(-60);mc.options.useKey.setPressed(true);}
   if(t==80){mc.options.useKey.setPressed(false);check(NightCombat.state[10]==bullets+20,"crafted ammunition is usable");bullets=(int)NightCombat.state[10];}
   if(t>100)next(20);return;
  }
  if(phase==15){if(t>=100&&t%40==0&&mc.player.isAlive()){deathPos=mc.player.getPos();mc.getServer().execute(()->NightCombat.player.damage(NightCombat.world.getDamageSources().generic(),1000));}if(mc.currentScreen instanceof DeathScreen&&++progress>30){check(mc.player.getHealth()==0,"normal survival damage reaches death screen");mc.player.requestRespawn();closeScreen(mc);next(16);}return;}
  if(phase==16){if(t>25&&mc.player.isAlive()){check(NightCombat.state[10]==bullets&&NightCombat.state[11]==shells,"death/respawn does not replenish or lose reserve ammo");check(inventory(mc,DoomCraft.PISTOL)<0,"death drops inventory and does not duplicate starter weapons");writeCheckpoint(true);System.out.println("SURVIVAL_DEMO_ALL_CHECKS_PASSED");next(17);}return;}
  if(phase==19){
   if(t==1)mc.getServer().execute(()->{var p=NightCombat.player;p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));p.setHealth(12);p.setAbsorptionAmount(4);p.addExperience(100);p.getHungerManager().setFoodLevel(8);p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WATER_BREATHING,1200));p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.REGENERATION,1200));p.setStackInHand(Hand.OFF_HAND,new ItemStack(Items.TORCH,36));var axe=new ItemStack(Items.STONE_AXE);axe.setDamage(40);p.getInventory().setStack(8,axe);});
   if(t==20){check(mc.player.getArmor()==6,"armor value reaches unified HUD");check(mc.player.experienceLevel>0,"real experience reaches unified HUD");check(mc.player.getStatusEffects().size()>=2,"active status effects reach unified HUD");check(mc.player.getOffHandStack().getCount()==36,"offhand stack reaches unified HUD");}
   if(t>20&&t<=110)mc.player.getInventory().selectedSlot=(t-21)/10;
   if(t==120){mc.options.getGuiScale().setValue(2);mc.onResolutionChanged();}
   if(t==140){mc.options.getGuiScale().setValue(4);mc.onResolutionChanged();}
   if(t==160){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getHandle(),960,540);mc.options.getGuiScale().setValue(3);mc.onResolutionChanged();}
   if(t==185){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getHandle(),1280,720);mc.onResolutionChanged();}
   if(t>205){check(mc.player.getInventory().getStack(8).getDamage()==40,"damaged tool retained during all-slot and GUI-scale checks");writeCheckpoint(false);System.out.println("SURVIVAL_DEMO_ALL_CHECKS_PASSED");next(17);}return;
  }
  if(phase==20){
   if(t==1){ready=false;mc.getServer().execute(()->{var p=NightCombat.player;var w=p.getServerWorld();p.clearStatusEffects();p.setHealth(20);stand=p.getBlockPos().add(12,5,0);for(BlockPos b:BlockPos.iterate(stand.add(-2,-1,-2),stand.add(2,3,2))){boolean wall=b.getX()==stand.getX()-2||b.getX()==stand.getX()+2||b.getZ()==stand.getZ()-2||b.getZ()==stand.getZ()+2||b.getY()==stand.getY()-1;w.setBlockState(b,wall?Blocks.GLASS.getDefaultState():Blocks.WATER.getDefaultState());}p.teleport(w,stand.getX()+.5,stand.getY(),stand.getZ()+.5,0,0);ready=true;});}
   if(t==100){check(mc.player.isSubmergedInWater(),"oxygen test uses real underwater state");check(mc.player.getAir()<250,"underwater survival actually depletes air");}
   if(t==125){ready=false;mc.getServer().execute(()->{var p=NightCombat.player;var w=p.getServerWorld();BlockPos q=table.offset(Direction.EAST,5);q=NightCombat.surface(w,q.getX(),q.getZ());p.teleport(w,q.getX()+.5,q.getY(),q.getZ()+.5,0,0);var horse=EntityType.HORSE.spawn(w,q,SpawnReason.COMMAND);if(horse!=null){horse.setTame(true);horse.setOwnerUuid(p.getUuid());p.startRiding(horse,true);}ready=true;});}
   if(t==160){check(mc.player.getVehicle() instanceof net.minecraft.entity.passive.HorseEntity,"mount health rendered from an actual ridden horse");}
   if(t==180)mc.getServer().execute(()->NightCombat.player.stopRiding());
   if(t>200){next(15);}return;
  }
  if(phase==17){if(t>40)mc.scheduleStop();}
 }
 private static Path checkpoint(){return Path.of(System.getProperty("doomcraft.runRoot")).getParent().resolve("docs/final-survival/checkpoint.properties");}
 private static void writeCheckpoint(boolean dead){try{var v=new java.util.Properties();v.setProperty("world",worldName);v.setProperty("bullets",""+bullets);v.setProperty("shells",""+shells);v.setProperty("dead",""+dead);v.setProperty("table",table.getX()+","+table.getY()+","+table.getZ());Files.createDirectories(checkpoint().getParent());try(var out=Files.newOutputStream(checkpoint())){v.store(out,"Actual survival checkpoint for fresh-process verification");}}catch(Exception e){throw new RuntimeException(e);}}
 private static void readCheckpoint(){try{var v=new java.util.Properties();try(var in=Files.newInputStream(checkpoint())){v.load(in);}check(RESUME.equals(v.getProperty("world")),"resume targets the recorded QA world");bullets=Integer.parseInt(v.getProperty("bullets"));shells=Integer.parseInt(v.getProperty("shells"));resumedAfterDeath=Boolean.parseBoolean(v.getProperty("dead"));String[] xyz=v.getProperty("table").split(",");table=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));}catch(Exception e){throw new RuntimeException(e);}}
 private static boolean open(net.minecraft.server.world.ServerWorld w,BlockPos b){return w.getBlockState(b).getCollisionShape(w,b).isEmpty()&&w.getFluidState(b).isEmpty()&&w.getBlockState(b.up()).getCollisionShape(w,b.up()).isEmpty()&&w.getBlockState(b.down()).isSolidBlock(w,b.down());}
 private static BlockPos combatSpot(){
  var p=NightCombat.player;var w=p.getServerWorld();BlockPos base=p.getBlockPos();
  for(int radius=0;radius<=20;radius+=2)for(int dx=-radius;dx<=radius;dx+=2)for(int dz=-radius;dz<=radius;dz+=2){
   if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
   BlockPos q=NightCombat.surface(w,base.getX()+dx,base.getZ()+dz);if(!open(w,q))continue;
   for(Direction dir:Direction.Type.HORIZONTAL){BlockPos c=q.offset(dir,4),b=NightCombat.surface(w,c.getX(),c.getZ());if(!open(w,b)||Math.abs(b.getY()-q.getY())>1)continue;
    if(w.raycast(new RaycastContext(Vec3d.ofBottomCenter(q).add(0,1.62,0),Vec3d.ofBottomCenter(b).add(0,1.2,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p)).getType()==net.minecraft.util.hit.HitResult.Type.MISS){p.teleport(w,q.getX()+.5,q.getY(),q.getZ()+.5,0,0);return b;}
   }
  }
  throw new IllegalStateException("No visible mob staging spot near spawn");
 }
 private static void prepareMining(MinecraftClient mc){
  var p=NightCombat.player;var w=p.getServerWorld();for(var e:w.getEntitiesByClass(HostileEntity.class,p.getBoundingBox().expand(30),e->true))e.discard();w.setTimeOfDay(1000);p.setHealth(20);
  // A repeatable natural tree in the explicitly chosen demo seed. No blocks are generated or replaced.
  BlockPos fixed=new BlockPos(-400,72,-53),foot=new BlockPos(-400,72,-54);
  if(w.getSeed()==1234&&w.getBlockState(fixed).isOf(Blocks.OAK_LOG)&&w.getBlockState(fixed.up()).isOf(Blocks.OAK_LOG)&&open(w,foot)){log=fixed;stand=foot;p.teleport(w,foot.getX()+.5,foot.getY(),foot.getZ()+.5,0,0);ready=true;return;}
  BlockPos center=p.getBlockPos();
  for(int r=1;r<65;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)for(int dy=-24;dy<=24;dy++){
   if(Math.abs(dx)!=r&&Math.abs(dz)!=r)continue;
   BlockPos b=center.add(dx,dy,dz);if(!w.getBlockState(b).isOf(Blocks.OAK_LOG)||!w.getBlockState(b.up()).isOf(Blocks.OAK_LOG))continue;
   for(Direction dir:Direction.Type.HORIZONTAL){BlockPos q=b.offset(dir);if(w.getBlockState(q).isAir()&&w.getBlockState(q.up()).isAir()&&w.getBlockState(q.down()).isSolidBlock(w,q.down())){
    log=b;stand=q;p.teleport(w,q.getX()+.5,q.getY(),q.getZ()+.5,0,0);ready=true;return;
   }}
  }
  throw new IllegalStateException("No accessible natural oak log near demo spawn");
 }
}
