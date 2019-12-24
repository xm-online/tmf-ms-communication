## Build
```bash
docker build -t smpp-emulator ./src/main/docker/smpp-emulator
```
## Run
```bash
docker run -p 2775:2775 <image-id>
```
