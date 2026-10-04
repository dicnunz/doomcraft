package dev.doomcraft;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class SurvivalMode {
 public static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("doomcraft.survival",Boolean.getBoolean("doomcraft.night")||Boolean.getBoolean("doomcraft.autoLab")?"false":"true"));
 public static final Item BULLETS=pack("bullet_pack",20,false),SHELLS=pack("shell_pack",8,true);
 private static SurvivalState saved;
 private static Item pack(String id,int count,boolean shells){return Registry.register(Registries.ITEM,Identifier.of("doomcraft",id),new Item(new Item.Settings()){
  @Override public TypedActionResult<ItemStack> use(World w,PlayerEntity p,Hand h){
   var stack=p.getStackInHand(h);if(!ENABLED||!NightCombat.active)return TypedActionResult.pass(stack);
   if(w.isClient)return TypedActionResult.success(stack);
   if(p!=NightCombat.player)return TypedActionResult.pass(stack);
   double[] state=NativeBridge.weaponStatus();int b=(int)state[10],s=(int)state[11];int before=shells?s:b,cap=shells?50:200;
   if(before>=cap){p.sendMessage(Text.literal("Ammo reserve full"),true);return TypedActionResult.fail(stack);}
   if(shells)s=Math.min(50,s+count);else b=Math.min(200,b+count);
   NativeBridge.setAmmo(b,s);NightCombat.state=NativeBridge.weaponStatus();store();if(!p.isCreative())stack.decrement(1);
   p.sendMessage(Text.literal("+"+(Math.min(cap,before+count)-before)+(shells?" shells":" bullets")),true);return TypedActionResult.success(stack);
  }
 });}
 static void install(){
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
   if(!ENABLED||!server.isSingleplayer())return;
   ServerPlayerEntity p=handler.player;
   saved=server.getOverworld().getPersistentStateManager().getOrCreate(SurvivalState.TYPE,"doomcraft_survival");
   int[] ammo=saved.players.get(p.getUuid());
   if(ammo==null){ammo=new int[]{60,12};saved.players.put(p.getUuid(),ammo);saved.markDirty();
    p.getInventory().offerOrDrop(new ItemStack(DoomCraft.PISTOL));p.getInventory().offerOrDrop(new ItemStack(NightCombat.SHOTGUN));
    p.sendMessage(Text.literal("DOOMCRAFT • Freedoom marine • Right-click: fire / ammo • E: inventory • F5: character"),false);
   }
   NightCombat.attachSurvival(p,ammo[0],ammo[1]);
   p.unlockRecipes(java.util.List.of(Identifier.of("doomcraft","bullet_pack"),Identifier.of("doomcraft","shell_pack"),Identifier.of("doomcraft","pistol"),Identifier.of("doomcraft","shotgun")));
   System.out.println("SURVIVAL_JOIN uuid="+p.getUuid()+" bullets="+ammo[0]+" shells="+ammo[1]);
  });
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{if(ENABLED)store();});
 }
 static void store(){
  if(saved==null||NightCombat.player==null||NightCombat.state.length<12)return;
  int b=(int)NightCombat.state[10],s=(int)NightCombat.state[11];var id=NightCombat.player.getUuid();int[] old=saved.players.get(id);
  if(old==null||old[0]!=b||old[1]!=s){saved.players.put(id,new int[]{b,s});saved.markDirty();}
 }
}
