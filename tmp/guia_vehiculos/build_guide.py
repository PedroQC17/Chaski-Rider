from pathlib import Path
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle
import pypdfium2 as pdfium

ROOT=Path(__file__).parent
OUT=ROOT.parents[1]/'output'/'pdf'
OUT.mkdir(parents=True,exist_ok=True)
src=Path(r'C:\Users\Angel\.codex\generated_images\01a1829d-268f-78d2-ab9f-7ee62931602d\exec-9f3980d9-fd63-4d61-8460-71c417171c92.png')
im=Image.open(src).convert('L').convert('RGB')
bands=[(0,226),(226,405),(405,565),(565,785),(785,1006),(1006,1208),(1208,1448)]
for i,(a,b) in enumerate(bands):
    im.crop((0,a,im.width,b)).save(ROOT/f'categoria_{i+1}.png')
font=Path('C:/Windows/Fonts')
pdfmetrics.registerFont(TTFont('Arial',str(font/'arial.ttf')))
pdfmetrics.registerFont(TTFont('ArialBold',str(font/'arialbd.ttf')))
W,H=A4
path=OUT/'guia_visual_vehiculos_bosque_del_itata.pdf'
c=canvas.Canvas(str(path),pagesize=A4)
c.setTitle('Guía visual de categorías de vehículos Bosque del Itata')
c.setAuthor('')
style=ParagraphStyle('body',fontName='Arial',fontSize=10.5,leading=15,textColor='#333333')
def text(s,x,y,width=495,size=10.5,bold=False,color='#333333'):
    st=ParagraphStyle('p',parent=style,fontName='ArialBold' if bold else 'Arial',fontSize=size,leading=size*1.4,textColor=color)
    p=Paragraph(s,st); _,h=p.wrap(width,600); p.drawOn(c,x,y-h); return h

data=[
('Motos y cuatrimotos','Motos, motonetas y cuatrimotos.','Vehículos motorizados livianos de dos o cuatro ruedas: motos, motonetas y quads o cuatrimotos.','Moto · Motoneta · Cuatrimoto','Identificar el tipo de vehículo liviano.'),
('Autos y camionetas','Autos y camionetas.','Vehículos livianos de pasajeros: automóviles, jeeps, SUV y camionetas simples sin remolque.','Automóvil · SUV o jeep · Camioneta','Comprobar que el vehículo no lleve remolque.'),
('Autos y camionetas con remolque','Autos con remolque y camionetas con remolque.','Automóviles o camionetas que arrastran un carro de arrastre o remolque liviano.','Automóvil con remolque · Camioneta con remolque','Reconocer el carro de arrastre unido al vehículo.'),
('Buses de dos ejes y casas rodantes','Buses de dos ejes y casas rodantes.','Buses de transporte de pasajeros de 2 ejes y casas rodantes o motorhomes.','Bus de dos ejes · Casa rodante','Contar los ejes del bus e identificar las casas rodantes.'),
('Camiones y maquinaria de dos ejes','Camiones de dos ejes y maquinaria agrícola o de construcción de dos ejes.','Camiones medianos o pesados de 2 ejes, así como maquinaria agrícola y de construcción de 2 ejes.','Camión · Tractor agrícola · Cargador de construcción','Verificar que el vehículo o la maquinaria tenga dos ejes.'),
('Buses de más de dos ejes','Buses de más de dos ejes.','Buses de pasajeros de gran tamaño que posean 3 o más ejes.','Bus de tres ejes','Distinguirlos de los buses de dos ejes de la categoría 4.'),
('Camiones multieje y sobredimensión','Camiones, maquinaria agrícola y maquinaria de construcción de más de dos ejes; vehículos con sobredimensión.','Camiones multieje de 3 o más ejes, maquinaria pesada de más de 2 ejes y vehículos especiales con sobredimensión.','Camión multieje · Maquinaria agrícola multieje · Transporte sobredimensionado','Revisar el número de ejes y la condición de sobredimensión.'),
]

def header(page):
    text('BOSQUE DEL ITATA',48,H-34,size=9,bold=True,color='#555555')
    text('Guía visual de categorías de vehículos',48,H-55,size=23,bold=True,color='#111111')
    text('Referencia para validación manual',48,H-94,size=11,color='#555555')
    c.setFillColorRGB(.4,.4,.4); c.setFont('Arial',8)
    c.drawString(48,29,'Bosque del Itata  |  Guía de validación manual')
    c.drawRightString(W-48,29,f'{page} / 4')

def category(idx,top,large=False):
    title,cl,crit,caption,key=data[idx]
    c.setFillColorRGB(.12,.12,.12); c.roundRect(48,top-28,30,28,4,fill=1,stroke=0)
    c.setFillColorRGB(1,1,1); c.setFont('ArialBold',16); c.drawCentredString(63,top-20,str(idx+1))
    text(f'Categoría {idx+1}',90,top-1,size=9,bold=True,color='#666666')
    text(title,90,top-16,size=15,bold=True,color='#111111')
    pic=Image.open(ROOT/f'categoria_{idx+1}.png')
    iw=499; ih=iw*pic.height/pic.width
    pic_top=top-55
    c.drawImage(ImageReader(pic),48,pic_top-ih,width=iw,height=ih)
    y=pic_top-ih-9
    y-=text(caption,48,y,size=9,color='#555555')+15
    y-=text('<b>Clase de vehículo</b>  '+cl,48,y,size=10.5)+10
    y-=text('<b>Criterio de validación</b>  '+crit,48,y,size=10.5)+14
    if large:
        c.setFillColorRGB(.95,.95,.95); c.roundRect(48,y-64,499,64,5,fill=1,stroke=0)
        text('Qué revisar',62,y-12,size=10,bold=True)
        text(key,62,y-31,width=470,size=10.5)

for pg,pair in enumerate([(0,1),(2,3),(4,5),(6,)],1):
    header(pg)
    if pg==1:
        text('Identifique el tipo de vehículo y revise la presencia de remolque, el número de ejes o la sobredimensión según cada categoría.',48,H-124,size=10.5)
    for j,idx in enumerate(pair):
        category(idx, H-180-j*300 if pg==1 else H-150-j*315,large=pg==4)
    if pg==4:
        y=H-530
        text('Puntos de revisión',48,y,size=16,bold=True,color='#111111')
        notes=[('Tipo de vehículo','Distinguir vehículos livianos, buses, camiones y maquinaria.'),('Remolque','En autos y camionetas, comprobar si arrastran un remolque liviano.'),('Ejes','Comparar dos ejes frente a tres o más en las categorías que lo requieren.'),('Sobredimensión','Revisar esta condición para identificar los vehículos especiales de la categoría 7.')]
        y-=34
        for a,b in notes:
            y-=text(f'<b>{a}</b>  {b}',48,y,size=10.5)+14
        text('Fuente de clasificación  categorias_vehiculos_bosque_del_itata.pdf',48,92,size=8,color='#666666')
        text('Ilustraciones de referencia para apoyar la identificación visual.',48,77,size=8,color='#666666')
    c.showPage()
c.save()
doc=pdfium.PdfDocument(str(path))
for i in range(len(doc)):
    doc[i].render(scale=1.5).to_pil().save(ROOT/f'final_{i+1}.png')
print(path)
print('Pages:',len(doc))
