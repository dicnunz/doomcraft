"""Original marine skin and tiny ammo icons; no commercial Doom art used."""
from pathlib import Path
import struct,zlib,json,random
r=Path(__file__).resolve().parents[1]/'src/main/resources';a=r/'assets/doomcraft'
def png(path,w,h,pixels):
 def c(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d))
 raw=b''.join(b'\0'+bytes(pixels[y*w*4:(y+1)*w*4]) for y in range(h))
 path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'\x89PNG\r\n\x1a\n'+c(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+c(b'IDAT',zlib.compress(raw))+c(b'IEND',b''))
p=bytearray(64*64*4)
def rect(x,y,w,h,color,noise=0):
 for yy in range(y,y+h):
  for xx in range(x,x+w):
   shade=((xx*17+yy*13)%5-2)*noise;p[(yy*64+xx)*4:(yy*64+xx)*4+4]=bytes([max(0,min(255,c+shade)) for c in color]+[255])
# UV base surfaces, lit olive plates, charcoal undersuit and boots.
rect(0,0,32,16,(43,54,39),2);rect(0,16,16,16,(51,61,38),2);rect(16,16,24,16,(65,83,41),2);rect(40,16,16,16,(65,82,45),2)
rect(16,48,16,16,(51,61,38),2);rect(32,48,16,16,(65,82,45),2)
# Steel helmet face: graphite outline, blue-gray visor, segmented respirator.
rect(8,8,8,8,(129,139,130),1);rect(9,9,6,3,(25,46,47));rect(9,9,6,1,(102,151,146));rect(10,12,4,3,(42,47,42));rect(11,12,1,3,(164,169,148));rect(13,12,1,3,(164,169,148))
# Helmet side ridge/back pack.
rect(0,8,8,2,(150,158,141));rect(16,8,8,2,(107,119,105));rect(24,8,8,8,(72,86,65),1)
# Chest plate and belt, small amber rank markers.
rect(20,20,8,7,(95,120,54),2);rect(21,21,6,1,(139,153,83));rect(23,22,2,4,(62,82,39));rect(20,27,8,2,(31,36,28));rect(23,27,2,2,(165,156,100));rect(20,29,8,3,(72,62,41),1)
rect(32,20,8,7,(59,73,42),1);rect(33,21,6,5,(41,47,34));rect(34,22,1,3,(146,151,117));rect(37,22,1,3,(146,151,117));rect(32,27,8,2,(31,36,28))
for x,y in [(44,20),(36,52)]:
 rect(x,y,4,3,(113,134,69),1);rect(x,y+3,4,5,(109,79,52),2);rect(x,y+8,4,4,(37,41,32),1);rect(x+1,y,2,1,(203,174,73))
for x,y in [(4,20),(20,52)]:
 rect(x,y,4,7,(73,81,47),2);rect(x,y+5,4,2,(108,115,70));rect(x,y+8,4,4,(29,34,28),1)
png(a/'textures/entity/marine.png',64,64,p)
for name,color in [('bullet_pack',(180,151,71)),('shell_pack',(171,49,34))]:
 px=bytearray(16*16*4)
 for y in range(3,14):
  for x in range(2,14):
   if x%4!=1:
    c=(218,197,125) if y<5 else ((55,56,47) if y>11 else color)
    px[(y*16+x)*4:(y*16+x)*4+4]=bytes(c)+b'\xff'
 png(a/f'textures/item/{name}.png',16,16,px)
 (a/'models/item'/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'doomcraft:item/{name}'}}))
recipes=r/'data/doomcraft/recipe';recipes.mkdir(parents=True,exist_ok=True)
for name,ingredients in [('bullet_pack',['iron_nugget','gunpowder']),('shell_pack',['paper','iron_nugget','gunpowder']),('pistol',['iron_ingot','iron_ingot','flint']),('shotgun',['iron_ingot','iron_ingot','iron_ingot','oak_planks'])]:
 (recipes/f'{name}.json').write_text(json.dumps({'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':[{'item':'minecraft:'+i} for i in ingredients],'result':{'id':'doomcraft:'+name,'count':1}}))
(a/'lang').mkdir(exist_ok=True);(a/'lang/en_us.json').write_text(json.dumps({'item.doomcraft.pistol':'Freedoom Pistol','item.doomcraft.shotgun':'Freedoom Shotgun','item.doomcraft.bullet_pack':'Bullet Pack (20)','item.doomcraft.shell_pack':'Shell Pack (8)'},indent=2))
