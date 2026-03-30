# dlms-cosem

A Java library for communicating with DLMS/COSEM smart meters over RS-485 serial and TCP/IP. Built from scratch with full IP ownership — no GPL dependencies.

---

## Features

- **Multi-meter polling** — concurrently read and write multiple meters on one or more RS-485 buses
- **HDLC framing** — full IEC 62056-46 HDLC implementation (SNRM, UA, DISC, I-frames, segmentation)
- **DLMS/COSEM application layer** — GET, SET, ACTION services (IEC 62056-53)
- **COSEM object model** — Register, Profile Generic (load profiles), Clock, Data objects with OBIS addressing
- **Transport abstraction** — RS-485 serial (via jSerialComm) and TCP/IP (WRAPPER mode)
- **Security** — Low authentication (password), High authentication, AES-128-GCM encryption (suite 0)
- **Non-blocking API** — `CompletableFuture`-based, meter pool with per-bus serialized access
- **Data callbacks** — plug in your own exporter to forward data to any central system

---

## Requirements

- Java 11+
- Maven 3.6+
- RS-485 serial adapter (e.g. USB-to-RS485) or TCP-accessible meter

---

## Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.pivotaccess.dlmscosem-cosem</groupId>
    <artifactId>dlms-cosem</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Build from source

```bash
git clone https://gitlab.pepc.rw/smart-meter/dlms-cosem.git
cd dlms-cosem
mvn clean install
```

---

## Quick Start

### Read a single meter

```java
MeterConfig config = MeterConfig.builder()
    .meterId("meter_01")
    .transport(TransportType.RS485)
    .portName("/dev/ttyUSB0")
    .baudRate(9600)
    .hdlcAddress(1)
    .build();

try (CosemClient client = CosemClient.connect(config)) {
    RegisterObject energy = client.getObject(
        ObisCode.of("1.0.1.8.0.255"),
        RegisterObject.class
    );
    System.out.println("Energy import: " + energy.getValue() + " " + energy.getUnit());
}
```

### Read multiple meters concurrently

```java
MeterPool pool = new MeterPool();

pool.addMeter(MeterConfig.builder()
    .meterId("meter_01")
    .portName("/dev/ttyUSB0")
    .hdlcAddress(1)
    .build());

pool.addMeter(MeterConfig.builder()
    .meterId("meter_02")
    .portName("/dev/ttyUSB0")  // same RS-485 bus
    .hdlcAddress(2)
    .build());

// Read all meters — bus access is automatically serialized
Map<String, CompletableFuture<DataObject>> results =
    pool.readAll(CosemAttribute.of("1.0.1.8.0.255", ClassId.REGISTER, 2));

results.forEach((meterId, future) ->
    future.thenAccept(value ->
        System.out.println(meterId + " → " + value)));
```

### Scheduled polling with callback

```java
pool.schedule(
    "meter_01",
    CosemAttribute.of("1.0.1.7.0.255", ClassId.REGISTER, 2),
    Duration.ofSeconds(10),
    (meterId, obis, value, timestamp) -> {
        // Forward to your central system, MQTT broker, database, etc.
        System.out.printf("[%s] %s @ %s = %s%n",
            meterId, obis, timestamp, value);
    }
);
```

### Read load profile (Profile Generic)

```java
ProfileGenericObject profile = client.getObject(
    ObisCode.of("1.0.99.1.0.255"),
    ProfileGenericObject.class
);

List<List<DataObject>> buffer = profile.getBuffer(
    Instant.now().minus(Duration.ofDays(1)),
    Instant.now()
);

buffer.forEach(row -> System.out.println(row));
```

### Set meter clock

```java
ClockObject clock = client.getObject(
    ObisCode.of("0.0.1.0.0.255"),
    ClockObject.class
);
clock.setTime(ZonedDateTime.now());
```

---

## Configuration Reference

### `MeterConfig`

| Field | Type | Description |
|---|---|---|
| `meterId` | `String` | Unique identifier for this meter |
| `transport` | `TransportType` | `RS485` or `TCP` |
| `portName` | `String` | Serial port (e.g. `/dev/ttyUSB0`) or IP address |
| `baudRate` | `int` | Serial baud rate (default: `9600`) |
| `hdlcAddress` | `int` | Meter HDLC server address (1–126) |
| `clientAddress` | `int` | HDLC client address (default: `16`) |
| `authentication` | `AuthLevel` | `NONE`, `LOW`, `HIGH` |
| `password` | `String` | Low-level authentication password |
| `encryptionKey` | `byte[]` | AES-128 encryption key (16 bytes) |
| `timeoutMs` | `int` | Response timeout in ms (default: `10000`) |
| `retries` | `int` | Number of retries on timeout (default: `3`) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Your Application                      │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                    dlms-cosem                            │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │  MeterPool  │  │  CosemClient │  │  COSEM Objects │  │
│  │ (concurrent)│  │  (sessions)  │  │  (OBIS model)  │  │
│  └─────────────┘  └──────────────┘  └────────────────┘  │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │    HDLC     │  │  BER Codec   │  │    Security    │  │
│  │  (framing)  │  │  (ASN.1)     │  │  (AES-128-GCM) │  │
│  └─────────────┘  └──────────────┘  └────────────────┘  │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐                       │
│  │  RS-485     │  │     TCP      │                       │
│  │  Transport  │  │  Transport   │                       │
│  └─────────────┘  └──────────────┘                       │
└──────────────────────────────────────────────────────────┘
         ↕ RS-485                    ↕ TCP/IP
   [M1] [M2] [M3]...           [M4] [M5]...
```

### Package layout

```
com.yourcompany.dlmscosem/
├── transport/         # RS485Transport, TcpTransport, Transport interface
├── hdlc/              # HdlcFrame, HdlcAddress, HdlcSession
├── apdu/              # BER encoder/decoder, AARQ, GET/SET/ACTION APDUs
├── cosem/
│   ├── CosemClient.java
│   ├── CosemAttribute.java
│   ├── ObisCode.java
│   ├── DataObject.java
│   └── objects/       # RegisterObject, ProfileGenericObject, ClockObject
├── security/          # AesGcmSecurity, AuthenticationLevel
├── pool/              # MeterPool, MeterConfig, SerialBusExecutor
└── export/            # DataCallback, MqttExporter, RestExporter
```

---

## Standards

This library implements the following IEC standards:

| Standard | Description |
|---|---|
| IEC 62056-46 | HDLC data link layer |
| IEC 62056-53 | DLMS/COSEM application layer |
| IEC 62056-62 | COSEM interface classes and objects |
| IEC 62056-21 | Mode E serial handshake |
| DLMS Blue Book | COSEM object model (dlms.com) |
| DLMS Green Book | Architecture and security (dlms.com) |

---

## Dependencies

| Library | Version | License | Purpose |
|---|---|---|---|
| [jSerialComm](https://github.com/Fazecast/jSerialComm) | 2.11.0 | Apache 2.0 | RS-485 serial communication |
| [SLF4J](https://www.slf4j.org/) | 2.0.9 | MIT | Logging facade |

> All runtime dependencies are permissively licensed (Apache 2.0 / MIT). There are no GPL dependencies — dlms-cosem can be used in proprietary commercial applications.

---

## Contributing

Contributions are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "feat: add my feature"`
4. Push to your fork: `git push origin feature/my-feature`
5. Open a pull request

### Commit message convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` — new feature
- `fix:` — bug fix
- `chore:` — build, tooling, dependencies
- `docs:` — documentation only
- `test:` — tests only
- `refactor:` — code change with no feature or fix

### Running tests

```bash
mvn test
```

---

## Roadmap

- [x] Project scaffolding
- [ ] RS-485 transport (jSerialComm)
- [ ] HDLC framing layer
- [ ] BER ASN.1 encoder/decoder
- [ ] DLMS GET / SET / ACTION
- [ ] COSEM object model (Register, Clock, Data)
- [ ] MeterPool concurrent polling
- [ ] Profile Generic (load profiles)
- [ ] Security — Low auth, AES-128-GCM
- [ ] TCP/WRAPPER transport
- [ ] Data export callbacks
- [ ] Central system integration example

---

## License

[MIT](LICENSE)