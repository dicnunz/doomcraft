"""Extract Freedoom patch sprites to PNG with only Python standard library."""
from pathlib import Path
import struct,zlib,re,json
root=Path(__file__).resolve().parents[1]
data=(root/'vendor/freedoom-0.13.0/freedoom2.wad').read_bytes()
magic,count,offset=struct.unpack_from('<4sii',data);assert magic==b'IWAD'
lumps={}
for i in range(count):
 start,size,name=struct.unpack_from('<ii8s',data,offset+i*16);lumps[name.rstrip(b'\0').decode()]=data[start:start+size]
pal=lumps['PLAYPAL'][:768]
names=re.search(r'char \*sprnames\[\] = \{(.*?)\};',(root/'vendor/doomgeneric-master/doomgeneric/info.c').read_text(),re.S)[1]
names=re.findall(r'"(.*?)"',names)
res=root/'src/main/resources/assets/doomcraft';out=res/'textures/sprites';out.mkdir(parents=True,exist_ok=True)
meta=[]
def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d))
for sid,name in enumerate(names):
 for frame in range(29):
  letter=chr(65+frame);key=next((k for k in [name+letter+'0',name+letter+'1'] if k in lumps),None)
  if key is None:key=next((k for k in lumps if k.startswith(name+letter)),None)
  if not key:continue
  d=lumps[key];w,h,left,top=struct.unpack_from('<hhhh',d)
  if not(0<w<1024 and 0<h<1024):continue
  pixels=bytearray(w*h*4)
  for x in range(w):
   p=struct.unpack_from('<I',d,8+x*4)[0];prev=-1
   while d[p]!=255:
    y,n=d[p:p+2];p+=3
    if y<=prev:y+=prev
    prev=y
    for j in range(n):
     if y+j<h:
      c=d[p+j]*3;dst=((y+j)*w+x)*4;pixels[dst:dst+4]=pal[c:c+3]+b'\xff'
    p+=n+1
  raw=b''.join(b'\0'+pixels[y*w*4:(y+1)*w*4] for y in range(h))
  png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')
  (out/f'{sid}_{frame}.png').write_bytes(png);meta.append((sid,frame,w,h,left,top))
java='package dev.doomcraft;\npublic final class SpriteData {\n public static final int[][] DATA={\n'+',\n'.join('  {'+','.join(map(str,row))+'}' for row in meta)+'\n };\n}\n'
(root/'src/main/java/dev/doomcraft/SpriteData.java').write_text(java)
(res/'models/item').mkdir(parents=True,exist_ok=True)
sid=names.index('PISG')
(res/'textures/item').mkdir(parents=True,exist_ok=True)
(res/'textures/item/pistol.png').write_bytes((out/f'{sid}_0.png').read_bytes())
(res/'models/item/pistol.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'doomcraft:item/pistol'}}))
print(f'Extracted {len(meta)} Freedoom sprite frames; original states select their frame IDs.')
