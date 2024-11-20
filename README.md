# logips
[![codecov](https://codecov.io/gh/compscidr/logips/graph/badge.svg?token=Rga4WAHMGu)](https://codecov.io/gh/compscidr/logips)

Simple lib to log ip addresses in kotlin

## Usage:
```
dependencies {
  implementation("com.jasonernst.logips:logips")
}
```

```kotlin
private val logger = LoggerFactory.getLogger("SomeLogger")
// log the ips addresses with the provided logger, excluding the loopback interface
LogIp.logAllIPAddresses(logger, "lo")

// log the ips with the default logger, excluding the default interfaces (vlans, docker, etc)
LogIp.logAllIpAddresses()
```
