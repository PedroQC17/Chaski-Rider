from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import json

root=Path(__file__).parent
inventory=json.loads((root/'firestore-inventory.json').read_text())
for item in inventory['collections']:
    item['fields']={k:v for k,v in item['fields'].items() if not k.startswith(('requests.','decisions.'))}
(root/'firestore-inventory.json').write_text(json.dumps(inventory,indent=2,ensure_ascii=False),encoding='utf-8')
im=Image.new('RGB',(2000,1550),'#f4f6fa'); d=ImageDraw.Draw(im)
def font(n,b=False): return ImageFont.truetype('C:/Windows/Fonts/'+('arialbd.ttf' if b else 'arial.ttf'),n)
def txt(x,y,s,n=24,c='#26354b',b=False): d.text((x,y),s,font=font(n,b),fill=c)
def box(x,y,w,h,title,subtitle,lines,color='#196d70'):
    d.rounded_rectangle((x,y,x+w,y+h),radius=18,fill='white',outline=color,width=3)
    d.rounded_rectangle((x,y,x+w,y+60),radius=18,fill=color)
    d.rectangle((x,y+30,x+w,y+60),fill=color)
    txt(x+20,y+14,title,27,'white',True); txt(x+20,y+78,subtitle,20,color,True)
    for i,line in enumerate(lines): txt(x+20,y+118+i*34,line,23)
def arrow(points,label,lx,ly,color='#196d70',dashed=False):
    for a,b in zip(points,points[1:]):
        if not dashed:d.line([a,b],fill=color,width=4)
        else:
            dist=((b[0]-a[0])**2+(b[1]-a[1])**2)**.5
            for start in range(0,int(dist),20):
                end=min(start+11,dist)
                d.line([(a[0]+(b[0]-a[0])*start/dist,a[1]+(b[1]-a[1])*start/dist),(a[0]+(b[0]-a[0])*end/dist,a[1]+(b[1]-a[1])*end/dist)],fill=color,width=4)
    a,b=points[-2:]; dx=b[0]-a[0];dy=b[1]-a[1];length=(dx*dx+dy*dy)**.5;ux=dx/length;uy=dy/length
    d.polygon([b,(b[0]-ux*16+uy*8,b[1]-uy*16-ux*8),(b[0]-ux*16-uy*8,b[1]-uy*16+ux*8)],fill=color)
    txt(lx,ly,label,20,color,True)

txt(55,35,'CHASKI RIDER | Modelo documental de Firestore',43,b=True)
txt(55,100,'Proyecto: chaski-rider · Consulta de solo lectura: 12/10/2026',25)
txt(55,141,'Líneas continuas: vínculo lógico / jerarquía. Discontinuas: estructura definida en código, sin documentos observados.',22)
box(55,210,490,245,'Firebase Authentication','Servicio externo a Firestore',[
 'Identificador: uid','Acceso con Google y correo/contraseña','uid vincula la identidad con el perfil'], '#4357a0')
box(700,210,575,515,'riders/{uid}','Colección raíz · 5 documentos observados',[
 'ID del documento = uid de Authentication','id, name, lastName, email, dni, phone','status, currentStep, vehicleType','isEnabled, isAvailable, rejectionReason','bankInfo: mapa de datos bancarios','dniFrontUrl, dniBackUrl, soatUrl, ...','profilePhotoPath: ruta de Storage','updatedAt, submittedAt, termsAcceptedAt','En código: documentReviews (mapa)', 'En código: submissionVersion'], '#196d70')
arrow([(545,315),(700,315)],'1 → 0..1',565,278,'#4357a0')
box(1445,210,500,260,'riderUnique/{key}','Colección raíz · 10 documentos observados',[
 'key = tipo + hash SHA-256 del valor','uid: identificador del propietario','Reservas de DNI y teléfono únicos','Vínculo: riderUnique.uid → riders/{uid}'])
arrow([(1445,405),(1370,405),(1370,440),(1275,440)],'N → 1',1292,363)
box(1445,535,500,285,'riderOfferDemos/{uid}','Colección raíz · 2 documentos observados',[
 'revision, pickedUp','offer, batch: mapa o null','decisions, requests: mapas','Mismo uid que el perfil del repartidor','Simulación anterior del backend'])
arrow([(1445,670),(1345,670),(1275,670)],'0..1 ↔ 1',1300,630)
box(55,560,490,290,'Firebase Storage','Servicio externo a Firestore',[
 'riders/{uid}/documents/{tipo}/{archivo}','riders/{uid}/profilePhoto/{archivo}','Guarda fotos y documentos binarios','El perfil guarda rutas, no el archivo'], '#4357a0')
arrow([(700,565),(625,565),(625,680),(545,680)],'Ruta',565,514,'#4357a0')
box(55,970,490,310,'registrationReviews/{version}','Subcolección bajo riders/{uid}',[
 'Definida en el código del backend','Sin documentos observados','submissionVersion, status','documentReviews, rejectionReason','reviewedAt, reviewedBy'], '#ad751c')
arrow([(820,725),(820,900),(300,900),(300,970)],'1 → 0..N revisiones',390,863,'#ad751c',True)
box(700,970,575,310,'supportTickets/{ticketId}','Subcolección bajo riders/{uid}',[
 'En Android; sin documentos observados','topic, category, body, status, createdAt','order: mapa opcional (datos del pedido)','orderId, merchantName, direcciones','pickedUp, problemId'], '#ad751c')
arrow([(990,725),(990,970)],'1 → 0..N tickets',1008,863,'#ad751c',True)
box(1445,970,500,310,'messages/{messageId}','Subcolección bajo cada supportTicket',[
 'En Android; sin documentos observados','sender: RIDER o SUPPORT','body: contenido del mensaje','createdAt: timestamp','Texto inicial: body del ticket padre'], '#ad751c')
arrow([(1275,1100),(1445,1100)],'1 → 0..N',1293,1057,'#ad751c',True)
d.rounded_rectangle((55,1300,1945,1495),radius=18,fill='#e5ebf4')
txt(80,1320,'LÍMITE DEL MODELO',25,b=True)
txt(80,1360,'Los pedidos actuales se simulan con demo_orders_lima.json y se guardan en Room (demo_orders.db), por usuario.',24)
txt(80,1400,'order dentro del ticket es una copia de contexto: orderId no apunta a una colección orders en el modelo actual.',24)
txt(80,1440,'Las cardinalidades describen el diseño. Firestore no impone claves foráneas. Mapas embebidos ≠ colecciones.',23)
im.save(root/'modelo-firestore-chaski.png')
