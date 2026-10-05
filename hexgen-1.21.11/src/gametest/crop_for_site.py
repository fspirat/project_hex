# Обрезает снимки из .screenshots (их кладёт workflow screenshots.yml) по окну мода и сохраняет webp для страницы сайта.
# Запуск из корня репозитория: python3 hexgen-1.21.11/src/gametest/crop_for_site.py
import glob,os,re
from PIL import Image
SRC='.screenshots'; DST='sites/fspirat.ru/fstweak/img'
os.makedirs(DST,exist_ok=True)
def panel_box(im):
    # Фон окон мода — тёмно-зелёный (~14,20,12): ищем строки и столбцы, где его много.
    g=im.convert('RGB'); W,H=g.size; px=g.load()
    def ok(c): return c[0]<24 and c[1]<30 and c[2]<22 and c[1]>=c[0]
    rows=[y for y in range(0,H,2) if sum(ok(px[x,y]) for x in range(0,W,4))>40]
    cols=[x for x in range(0,W,2) if sum(ok(px[x,y]) for y in range(0,H,4))>30]
    return min(cols),min(rows),max(cols),max(rows)
def fit(box,W,H,pad,ratio=16/10):
    x0,y0,x1,y1=box; x0-=pad;y0-=pad;x1+=pad;y1+=pad
    w=x1-x0;h=y1-y0
    if w/h<ratio: d=int(h*ratio-w);x0-=d//2;x1+=d-d//2
    else: d=int(w/ratio-h);y0-=d//2;y1+=d-d//2
    return max(0,x0),max(0,y0),min(W,x1),min(H,y1)
for f in sorted(glob.glob(SRC+'/*.png')):
    name=re.sub(r'^\d+_','',os.path.basename(f))[:-4]
    im=Image.open(f).convert('RGB'); W,H=im.size
    if name=='chat': box=(0,H-250,720,H)
    elif name=='inventory': box=fit((770,320,1150,770),W,H,24)
    else:
        x0,y0,x1,y1=panel_box(im); box=fit((x0,y0-70,x1,y1+10),W,H,50)
    c=im.crop(box)
    if c.width>1600: c=c.resize((1600,round(c.height*1600/c.width)),Image.LANCZOS)
    c.save(f'{DST}/{name}.webp',quality=88,method=6)
    print(name,box,c.size,os.path.getsize(f'{DST}/{name}.webp'))
