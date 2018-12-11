# CoinSplasher

## how to run
we need a proxy, because modern browsers like chromium will block direct calls against binance.com
### local
docker run --name my-custom-nginx-proxy -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro -p 9090:80 nginx

`sbt dev`
goto http://localhost:8080

## build for production
`fullOptJS::webpack`

## credits
### shadaj / slinky
many thanks to https://github.com/shadaj/create-react-scala-app.g8

which helped a lot setting this whole project up
### HODLBOT
as a source of inspiration
