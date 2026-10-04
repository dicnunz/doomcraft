/* GPL-2.0-or-later. Doom simulation host; all AI/weapon states remain upstream. */
#include <math.h>
#include <string.h>
#include <stdint.h>
#include "bridge.h"
#include "doomstat.h"
#include "d_event.h"

#include "r_state.h"
#include "z_zone.h"
#include "m_random.h"
#include "m_argv.h"
#include "doomgeneric.h"
#define N 24
#define H 6
#define SCALE (32.0*FRACUNIT)
static unsigned char grid[N*N*H];
static sector_t sector;
static subsector_t sub;
static short bm[N*N], bml[1]={-1};
static mobj_t *links[N*N], *demon;
static double pitch;
static int weaponsOnly, shotCount;
static double shots[1020];
static int events[1024], eventCount, initialized;
extern void P_RunThinkers(void);
static double blocks(fixed_t v){return v/SCALE;}
static int cell(int x,int y,int z){return (z*N+y)*N+x;}
static int solid(double x,double y,double z){
 int ix=floor(x),iy=floor(y),iz=floor(z);
 if(ix<0||iy<0||ix>=N||iy>=N||iz<0||iz>=H)return 1;
 return grid[cell(ix,iy,iz)]!=0;
}
boolean DC_Position(mobj_t *m,fixed_t x,fixed_t y){
 double r=blocks(m->radius),zz=blocks(m->z),hh=blocks(m->height);
 for(int z=floor(fmax(0,zz));z<=floor(zz+hh-0.0001);z++)
 for(int b=floor(blocks(y)-r);b<=floor(blocks(y)+r-0.0001);b++)
 for(int a=floor(blocks(x)-r);a<=floor(blocks(x)+r-0.0001);a++)
  if(solid(a+.5,b+.5,z+.5))return false;
 return true;
}
boolean DC_Sight(mobj_t *a,mobj_t *b){
 double ax=blocks(a->x),ay=blocks(a->y),az=blocks(a->z+a->height*3/4);
 double dx=blocks(b->x)-ax,dy=blocks(b->y)-ay,dz=blocks(b->z+b->height/2)-az;
 int steps=(int)(sqrt(dx*dx+dy*dy+dz*dz)*64)+1;
 for(int i=0;i<=steps;i++){double t=(double)i/steps;if(solid(ax+dx*t,ay+dy*t,az+dz*t))return false;}
 return true;
}
fixed_t DC_Slope(void){return (fixed_t)(-tan(pitch*3.141592653589793/180)*FRACUNIT);}
void DC_LineAttack(mobj_t *a,angle_t angle,fixed_t distance,fixed_t slope,int damage){
 double rad=angle*(2*3.141592653589793/4294967296.0),dx=cos(rad),dy=sin(rad),dz=(double)slope/FRACUNIT;
 if(weaponsOnly){if(shotCount+5<=1020){shots[shotCount++]=dx;shots[shotCount++]=dy;shots[shotCount++]=dz;shots[shotCount++]=damage;shots[shotCount++]=blocks(distance);}return;}
 double ax=blocks(a->x),ay=blocks(a->y),az=blocks(a->z)+1.62;
 linetarget=NULL;
 for(double t=0;t<blocks(distance);t+=1.0/64){
  double x=ax+dx*t,y=ay+dy*t,z=az+dz*t;
  if(solid(x,y,z)){
   int ix=floor(x),iy=floor(y),iz=floor(z);
   if(damage>0&&ix>=0&&iy>=0&&iz>=0&&ix<N&&iy<N&&iz<H&&eventCount<1024)events[eventCount++]=cell(ix,iy,iz);
   return;
  }
  for(thinker_t *th=thinkercap.next;th!=&thinkercap;th=th->next){
   if(th->function.acp1!=(actionf_p1)P_MobjThinker)continue;
   mobj_t *m=(mobj_t*)th;
   if(m==a||!(m->flags&MF_SHOOTABLE))continue;
   double rr=blocks(m->radius);
   if(fabs(x-blocks(m->x))<rr&&fabs(y-blocks(m->y))<rr&&z>=blocks(m->z)&&z<=blocks(m->z+m->height)){
    linetarget=m;if(damage)P_DamageMobj(m,a,a,damage);return;
   }
  }
 }
}
void DC_Init(void){
 weaponsOnly=0;shotCount=0;
 if(!initialized){static char *argv[]={"doomcraft",NULL};myargc=1;myargv=argv;Z_Init();initialized=1;}
 else Z_FreeTags(PU_LEVEL,PU_PURGELEVEL-1);
 memset(&sector,0,sizeof sector);memset(&sub,0,sizeof sub);memset(links,0,sizeof links);memset(grid,0,sizeof grid);
 sector.ceilingheight=H*32*FRACUNIT;sector.lightlevel=255;sub.sector=&sector;
 sectors=&sector;numsectors=1;subsectors=&sub;numsubsectors=1;numnodes=0;numlines=0;
 bmaporgx=bmaporgy=0;bmapwidth=bmapheight=N;blockmap=bm;blockmaplump=bml;blocklinks=links;
 P_InitThinkers();M_ClearRandom();memset(players,0,sizeof players);memset(playeringame,0,sizeof playeringame);
 playeringame[0]=true;consoleplayer=0;gameskill=sk_medium;gamemode=commercial;gamestate=GS_LEVEL;leveltime=0;eventCount=0;
 player_t *p=&players[0];p->health=100;p->playerstate=PST_LIVE;p->readyweapon=wp_pistol;p->pendingweapon=wp_nochange;
 p->weaponowned[wp_pistol]=true;p->ammo[am_clip]=200;p->maxammo[am_clip]=200;
 p->mo=P_SpawnMobj(12*SCALE,5*SCALE,0,MT_PLAYER);p->mo->player=p;p->mo->thinker.function.acv=NULL;
 P_SetupPsprites(p);
 demon=P_SpawnMobj(12*SCALE,18*SCALE,0,MT_TROOP);demon->target=p->mo;P_SetMobjState(demon,S_TROO_RUN1);
}
void DC_Grid(const unsigned char *g){memcpy(grid,g,sizeof grid);}
void DC_Pose(double x,double y,double z,double yaw,double pp,int health,int fire){
 player_t *p=&players[0];P_UnsetThingPosition(p->mo);
 p->mo->x=x*SCALE;p->mo->y=y*SCALE;p->mo->z=z*SCALE;p->mo->momx=p->mo->momy=p->mo->momz=0;
 double a=fmod(yaw+90,360);if(a<0)a+=360;p->mo->angle=a/360*4294967296.0;
 P_SetThingPosition(p->mo);p->mo->health=p->health=health;p->cmd.buttons=fire?BT_ATTACK:0;pitch=fmax(-80,fmin(80,pp));
}
void DC_Tick(void){
 player_t *p=&players[0];
 if(weaponsOnly&&(weaponinfo[p->readyweapon].ammo==am_noammo||p->ammo[weaponinfo[p->readyweapon].ammo]<=0))p->cmd.buttons=0;
 P_MovePsprites(p);if(!weaponsOnly)P_RunThinkers();leveltime++;
}
void DC_InitWeapons(void){DC_Init();weaponsOnly=1;players[0].weaponowned[wp_shotgun]=true;players[0].ammo[am_shell]=50;players[0].maxammo[am_shell]=50;}
void DC_SelectWeapon(int weapon){if(weaponsOnly&&(weapon==wp_pistol||weapon==wp_shotgun)&&players[0].ammo[weaponinfo[weapon].ammo]>0&&players[0].readyweapon!=weapon)players[0].pendingweapon=weapon;}
int DC_Shots(double *out){int n=shotCount;memcpy(out,shots,n*sizeof(double));shotCount=0;return n;}
int DC_WeaponStatus(double *out){DC_Snapshot(out);out[9]=players[0].readyweapon;out[10]=players[0].ammo[am_clip];out[11]=players[0].ammo[am_shell];out[1]=weaponinfo[players[0].readyweapon].ammo==am_noammo?0:players[0].ammo[weaponinfo[players[0].readyweapon].ammo];return 12;}
void DC_Hit(int damage){if(demon&&demon->health>0)P_DamageMobj(demon,players[0].mo,players[0].mo,damage);}
int DC_Snapshot(double *out){
 player_t *p=&players[0];pspdef_t *w=&p->psprites[ps_weapon];pspdef_t *f=&p->psprites[ps_flash];
 int n=0;out[n++]=p->health;out[n++]=p->ammo[am_clip];out[n++]=w->state?w->state->sprite:-1;out[n++]=w->state?w->state->frame&32767:0;
 out[n++]=w->sx/(double)FRACUNIT;out[n++]=w->sy/(double)FRACUNIT;out[n++]=f->state?f->state->sprite:-1;out[n++]=f->state?f->state->frame&32767:0;out[n++]=leveltime;out[n++]=demon->health;
 for(thinker_t *th=thinkercap.next;th!=&thinkercap&&n<1000;th=th->next){
  if(th->function.acp1!=(actionf_p1)P_MobjThinker)continue;
  mobj_t *m=(mobj_t*)th;
  out[n++]=blocks(m->x);out[n++]=blocks(m->y);out[n++]=blocks(m->z);out[n++]=m->sprite;out[n++]=m->frame&32767;out[n++]=m->health;out[n++]=m->type;out[n++]=m->angle;
 }
 return n;
}
int DC_Blocks(int *out){int n=eventCount;memcpy(out,events,n*sizeof(int));eventCount=0;return n;}
/* No Doom framebuffer is presented. Minecraft renders exported actor/weapon states. */
void DG_Init(void){} void DG_DrawFrame(void){} void DG_SleepMs(uint32_t ms){} uint32_t DG_GetTicksMs(void){return 0;}
int DG_GetKey(int *p,unsigned char *k){return 0;} void DG_SetWindowTitle(const char *s){}

void DC_SetAmmo(int bullets,int shells){if(!weaponsOnly)return;players[0].ammo[am_clip]=bullets<0?0:bullets>200?200:bullets;players[0].ammo[am_shell]=shells<0?0:shells>50?50:shells;}
