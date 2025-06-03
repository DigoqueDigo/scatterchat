# ScatterChat

## Compile
```
mvn clean compile
```

### Run DHT Node
```
cd src/main/erlang/scatterchat/dht/
./build.sh start
```

### Run Aggregation Server
```
mvn exec:java -Dexec.mainClass=scatterchat.aggrserver.AggrServer -Dexec.args="config/config.json <saID>"
```

### Run Chat Server
```
mvn exec:java -Dexec.mainClass=scatterchat.chatserver.ChatServer -Dexec.args="config/config.json <scID>"
```

### Run Client
```
mvn exec:java -Dexec.mainClass=scatterchat.client.Client -Dexec.args="config/config.json <cliID>"
```
> [!IMPORTANT]  
> Starting too few aggregation servers can lead to problems during aggregation, so I recommend the following setup
> - three DHT nodes
> - three Aggregation Servers
> - three Chat Servers (one per Aggregation Server)
> - two clients
