#!/usr/bin/env python3
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, sys
'''스토어 그림: 스크린샷 6장 (1080 × 1920) · 그래픽 이미지 (1024 × 500, 한국어 · 영어) → docs/store/

    git clone --depth 1 -b screenshots-android <저장소> shots
    python3 scripts/build_fonts.py   # .cache 에 전체 글꼴
    python3 scripts/build_store.py shots
'''
SHOTS = sys.argv[1] if len(sys.argv) > 1 else 'shots'
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
F=ROOT+'/.cache/NotoSerifKR-variable.ttf'; L=ROOT+'/.cache/Lora-variable.ttf'
OUT=ROOT+'/docs/store'
PAPER=(243,238,226); INK=(46,42,34); SOFT=(107,100,86); MOSS=(95,122,58)
def font(path,size,w=500):
    f=ImageFont.truetype(path,size)
    try: f.set_variation_by_axes([w])
    except Exception: pass
    return f
def rounded(im,r):
    m=Image.new('L',im.size,0); ImageDraw.Draw(m).rounded_rectangle([0,0,im.size[0]-1,im.size[1]-1],r,fill=255)
    o=Image.new('RGBA',im.size); o.paste(im,(0,0),m); return o
def phone(shot,w):
    im=Image.open(SHOTS+'/'+shot+'.png').convert('RGB'); h=int(im.height*w/im.width); im=im.resize((w,h),Image.LANCZOS)
    return rounded(im,int(w*0.07))
def shadow(canvas,box,r):
    sh=Image.new('RGBA',canvas.size,(0,0,0,0)); ImageDraw.Draw(sh).rounded_rectangle(box,r,fill=(46,42,34,60)); sh=sh.filter(ImageFilter.GaussianBlur(24)); canvas.alpha_composite(sh)
# 스크린샷 6장 (1080 × 1920)
shots=[('g03_home','남은 날을 조용히 세는 정원'),('g02_meet','세상에 하나뿐인 돌, 하루'),('g36_care','기쁨도 슬픔도 한 줄에 실어 보내요'),
       ('g49_record_month','그 달의 별자리에서 피어나는 마음'),('g26_breath_in','하루를 여는 숨, 하루를 마무리하는 명상'),('g54_birthday_eve','가족의 돌과 함께, 밤에는 별빛 아래')]
for i,(s,cap) in enumerate(shots,1):
    W,H=1080,1920; c=Image.new('RGBA',(W,H),PAPER+(255,)); d=ImageDraw.Draw(c)
    f=font(F,62,600); tw=d.textlength(cap,font=f)
    if tw>W-120: f=font(F,52,600); tw=d.textlength(cap,font=f)
    d.text(((W-tw)/2,120),cap,font=f,fill=INK)
    pw=760; p=phone(s,pw); x=(W-pw)//2; y=300
    shadow(c,[x,y+16,x+pw,y+16+p.height],int(pw*0.07)); c.alpha_composite(p,(x,y))
    c.convert('RGB').save(f'{OUT}/screenshot-{i}-{s.split("_",1)[1]}.png')
# 그래픽 이미지 1024 × 500 (한국어 · 영어)
def feature(title,sub,name,tfont,sfont):
    W,H=1024,500; c=Image.new('RGBA',(W,H),PAPER+(255,)); d=ImageDraw.Draw(c)
    # 오른쪽: 이번 달의 정원 (꽃 무늬) 잘라 둥글게
    # 오른쪽: 이번 달의 정원, 낮 (꽃) 과 밤 (별) 두 장이 살짝 겹쳐
    n=Image.open(SHOTS+'/g50_record_month_night.png').convert('RGB').crop((40,660,1040,1660)).resize((330,330),Image.LANCZOS)
    g=Image.open(SHOTS+'/g49_record_month.png').convert('RGB').crop((40,660,1040,1660)).resize((330,330),Image.LANCZOS)
    shadow(c,[W-330-20,30+10,W-20,360+10],32); c.alpha_composite(rounded(n,32),(W-330-20,30))
    shadow(c,[W-330-150,140+10,W-150,470+10],32); c.alpha_composite(rounded(g,32),(W-330-150,140))
    d.text((56,150),title,font=tfont,fill=INK); d.text((60,265),sub,font=sfont,fill=SOFT)
    c.convert('RGB').save(f'{OUT}/{name}')
feature('하루의 정원','남은 날을 조용히 세고,\n오늘의 마음을 한 줄로','feature-ko.png',font(F,84,600),font(F,30,500))
feature('Carpe Diem','Count the days left, quietly.\nLet today go in a single line.','feature-en.png',font(L,84,600),font(L,30,500))
print(sorted(os.listdir(OUT)))
