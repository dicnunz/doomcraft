#include <jni.h>
#include "bridge.h"
JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_init(JNIEnv *e,jclass c){DC_Init();}
JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_grid(JNIEnv *e,jclass c,jbyteArray a){
 if((*e)->GetArrayLength(e,a)!=3456)return;unsigned char b[3456];(*e)->GetByteArrayRegion(e,a,0,3456,(jbyte*)b);DC_Grid(b);
}
JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_pose(JNIEnv *e,jclass c,jdouble x,jdouble y,jdouble z,jdouble yaw,jdouble pitch,jint health,jboolean fire){DC_Pose(x,y,z,yaw,pitch,health,fire);}
JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_tick(JNIEnv *e,jclass c){DC_Tick();}
JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_hit(JNIEnv *e,jclass c,jint d){DC_Hit(d);}
JNIEXPORT jdoubleArray JNICALL Java_dev_doomcraft_NativeBridge_snapshot(JNIEnv *e,jclass c){double v[1024];int n=DC_Snapshot(v);jdoubleArray a=(*e)->NewDoubleArray(e,n);(*e)->SetDoubleArrayRegion(e,a,0,n,v);return a;}
JNIEXPORT jintArray JNICALL Java_dev_doomcraft_NativeBridge_blocks(JNIEnv *e,jclass c){int v[1024];int n=DC_Blocks(v);jintArray a=(*e)->NewIntArray(e,n);(*e)->SetIntArrayRegion(e,a,0,n,v);return a;}

JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_initWeapons(JNIEnv *e,jclass c){DC_InitWeapons();}
JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_selectWeapon(JNIEnv *e,jclass c,jint w){DC_SelectWeapon(w);}
JNIEXPORT jdoubleArray JNICALL Java_dev_doomcraft_NativeBridge_shots(JNIEnv *e,jclass c){double v[1024];int n=DC_Shots(v);jdoubleArray a=(*e)->NewDoubleArray(e,n);(*e)->SetDoubleArrayRegion(e,a,0,n,v);return a;}
JNIEXPORT jdoubleArray JNICALL Java_dev_doomcraft_NativeBridge_weaponStatus(JNIEnv *e,jclass c){double v[1024];int n=DC_WeaponStatus(v);jdoubleArray a=(*e)->NewDoubleArray(e,n);(*e)->SetDoubleArrayRegion(e,a,0,n,v);return a;}

JNIEXPORT void JNICALL Java_dev_doomcraft_NativeBridge_setAmmo(JNIEnv *e,jclass c,jint bullets,jint shells){DC_SetAmmo(bullets,shells);}
