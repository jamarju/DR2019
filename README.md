# DR2019

## controlador-arduino

Controla robot Arduino a través de órdenes por el puerto USB/serie.

Notas:

- Cambiar pines en `motores.h`

## android/RobotGrabador

Notas:

- Mando Bluetooth para Android: [Aliexpress](https://www.aliexpress.com/item/Wireless-Bluetooth-Gamepad-For-Android-Remote-Controller-For-Iphone-Joystick-Game-Pad-Control-For-3D-VR/32694725809.html)

## android/Conductor

Notas:

- Android Studio -> Tools -> SDK Manager -> Instalar NDK (probado con versión 19.1).

## scripts

Scripts de ayuda.

- Instalar [Anaconda](https://www.anaconda.com/distribution/)
- Crear entorno + dependencias

```
conda create --name dr2019
conda install py-opencv pandas numpy tqdm
```

### vid2frames.py

Convierte .mp4 a frames jpg:

```
$ python3 scripts/vid2frames.py -h
usage: vid2frames.py [-h] video

Convierte vídeo a frames jpg

positional arguments:
  video       Video

optional arguments:
  -h, --help  show this help message and exit
```

### labelcsv.py

Etiqueta los frames en un .csv para tareas de regresión y clasificación multietiqueta.

```
$ python scripts/labelcsv.py -h
usage: labelcsv.py [-h] dir csv label

Etiquetador de vídeos

positional arguments:
  dir         Directorio con frames (jpg)
  csv         Archivo .csv
  label       Etiqueta

optional arguments:
  -h, --help  show this help message and exit

[esc] sale, [a/z] avanza/retrocede, [ratón izdo] etiqueta, [espacio]
desetiqueta
```