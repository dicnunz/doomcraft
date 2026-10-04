/* DoomCraft local prototype, GPL-2.0-or-later. */
#ifndef DC_BRIDGE_H
#define DC_BRIDGE_H
#include "p_local.h"
boolean DC_Position(mobj_t *, fixed_t, fixed_t);
boolean DC_Sight(mobj_t *, mobj_t *);
fixed_t DC_Slope(void);
void DC_LineAttack(mobj_t *, angle_t, fixed_t, fixed_t, int);
void DC_Init(void);
void DC_Grid(const unsigned char *);
void DC_Pose(double,double,double,double,double,int,int);
void DC_Tick(void);
void DC_Hit(int);
int DC_Snapshot(double *);
int DC_Blocks(int *);


void DC_InitWeapons(void);
void DC_SelectWeapon(int weapon);
int DC_Shots(double *out);
int DC_WeaponStatus(double *out);

void DC_SetAmmo(int bullets,int shells);
#endif
