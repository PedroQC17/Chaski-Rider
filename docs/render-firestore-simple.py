from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

root = Path(__file__).parent
image = Image.new('RGB', (2200, 1560), 'white')
draw = ImageDraw.Draw(image)
def font(size, bold=False):
    return ImageFont.truetype('C:/Windows/Fonts/' + ('arialbd.ttf' if bold else 'arial.ttf'), size)
def text(x, y, value, size=25, bold=False):
    draw.text((x,y), value, font=font(size,bold), fill='black')
def table(x,y,w,title,rows):
    header = 65
    row_height = 42
    height = header + len(rows)*row_height
    draw.rectangle((x,y,x+w,y+height), fill='white', outline='black', width=3)
    text(x+16,y+18,title,29,True)
    draw.line((x,y+header,x+w,y+header),fill='black',width=3)
    split=x+w-155
    draw.line((split,y+header,split,y+height),fill='black',width=2)
    for i,(field,kind) in enumerate(rows):
        yy=y+header+i*row_height
        if i:draw.line((x,yy,x+w,yy),fill='black',width=1)
        text(x+14,yy+8,field,23)
        text(split+12,yy+8,kind,22)
def arrow(points, label, pos):
    draw.line(points,fill='black',width=3)
    a,b=points[-2:]
    dx,dy=b[0]-a[0],b[1]-a[1]
    length=(dx*dx+dy*dy)**.5
    ux,uy=dx/length,dy/length
    draw.polygon([b,(b[0]-ux*16+uy*7,b[1]-uy*16-ux*7),(b[0]-ux*16-uy*7,b[1]-uy*16+ux*7)],fill='black')
    text(*pos,label,22,True)

table(65,90,555,'riderUnique/{key}',[
    ('uid','string')])
table(800,50,600,'riders/{uid}',[
    ('id (= uid)','string'),('name / lastName','string'),('email / dni / phone','string'),
    ('status / vehicleType','string'),('currentStep','integer'),('isEnabled / isAvailable','boolean'),
    ('termsAccepted / phoneVerified','boolean'),('rejectionReason','string'),('bankInfo','map'),
    ('documentReviews','map'),('submissionVersion','integer'),('profilePhotoPath','string'),
    ('dniFrontUrl / dniBackUrl','string'),('driverLicenseUrl / soatUrl','string'),
    ('bankStatementUrl','string'),('updatedAt / submittedAt','timestamp'),
    ('termsAcceptedAt','timestamp'),('profilePhotoCreatedAt','timestamp')])
table(1570,90,565,'riderOfferDemos/{uid}',[
    ('revision','integer'),('pickedUp','boolean'),('offer','map | null'),
    ('batch','map | null'),('decisions','map'),('requests','map')])
arrow([(620,175),(800,175)],'uid → uid',(635,135))
arrow([(1570,245),(1400,245)],'uid = uid',(1420,205))

table(65,1030,555,'registrationReviews/{version}',[
    ('submissionVersion','integer'),('status / rejectionReason','string'),('documentReviews','map'),
    ('isAvailable','boolean'),('reviewedAt','timestamp'),('reviewedBy','string')])
table(800,1030,600,'supportTickets/{ticketId}',[
    ('topic / category','string'),('body / status','string'),('createdAt','timestamp'),('order','map | null'),
    ('order.orderId','string'),('order.problemId','string | null'),('order.merchantName','string'),
    ('order.pickupAddress','string'),('order.deliveryAddress','string'),('order.pickedUp','boolean')])
table(1570,1030,565,'messages/{messageId}',[
    ('sender','string'),('body','string'),('createdAt','timestamp')])
arrow([(950,871),(950,940),(340,940),(340,1030)],'1 → 0..N',(395,900))
arrow([(1100,871),(1100,1030)],'1 → 0..N',(1120,947))
arrow([(1400,1140),(1570,1140)],'1 → 0..N',(1417,1100))
image.save(root/'modelo-firestore-chaski-simple.png')
