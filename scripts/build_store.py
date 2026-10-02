#!/usr/bin/env python3
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, sys, shutil
'''스토어 그림, 네 말: 스크린샷 6장 (1080 × 1920) · 그래픽 이미지 (1024 × 500) → docs/store/<말>/

    Actions › Android screenshots 를 store 켜고 실행 (네 말 장면이 store/<말>/ 로)
    git clone --depth 1 -b screenshots-android <저장소> shots
    python3 scripts/build_fonts.py   # .cache 에 전체 글꼴
    python3 scripts/build_store.py shots
'''
SHOTS = sys.argv[1] if len(sys.argv) > 1 else 'shots'
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
C=ROOT+'/.cache/'
OUT=ROOT+'/docs/store'
PAPER=(243,238,226); INK=(46,42,34); SOFT=(107,100,86)
SCENES=['g03_home','g02_meet','g36_care','g49_record_month','g26_breath_in','g54_birthday_eve']
LANGS={
  'ko':dict(loc='ko-KR',serif='NotoSerifKR-variable.ttf',title='하루의 정원',sub='남은 날을 조용히 세고,\n오늘의 마음을 한 줄로',
    caps=['남은 날을 조용히 세는 정원','세상에 하나뿐인 돌, 하루','기쁨도 슬픔도 한 줄에 실어 보내요','그 달의 별자리에서 피어나는 마음','하루를 여는 숨, 하루를 마무리하는 명상','가족의 돌과 함께, 밤에는 별빛 아래']),
  'en':dict(loc='en-US',serif='Lora-variable.ttf',title='Carpe Diem',sub='Count the days left, quietly.\nLet today go in a single line.',
    caps=['A garden that quietly counts your days','Haru, a stone like no other','Joy or sorrow, let it go in one line','Feelings bloom along the month’s stars','A breath to open the day, a calm to close it','Beside your family’s stones, under the stars']),
  'ja':dict(loc='ja-JP',serif='NotoSerifJP-variable.ttf',title='ハルの庭',sub='残りの日々を静かに数え、\n今日の気持ちをひとことに',
    caps=['残りの日々を静かに数える庭','世界にひとつだけの石、ハル','うれしさも悲しみも、ひとことにのせて','その月の星座にそって咲く気持ち','一日をひらく呼吸、一日をとじる瞑想','家族の石と一緒に、夜は星あかりの下で']),
  'zh-TW':dict(loc='zh-TW',serif='NotoSerifTC-variable.ttf',title='小日的庭院',sub='靜靜數著剩下的日子，\n把今天的心情寫成一句話',
    caps=['靜靜數著剩下日子的庭院','世上獨一無二的石頭，小日','喜悅或悲傷，都寫進一句話裡放下','心情沿著當月的星座綻放','開啟一天的呼吸，結束一天的冥想','和家人的石頭一起，夜裡在星光下']),
}
def font(name,size,w=500):
    f=ImageFont.truetype(C+name,size)
    try: f.set_variation_by_axes([w])
    except Exception: pass
    return f
def rounded(im,r):
    m=Image.new('L',im.size,0); ImageDraw.Draw(m).rounded_rectangle([0,0,im.size[0]-1,im.size[1]-1],r,fill=255)
    o=Image.new('RGBA',im.size); o.paste(im,(0,0),m); return o
def shadow(canvas,box,r):
    sh=Image.new('RGBA',canvas.size,(0,0,0,0)); ImageDraw.Draw(sh).rounded_rectangle(box,r,fill=(46,42,34,60)); sh=sh.filter(ImageFilter.GaussianBlur(24)); canvas.alpha_composite(sh)
def fit(d,text,name,size,w,maxw):
    f=font(name,size,w)
    while d.textlength(text,font=f)>maxw and size>30: size-=2; f=font(name,size,w)
    return f
for lang,L in LANGS.items():
    src=f'{SHOTS}/store/{L["loc"]}'; dst=f'{OUT}/{lang}'
    shutil.rmtree(dst,ignore_errors=True); os.makedirs(dst)
    # 스크린샷 6장: 위에 한 줄, 아래 폰 화면
    for i,(s,cap) in enumerate(zip(SCENES,L['caps']),1):
        W,H=1080,1920; c=Image.new('RGBA',(W,H),PAPER+(255,)); d=ImageDraw.Draw(c)
        f=fit(d,cap,L['serif'],62,600,W-120); tw=d.textlength(cap,font=f)
        d.text(((W-tw)/2,120),cap,font=f,fill=INK)
        pw=760; im=Image.open(f'{src}/{s}.png').convert('RGB'); im=im.resize((pw,int(im.height*pw/im.width)),Image.LANCZOS); p=rounded(im,int(pw*0.07))
        x=(W-pw)//2; y=300
        shadow(c,[x,y+16,x+pw,y+16+p.height],int(pw*0.07)); c.alpha_composite(p,(x,y))
        c.convert('RGB').save(f'{dst}/screenshot-{i}-{s.split("_",1)[1]}.png')
    # 그래픽 이미지: 왼쪽 아이콘 · 이름 · 한 줄, 오른쪽 그 말의 정원 (하루 · 나무 · 연) 을 창처럼
    W,H=1024,500; c=Image.new('RGBA',(W,H),PAPER+(255,)); d=ImageDraw.Draw(c)
    g=Image.open(f'{src}/g04_home_all.png').convert('RGB').crop((0,1140,1080,1700))
    gw=560; g=g.resize((gw,int(g.height*gw/g.width)),Image.LANCZOS); gx=W-gw-36; gy=(H-g.height)//2
    shadow(c,[gx,gy+12,gx+gw,gy+12+g.height],30); c.alpha_composite(rounded(g,30),(gx,gy))
    ic=Image.open(f'{OUT}/icon-512.png').convert('RGBA').resize((96,96),Image.LANCZOS)
    m=Image.new('L',ic.size,0); ImageDraw.Draw(m).ellipse([0,0,95,95],fill=255); ic.putalpha(m)
    c.alpha_composite(ic,(56,96))
    tf=fit(d,L['title'],L['serif'],66,600,gx-56-40); d.text((56,220),L['title'],font=tf,fill=INK)
    sf=font(L['serif'],25,500)
    for k,line in enumerate(L['sub'].split('\n')):
        while d.textlength(line,font=sf)>gx-56-36: sf=font(L['serif'],sf.size-1,500)
        d.text((58,318+k*40),line,font=sf,fill=SOFT)
    c.convert('RGB').save(f'{dst}/feature.png')
# 예전 한 벌 (한국어 · 영어만) 은 지움
for f in os.listdir(OUT):
    if f.startswith(('screenshot-','feature-')): os.remove(f'{OUT}/{f}')
print({l:sorted(os.listdir(f'{OUT}/{l}')) for l in LANGS})
