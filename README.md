# AI4Seniors Spring boot app

## Pre Req

**Installtions**:
Make sure Java 17 or 21 is installed in your local,
Refer: https://dev.to/realnamehidden1_61/how-to-install-java-jdk-17-on-windows-10-5b0d

1. copy the api key json file under /opt/env/{gcv-key.json}
2. Once cloned the repo, start the server at 8080

## Test

to check the gcp connection:
`http://localhost:8080/test`

cURL to test:
`
curl -X POST http://localhost:8080/ocr \
  -H "Content-Type: multipart/form-data" \
  -F "file=@your-image.png"
`