#!/usr/bin/env python3
"""스토어 소개 영상 (가로 1920 × 1080, 30fps) 을 녹화한 장면들로 이어 붙인다.

녹화: Actions › Android screenshots 를 video 켜고 실행 → screenshots-android(-dev) 의 video/<말>/vNN_*.mp4
사용: python3 scripts/build_video.py <녹화 폴더 (video/ 가 든 곳)> <내보낼 폴더> [말 …]
한지 종이 바탕 왼쪽에 폰 화면, 오른쪽에 장면마다 한 줄. 끝은 앱 이름 한 장.
"""
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONTS = os.path.join(ROOT, "android/app/src/main/res/font")
FONT = {"ko-KR": "notoserifkr_medium.ttf", "en-US": "lora_medium.ttf", "ja-JP": "notoserifjp_medium.ttf", "zh-TW": "notoseriftc_medium.ttf"}
PAPER = (246, 241, 230)
INK = (51, 40, 31)
SOFT = (125, 110, 95)
W, H, FPS = 1920, 1080, 30
# 폰 화면: 상태 표시줄 · 아래 막대를 잘라 낸 1080 × 2250 을 높이 PHONE_H 로
CROP_TOP, CROP_H = 90, 2250
PHONE_H = 1000
PHONE_W = round(1080 * PHONE_H / CROP_H) // 2 * 2
PHONE_X, PHONE_Y = 330, (H - PHONE_H) // 2
FADE = 0.6

# (녹화, 시작 초, 길이 초, 한 줄)
SCENES = [
    ("v01_meet", 0.8, 4.6, {"ko-KR": "세상에 하나뿐인 돌, 하루", "en-US": "Haru, a stone like no other", "ja-JP": "世界にひとつだけの石、ハル", "zh-TW": "世上獨一無二的石頭，小日"}),
    ("v03_home", 0.3, 4.6, {"ko-KR": "남은 날을 조용히 세는 정원", "en-US": "A garden that quietly\ncounts your days", "ja-JP": "残りの日々を静かに数える庭", "zh-TW": "靜靜數著剩下日子的庭院"}),
    ("v06_ripple", 0.5, 4.6, {"ko-KR": "숨을 따라가기만 하면 돼요", "en-US": "Just follow the breath", "ja-JP": "息についていくだけ", "zh-TW": "跟著呼吸走就好"}),
    ("v07_gaze", 0.0, 4.2, {"ko-KR": "화톳불 곁에서, 돌멍하기", "en-US": "Stone gazing by the fire", "ja-JP": "たき火のそばで、石ながめ", "zh-TW": "在營火旁，看石發呆"}),
    ("v08_fireflies", 0.8, 4.6, {"ko-KR": "밤에는 달과 반딧불", "en-US": "The moon and fireflies at night", "ja-JP": "夜は月とほたる", "zh-TW": "夜裡有月亮和螢火蟲"}),
    ("v09_snow", 0.3, 4.6, {"ko-KR": "절기와 계절을 따라 바뀌는 정원", "en-US": "A garden that turns\nwith the seasons", "ja-JP": "季節とともに変わる庭", "zh-TW": "隨季節變化的庭院"}),
]
END = {"ko-KR": ("하루의 정원", "오늘을 소중히."), "en-US": ("Carpe Diem", "Days Left Garden"), "ja-JP": ("ハルの庭", "今日を大切に。"), "zh-TW": ("小日的庭院", "珍惜今天。")}
END_SECONDS = 3.4


def font(loc, size):
    return ImageFont.truetype(os.path.join(FONTS, FONT[loc]), size)


def caption_layer(loc, text, path):
    """바탕 + 폰 자리의 둥근 창 (투명) + 오른쪽 한 줄."""
    img = Image.new("RGBA", (W, H), PAPER + (255,))
    d = ImageDraw.Draw(img)
    r = 44
    box = (PHONE_X, PHONE_Y, PHONE_X + PHONE_W, PHONE_Y + PHONE_H)
    hole = Image.new("L", (W, H), 0)
    ImageDraw.Draw(hole).rounded_rectangle(box, r, fill=255)
    img.putalpha(Image.eval(hole, lambda v: 255 - v))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((box[0] - 3, box[1] - 3, box[2] + 3, box[3] + 3), r + 3, outline=INK, width=4)
    f = font(loc, 64 if loc != "en-US" else 70)
    cx = (PHONE_X + PHONE_W + W) // 2 + 20
    lines = text.split("\n")
    lh = int(f.size * 1.45)
    y = H // 2 - lh * len(lines) // 2
    for ln in lines:
        d.text((cx, y + lh // 2), ln, font=f, fill=INK, anchor="mm")
        y += lh
    img.save(path)


def end_card(loc, path):
    img = Image.new("RGB", (W, H), PAPER)
    d = ImageDraw.Draw(img)
    name, sub = END[loc]
    d.text((W // 2, H // 2 - 40), name, font=font(loc, 110), fill=INK, anchor="mm")
    d.text((W // 2, H // 2 + 70), sub, font=font(loc, 48), fill=SOFT, anchor="mm")
    img.save(path)


def run(cmd):
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def build(src, out, loc):
    clips = os.path.join(src, "video", loc)
    tmp = tempfile.mkdtemp()
    parts = []
    for i, (name, start, dur, text) in enumerate(SCENES):
        cap = os.path.join(tmp, f"cap{i}.png")
        caption_layer(loc, text[loc], cap)
        part = os.path.join(tmp, f"p{i}.mp4")
        # 느린 에뮬레이터 녹화 (초당 몇 장) 를 30fps 로 부드럽게 섞고, 종이 바탕 · 둥근 창 아래에 놓음
        run(["ffmpeg", "-y", "-ss", str(start), "-t", str(dur), "-i", os.path.join(clips, name + ".mp4"), "-loop", "1", "-t", str(dur), "-i", cap,
             "-filter_complex",
             f"color=c=0x{PAPER[0]:02x}{PAPER[1]:02x}{PAPER[2]:02x}:s={W}x{H}:r={FPS}:d={dur}[bg];"
             f"[0:v]crop=1080:{CROP_H}:0:{CROP_TOP},scale={PHONE_W}:{PHONE_H},fps={FPS},minterpolate=fps={FPS}:mi_mode=blend,setpts=PTS-STARTPTS[ph];"
             f"[bg][ph]overlay={PHONE_X}:{PHONE_Y}:shortest=1[a];[a][1:v]overlay=0:0,format=yuv420p",
             "-r", str(FPS), "-c:v", "libx264", "-crf", "18", "-preset", "medium", "-an", part])
        parts.append((part, dur))
    endp = os.path.join(tmp, "end.png")
    end_card(loc, endp)
    endv = os.path.join(tmp, "end.mp4")
    run(["ffmpeg", "-y", "-loop", "1", "-t", str(END_SECONDS), "-i", endp, "-vf", f"fps={FPS},format=yuv420p", "-c:v", "libx264", "-crf", "18", "-an", endv])
    parts.append((endv, END_SECONDS))
    # 장면 사이 부드럽게 겹침
    inputs, chain, t, last = [], "", 0.0, "0:v"
    for k, (p, d) in enumerate(parts):
        inputs += ["-i", p]
    t = parts[0][1]
    for k in range(1, len(parts)):
        off = t - FADE
        lab = f"x{k}"
        chain += f"[{last}][{k}:v]xfade=transition=fade:duration={FADE}:offset={off:.3f}[{lab}];"
        last, t = lab, off + parts[k][1]
    final = os.path.join(out, f"carpediem-{loc}.mp4")
    run(["ffmpeg", "-y", *inputs, "-filter_complex", chain + f"[{last}]fade=t=in:st=0:d=0.5,format=yuv420p[v]", "-map", "[v]",
         "-c:v", "libx264", "-crf", "18", "-preset", "slow", "-movflags", "+faststart", "-an", final])
    print(final, f"{t:.1f}s")


def main():
    src, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    for loc in sys.argv[3:] or list(FONT):
        build(src, out, loc)


if __name__ == "__main__":
    main()
