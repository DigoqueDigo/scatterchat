# ScatterChat

A fully decentralized chat system designed for resilience, scalability, and eventual consistency. It combines modern distributed systems concepts such as causal consistency via vector clocks, operation-based CRDTs for conflict-free state synchronization, consistent hashing to balance load across distributed nodes, and multiple communication paradigms including both direct and aggregated messaging.

![demo](https://github.com/user-attachments/assets/966e619e-bc05-40e7-b3ea-a8df93eecb23)


> [!NOTE]
> Each component operates independently and discovers others using a shared configuration file (`config/config.json`), for optimal stability and performance, run multiple instances of each component as outlined below.

---

## 📦 Dependencies

- **Rebar3**
- **Erlang/OTP 27** 
- **Java 21** or higher  
- **Maven** (version 3.8+ recommended)

---

## ⚙️ Compile

```bash
mvn clean compile
```

---

## 🚀 Run Components

If you're using Sway as your window manager, simply run `./present main` to launch all services at once. Otherwise, you'll need to start each component individually as demonstrated below.

---

### 🔗 Run DHT Node

```
./present dht
```

The binding addresses are automatically assigned, so they are not specified in the configuration file.

---

### 🧠 Run Aggregation Server

```
./present sa <saID>
```

Replace `<saID>` with the Aggregation Server ID (e.g., sa1).

---

### 💬 Run Chat Server

```
./present sc <scID>
```

Replace `<scID>` with the Chat Server ID (e.g., sc1).

---

### 👤 Run Client

```
./present client <cliID>
```

Replace `<cliID>` with the Client ID (e.g., cli1).

---

## ⚙️ Recommended Setup

To ensure proper system operation and avoid aggregation issues, we recommend the following setup:

- 3 DHT Nodes
- 3 Aggregation Servers
- 3 Chat Servers (one per Aggregation Server)
- 2 Clients
