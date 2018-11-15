# CoinSplasher

inspired by HODLBOT

command line tool to rebalance your coins

supports currently only binance accounts

# Build
sbt assembly

# Run
you can provide ENVs for your binance account, or you will be asked on each run

BINANCE_API_KEY

BINANCE_SECRET
## print out trades
`java -jar target/scala-2.12/console-assembly-0.0.1.jar -o`
## execute trades from stdin
`java -jar target/scala-2.12/console-assembly-0.0.1.jar -i`
CTRL+D to exit input mode
## automatic mode
`java -jar target/scala-2.12/console-assembly-0.0.1.jar -a`