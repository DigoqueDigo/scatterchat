# Scatterchat

## Compile
```
mvn clean compile
```

### Run DHT Nodes
```
cd src/main/erlang/scatterchat/dht/
./build.sh start
```

### Run Aggregation Servers
```
mvn exec:java -Dexec.mainClass=scatterchat.aggrserver.AggrServer -Dexec.args="config/config.json <saID>"
```

### Run Chat Servers
```
mvn exec:java -Dexec.mainClass=scatterchat.chatserver.ChatServer -Dexec.args="config/config.json <scID>"
```

### Run Client
```
mvn exec:java -Dexec.mainClass=scatterchat.client.Client -Dexec.args="config/config.json <cliID>"
```