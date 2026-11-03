The app's icons. `public/` holds the files the app serves, rendered from the SVGs here with
[rsvg-convert](https://gitlab.gnome.org/GNOME/librsvg):

```shell
rsvg-convert -w 192 -h 192 icons/icon.svg -o public/icon-192.png
rsvg-convert -w 512 -h 512 icons/icon.svg -o public/icon-512.png
rsvg-convert -w 512 -h 512 icons/icon-maskable.svg -o public/icon-maskable-512.png
rsvg-convert -w 180 -h 180 icons/icon-maskable.svg -o public/apple-touch-icon.png
```
