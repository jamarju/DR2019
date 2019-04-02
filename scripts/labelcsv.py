import argparse
import pandas as pd
import cv2
from pathlib import Path
import numpy as np

parser = argparse.ArgumentParser(
    description='Etiquetador de vídeos',
    epilog='[esc] sale, [a/z] avanza/retrocede, [ratón izdo] etiqueta, [espacio] desetiqueta')
parser.add_argument('dir', type=Path, help='Directorio con frames (jpg)')
parser.add_argument('csv', type=Path, help='Archivo .csv')
parser.add_argument('label', type=str, help='Etiqueta')
args = parser.parse_args()

label_x = args.label + '_x'
label_y = args.label + '_y'
mx, my = 0, 0

cursor_color = (0xaa, 0)


def done():
    global df, args
    cv2.destroyAllWindows()
    df.to_csv(args.csv, index_label='file')
    print(f"{df.shape} (filas, columnas)")
    exit(0)


def load_image(step=0):
    global image, idx
    idx += step
    cv2.setTrackbarPos('timeline', 'image', idx)
    if idx >= len(fl):
        done()
    image = cv2.imread(str(fl[idx]), cv2.IMREAD_COLOR)
    cv2.imshow('image', image)
    update_pointer()


def update_pointer():
    global mx, my
    clone = image.copy()

    # Add labels
    if fl[idx].name in df.index:
        for label in labels:
            cx, cy = (label + suffix for suffix in ('_x', '_y'))
            if cx in df.columns:
                assert cy in df.columns
                lx, ly = df.at[fl[idx].name, cx], df.at[fl[idx].name, cy]
                if not np.isnan(lx) and not np.isnan(ly):
                    lx, ly = int(lx), int(ly)
                    cv2.circle(clone, (lx, ly), 5, (0, 255, 0), -1)
                    cv2.putText(clone, label, (lx + 10, ly), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2, cv2.LINE_AA)

    # Add mouse pointer
    cv2.circle(clone, (mx, my), 7, (0, 128, 255), -1)
    cv2.imshow('image', clone)


def on_mouse(event, x, y, flags, param):
    # grab references to the global variables
    global mx, my

    if event == cv2.EVENT_MOUSEMOVE:
        mx, my = x, y
        update_pointer()

    if event == cv2.EVENT_LBUTTONDOWN:
        df.at[fl[idx].name, label_x] = x
        df.at[fl[idx].name, label_y] = y
        load_image(+1)


def on_timeline_change(new_idx):
    global idx
    idx = new_idx
    load_image(0)


if args.csv.exists():
    df = pd.read_csv(args.csv, index_col=0)
    labels = set( col.rstrip('_x') for col in df.columns[::2] )
else:
    df = pd.DataFrame()
    labels = set()

labels.add(args.label)
print(labels)

fl = sorted(args.dir.glob('*.jpg'))
video_len = len(fl)
idx = 0

df = df.reindex([f.name for f in fl])

cv2.namedWindow("image")
cv2.setMouseCallback("image", on_mouse)
cv2.createTrackbar("timeline", "image", 0, video_len - 1, on_timeline_change)

load_image()
while True:
    key = cv2.waitKey(10)
    if key == 27:
        done()
    if key == ord('z') and idx > 0:
        load_image(-1)
    if key == ord('a'):
        load_image(+1)
    if key == ord(' '):
        df.at[fl[idx].name, label_x] = None
        df.at[fl[idx].name, label_y] = None
        load_image()
