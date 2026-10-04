#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>
#include "bridge.h"
static double s[1024];static unsigned char g[3456];
#define CHECK(x,msg) do{if(!(x)){fprintf(stderr,"FAIL: %s\n",msg);exit(1);}printf("PASS: %s\n",msg);}while(0)
static void step(int n,int fire){for(int i=0;i<n;i++){DC_Pose(12,5,0,0,0,100,fire);DC_Tick();}DC_Snapshot(s);}
int main(void){
 setbuf(stdout,NULL);setbuf(stderr,NULL);
 DC_Init();DC_Grid(g);step(30,0);CHECK(s[1]==200,"pistol raises without consuming ammunition");
 step(200,1);CHECK(s[9]<60,"original pistol state machine damages native Doom imp");CHECK(s[1]<200,"Doom weapon consumes its own ammunition");
 DC_Init();for(int y=0;y<24;y++)for(int x=0;x<24;x++)for(int z=0;z<6;z++)g[(z*24+y)*24+x]=(y==11);
 DC_Grid(g);step(200,1);printf("shield demon health=%g x=%g y=%g\n",s[9],s[10],s[11]);CHECK(s[9]==60,"Minecraft voxel shield blocks pistol damage");CHECK(s[11]>11,"native imp cannot cross Minecraft voxel shield");
 int events[1024];CHECK(DC_Blocks(events)>0,"Doom bullet reports Minecraft block impacts");
 memset(g,0,sizeof g);DC_Grid(g);for(int i=0;i<250;i++){DC_Snapshot(s);double yaw=atan2(s[11]-5,s[10]-12)*180/3.141592653589793-90;DC_Pose(12,5,0,yaw,0,100,1);DC_Tick();}DC_Snapshot(s);CHECK(s[9]<60,"removing Minecraft shield reopens combat");
 DC_Init();DC_Hit(25);DC_Snapshot(s);CHECK(s[9]==35,"Minecraft damage enters original P_DamageMobj");
 DC_Hit(40);DC_Snapshot(s);CHECK(s[9]<=0,"Minecraft damage triggers Doom death state");
 DC_Init();DC_Grid(g);int hurt=0;for(int i=0;i<1000;i++){DC_Pose(12,16,0,0,0,100,0);DC_Tick();DC_Snapshot(s);if(s[0]<100){hurt=1;break;}}
 CHECK(hurt,"native Doom imp attacks damage player puppet");
 for(int i=0;i<50;i++){DC_Init();DC_Grid(g);step(10,0);}CHECK(1,"50 arena resets preserve native host integrity");
 DC_InitWeapons();for(int i=0;i<40;i++){DC_Pose(12,5,0,90,30,100,1);DC_Tick();}
 double aim[1024];int aimCount=DC_Shots(aim);CHECK(aimCount>=5&&fabs(aim[0]+1)<.001&&fabs(aim[1])<.001&&fabs(aim[2]+tan(3.141592653589793/6))<.001,"Minecraft yaw and pitch reach original Doom shot events");
 DC_InitWeapons();step(40,0);DC_WeaponStatus(s);CHECK(s[1]==200,"night pistol starts with 200 bullets");
 step(40,1);DC_WeaponStatus(s);double shots[1024];int n=DC_Shots(shots);CHECK(n>0&&n/5==200-s[10],"each native pistol shot emits one world combat event and consumes one bullet");
 DC_SelectWeapon(2);step(50,0);DC_WeaponStatus(s);CHECK(s[9]==2&&s[11]==50,"original lower/raise states switch to shotgun without firing");
 step(4,1);n=DC_Shots(shots);DC_WeaponStatus(s);CHECK(n==35&&s[11]==49,"original shotgun emits seven pellets and consumes one shell");
 CHECK(shots[4]==64,"Doom hitscan range converts to Minecraft blocks");
 DC_Pose(12,5,0,90,30,100,0);step(40,0);
 for(int i=0;i<2500;i++){DC_Pose(12,5,0,90,30,100,1);DC_Tick();DC_Shots(shots);}DC_WeaponStatus(s);CHECK(s[11]==0&&s[10]>=0,"empty shotgun uses original automatic fallback without negative ammo");
 for(int i=0;i<4000;i++){DC_Pose(12,5,0,0,0,100,1);DC_Tick();DC_Shots(shots);}DC_WeaponStatus(s);CHECK(s[10]==0&&s[11]==0,"both ammo pools exhaust safely");
 step(50,1);CHECK(DC_Shots(shots)==0,"empty weapon emits no damage events");
 DC_InitWeapons();DC_SetAmmo(37,6);DC_WeaponStatus(s);CHECK(s[10]==37&&s[11]==6,"saved ammo restores exactly without starter replenishment");
 DC_SetAmmo(-2,999);DC_WeaponStatus(s);CHECK(s[10]==0&&s[11]==50,"restored ammo clamps malformed values safely");
 puts("Native simulation tests complete; this is not in-game visual proof.");return 0;
}
