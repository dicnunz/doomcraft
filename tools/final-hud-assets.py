from pathlib import Path
import struct,zlib,random
r=Path(__file__).resolve().parents[1]
rng=random.Random(35);w,h=640,72
p=[(65+(n:=rng.randrange(-5,6)),67+n,60+n) for _ in range(w*h)]
def line(x1,y1,x2,y2,c):
 for y in range(y1,y2+1):
  for x in range(x1,x2+1):p[y*w+x]=c
line(0,0,639,1,(15,16,13));line(0,2,639,2,(145,143,126));line(0,3,639,3,(96,98,87));line(0,71,639,71,(15,16,13))
for x in (0,84,175,283,350,434,514,637):
 line(x,4,x,70,(21,23,19));line(x+1,4,x+1,70,(113,113,96))
 if x+5>=640:continue
 for y in (6,67):line(x+3,y-1,x+5,y+1,(22,23,20));line(x+3,y-1,x+3,y-1,(154,151,129))
def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d))
raw=b''.join(b'\0'+bytes(v for rgb in p[y*w:(y+1)*w] for v in rgb) for y in range(h))
(r/'src/main/resources/assets/doomcraft/textures/hud/survival_console.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
