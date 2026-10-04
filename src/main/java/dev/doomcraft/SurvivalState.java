package dev.doomcraft;
import java.util.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
/** World-owned, UUID-keyed reserves; normal Minecraft saving handles atomic disk storage. */
final class SurvivalState extends PersistentState {
 static final Type<SurvivalState> TYPE=new Type<>(SurvivalState::new,SurvivalState::read,null);
 final Map<UUID,int[]> players=new HashMap<>();
 static SurvivalState read(NbtCompound n,RegistryWrapper.WrapperLookup registries){
  var s=new SurvivalState();var entries=n.getCompound("players");
  for(String id:entries.getKeys())try{var p=entries.getCompound(id);s.players.put(UUID.fromString(id),new int[]{Math.clamp(p.getInt("bullets"),0,200),Math.clamp(p.getInt("shells"),0,50)});}catch(IllegalArgumentException ignored){}
  return s;
 }
 @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup registries){
  var entries=new NbtCompound();players.forEach((id,a)->{var p=new NbtCompound();p.putInt("bullets",a[0]);p.putInt("shells",a[1]);entries.put(id.toString(),p);});n.put("players",entries);n.putInt("schema",1);return n;
 }
}
