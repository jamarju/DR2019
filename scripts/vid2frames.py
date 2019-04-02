import argparse
from pathlib import Path

import cv2
from tqdm import tqdm

parser = argparse.ArgumentParser(description='Convierte vídeo a frames jpg')
parser.add_argument('video', type=Path, help='Video')
args = parser.parse_args()

prefix = args.video.stem
path = args.video.parent

vidcap = cv2.VideoCapture(str(args.video))
video_len = int(vidcap.get(cv2.CAP_PROP_FRAME_COUNT))

with tqdm(total = video_len) as pbar:
    while 1:
        success,image = vidcap.read()
        if not success:
            break
        framepos = vidcap.get(cv2.CAP_PROP_POS_MSEC)
        outname = '%s-%06d.jpg' % (prefix, int(framepos))
        cv2.imwrite(outname, image)     # save frame as JPEG file
        pbar.update(1)
