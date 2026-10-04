package dev.doomcraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;

/** Opt-in developer harness. Always creates a NEW world in the isolated run directory. */
public final class LabAutomation {
 private static boolean created,started;
 private static int ticks;
 public static void install(){
  if(!Boolean.getBoolean("doomcraft.autoLab"))return;
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(!created&&mc.currentScreen!=null&&mc.currentScreen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))mc.setScreen(new TitleScreen());
   if(!created&&mc.currentScreen instanceof TitleScreen&&mc.getOverlay()==null){
    created=true;mc.options.pauseOnLostFocus=false;mc.options.getChatVisibility().setValue(net.minecraft.network.message.ChatVisibility.FULL);
    mc.createIntegratedServerLoader().createAndStart((NightCombat.ENABLED?"DoomCraft-Night-":"DoomCraft-Lab-")+System.currentTimeMillis(),
      new LevelInfo(NightCombat.ENABLED?"DoomCraft Night":"DoomCraft Lab",GameMode.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE),
      new GeneratorOptions(1234,false,false),r->r.get(RegistryKeys.WORLD_PRESET).get(NightCombat.ENABLED?WorldPresets.DEFAULT:WorldPresets.FLAT).createDimensionsRegistryHolder(),new TitleScreen());
   }
   if(created&&!started&&mc.player!=null&&mc.getNetworkHandler()!=null){
    started=true;mc.getNetworkHandler().sendChatCommand(NightCombat.ENABLED?"doomnight":"doomcraft");
   }
   if(NightCombat.ENABLED){NightDemos.tick(mc);return;}
   if(DeveloperDemos.ENABLED){DeveloperDemos.tick(mc);return;}
   if(!DoomCraft.active)return;
   ticks++;
   if(ticks==40)capture(mc,"01-arena.png");
   if(!Boolean.getBoolean("doomcraft.verify"))return;
   if(ticks==60){mc.options.useKey.setPressed(true);System.out.println("DOOMCRAFT_VERIFY holding pistol use key");}
   if(ticks==140)capture(mc,"02-combat.png");
   if(ticks>140&&ticks<330&&DoomCraft.state.length>=18){
    double[] st=DoomCraft.state;double dx=st[10]-mc.player.getX(),dz=st[11]-mc.player.getZ();
    mc.player.setYaw((float)Math.toDegrees(Math.atan2(-dx,dz)));
    mc.player.setPitch((float)Math.toDegrees(Math.atan2(mc.player.getEyeY()-DoomCraft.FLOOR-st[12]-.9,Math.hypot(dx,dz))));
   }
   if(ticks>=140&&ticks<=320&&ticks%2==0)capture(mc,String.format("demo-%03d.png",(ticks-140)/2));
   if(ticks==330){
    mc.options.useKey.setPressed(false);capture(mc,"03-pistol-result.png");
    System.out.println("DOOMCRAFT_VERIFY pistol demon_health="+DoomCraft.state[9]+" ammo="+DoomCraft.state[1]+" destroyed_mc_blocks="+DoomCraft.brokenBlocks+" damage_to_mc_player="+DoomCraft.receivedDamage);
    mc.getNetworkHandler().sendChatCommand("doomcraft");
   }
   if(ticks==360){
    mc.player.getInventory().selectedSlot=1;
    mc.getServer().execute(()->{var p=mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());double[] st=DoomCraft.state;p.teleport(p.getServerWorld(),st[10],DoomCraft.FLOOR,st[11]-2,0,0);});
   }
   if(ticks==385||ticks==410||ticks==435){
    for(var e:mc.world.getEntities())if(e instanceof DoomCraft.Demon&&e.isAlive()){mc.interactionManager.attackEntity(mc.player,e);mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);break;}
   }
   if(ticks==450){capture(mc,"04-minecraft-sword.png");System.out.println("DOOMCRAFT_VERIFY sword demon_health="+DoomCraft.state[9]+" mc_melee_events="+DoomCraft.meleeEvents);}
   if(ticks==470){mc.getNetworkHandler().sendChatCommand("doomcraft");}

  });
 }
 private static void capture(MinecraftClient mc,String name){
  ScreenshotRecorder.saveScreenshot(mc.runDirectory,name,mc.getFramebuffer(),text->System.out.println("DOOMCRAFT_CAPTURE "+text.getString()));
 }
}
