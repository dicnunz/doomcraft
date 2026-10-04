"""Freedoom's licensed status bar, digits, face patches and weapon sounds."""
exec((__import__('pathlib').Path(__file__).parent/'extract_assets.py').read_text())
import wave,subprocess
hud=res/'textures/hud';hud.mkdir(exist_ok=True)
metadata=[]
for key in lumps:
 if not(key in ['STBAR','STARMS','STTPRC'] or key.startswith(('STTNUM','STYSNUM','STGNUM','STFST','STFOUCH','STFDEAD','STFKILL'))):continue
 d=lumps[key];w,h,left,top=struct.unpack_from('<hhhh',d)
 pixels=bytearray(w*h*4)
 for x in range(w):
  p=struct.unpack_from('<I',d,8+x*4)[0]
  while d[p]!=255:
   y,n=d[p:p+2];p+=3
   for j in range(n):
    if y+j<h:
     c=d[p+j]*3;dst=((y+j)*w+x)*4;pixels[dst:dst+4]=pal[c:c+3]+b'\xff'
   p+=n+1
 raw=b''.join(b'\0'+pixels[y*w*4:(y+1)*w*4] for y in range(h))
 (hud/(key.lower()+'.png')).write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
 metadata.append(f'  m.put("{key.lower()}",new int[]{{{w},{h}}});')
(root/'src/main/java/dev/doomcraft/HudPatches.java').write_text('package dev.doomcraft;\nimport java.util.*;\nfinal class HudPatches {static final Map<String,int[]> DATA=build();static Map<String,int[]> build(){Map<String,int[]> m=new HashMap<>();\n'+'\n'.join(metadata)+'\nreturn m;}}\n')
(res/'textures/item/shotgun.png').write_bytes((out/'1_0.png').read_bytes())
(res/'models/item/shotgun.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'doomcraft:item/shotgun'}}))
(res/'sounds').mkdir(exist_ok=True)
for key,name in [('DSPISTOL','pistol'),('DSSHOTGN','shotgun')]:
 d=lumps[key];fmt,rate,n=struct.unpack_from('<HHI',d);assert fmt==3
 wav=root/'build'/f'{name}.wav'
 with wave.open(str(wav),'wb') as f:f.setnchannels(1);f.setsampwidth(1);f.setframerate(rate);f.writeframes(d[8:8+n])
 subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-ac','2','-c:a','vorbis','-strict','experimental',str(res/'sounds'/f'{name}.ogg')],check=True)
(res/'sounds.json').write_text(json.dumps({n:{'sounds':[f'doomcraft:{n}']} for n in ['pistol','shotgun']}))
print('Freedoom HUD and sounds extracted.')
