# Mango Reader

Simple self-hosted manga/webtoon/image server and reader written in Java, Spring Boot.

- Supported formats: `.cbz`/`.zip`, `.crz`/`.rar`.
- PWA support for easy access.

Under development.

## Installation

The app is meant to be run as Docker container.
Sample `docker-compose.yml`:
```yaml
services:
  mangoreader:
    image: ghcr.io/sosedik/mangoreader:release
    ports:
      - "0.0.0.0:8080:8080"
    environment:
      - MANGO_INPUT_DIRS=/workspace/library1
    volumes:
      - /path/to/library:/workspace/library1
    restart: unless-stopped
```

## Library structure

```
.
├── Manga 1
│   ├── Volume 1.cbz
│   ├── Volume 2.zip
│   ├── Volume 3.cbr
│   └── Volume 4.rar
└── Manga 2
    └── Ch.1
        ├── Image 1.jpeg
        ├── Image 2.png
        └── Image 3.webp
```

#### Credits

Inspired by [Mango](https://github.com/getmango/Mango).
