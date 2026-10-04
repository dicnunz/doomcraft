package dev.doomcraft;
/** Called only by the integrated server thread. Never from the renderer. */
public final class NativeBridge {
 static {
  String override=System.getProperty("doomcraft.native");
  if(override!=null)System.load(java.nio.file.Path.of(override).toAbsolutePath().toString());
  else {
   if(!System.getProperty("os.name").contains("Mac")||!System.getProperty("os.arch").equals("aarch64"))throw new IllegalStateException("DoomCraft 0.3.0 includes an Apple Silicon native engine; other platforms need a native build and -Ddoomcraft.native=/path/to/library");
   try(var in=NativeBridge.class.getResourceAsStream("/natives/macos-aarch64/libdoomcraft.dylib")){
    if(in==null)throw new IllegalStateException("Native Doom engine missing: rebuild with ./tools/gradle.sh build");
    var dir=java.nio.file.Files.createTempDirectory("doomcraft-native-");var lib=dir.resolve("libdoomcraft.dylib");
    dir.toFile().deleteOnExit();lib.toFile().deleteOnExit();java.nio.file.Files.copy(in,lib);System.load(lib.toString());
   }catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}
  }
 }
 public static native void init();
 public static native void initWeapons();
 public static native void setAmmo(int bullets,int shells);
 public static native void selectWeapon(int weapon);
 public static native double[] shots();
 public static native double[] weaponStatus();
 public static native void grid(byte[] cells);
 public static native void pose(double x,double z,double y,double yaw,double pitch,int health,boolean fire);
 public static native void tick();
 public static native void hit(int damage);
 public static native double[] snapshot();
 public static native int[] blocks();
}
