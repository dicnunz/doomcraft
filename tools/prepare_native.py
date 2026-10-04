from pathlib import Path
import re, shutil
root=Path(__file__).resolve().parents[1]
src=root/'vendor/doomgeneric-master/doomgeneric'
out=root/'build/doom'
out.mkdir(parents=True,exist_ok=True)
for f in src.iterdir():
 if f.suffix in ('.c','.h'): shutil.copy2(f,out/f.name)
def inject(file, name, code):
 p=out/file;s=p.read_text();pattern=r'(\b'+name+r'\s*\([^;{}]*\)\s*\{)';s,n=re.subn(pattern,lambda m:m[0]+'\n'+code,s,count=1);assert n==1,(file,name);p.write_text(s)
for file in ['p_map.c','p_sight.c']:
 p=out/file;p.write_text('#include "bridge.h"\n'+p.read_text())
inject('p_map.c','P_CheckPosition','    numspechit = 0; ceilingline = NULL; if (!DC_Position(thing, x, y)) return false;')
inject('p_map.c','P_AimLineAttack','    linetarget = NULL; return DC_Slope();')
inject('p_map.c','P_LineAttack','    DC_LineAttack(t1, angle, distance, slope, damage); return;')
inject('p_sight.c','P_CheckSight','    return DC_Sight(t1, t2);')
inject('s_sound.c','S_StartSound','    return; /* Audio not initialized in the simulation-only host. */')
inject('s_sound.c','S_StopSound','    return;')
# Original iterator reads a freed thinker after Z_Free; retain next before freeing.
p=out/'p_tick.c';s=p.read_text().replace('thinker_t*\tcurrentthinker;', 'thinker_t*\tcurrentthinker; thinker_t *nextthinker;').replace('while (currentthinker != &thinkercap)\n    {','while (currentthinker != &thinkercap)\n    {\n        nextthinker = currentthinker->next;').replace('currentthinker = currentthinker->next;', 'currentthinker = nextthinker;');p.write_text(s)
