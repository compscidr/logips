# logips
[![codecov](https://codecov.io/gh/compscidr/logips/graph/badge.svg?token=Rga4WAHMGu)](https://codecov.io/gh/compscidr/logips)

Simple lib to log ip addresses in kotlin. Allows filtering of interfaces
by name or up/down status.

Can also just retrieve a list of interfaces with the same filtering applied.



## Usage:
```
dependencies {
  implementation("com.jasonernst.logips:logips")
}
```

```kotlin
private val logger = LoggerFactory.getLogger("SomeLogger")
// log the ips addresses with the provided logger, excluding the loopback interface
LogIp.logAllIpAddresses(logger, "lo")

// log the ips with the default logger, excluding the default interfaces (vlans, docker, etc)
LogIp.logAllIpAddresses()

// get all the interfaces except the loopback interface
val interfaces = LogIp.getInterfaces(excludeInterfaces = listOf("lo"))
// do something with the interface... 

// this just prints the interfaces the same as the logAllIpAddresses function above, but
// it could be used if you wanted to do something with each ip address
val interfaceNameIpMap = LogIp.getInterfaceNameAddressMap(excludeInterfaces = listOf("lo"))
for (interfaceName in interfaceNameMap.keys) {
    logger.debug("Interface $interfaceName")
    val ipAddresses = interfaceNameMap[interfaceName]
    if (ipAddresses == null) {
        logger.debug("  No ips")
    } else {
        for (ipAddress in ipAddresses) {
            logger.debug("  IP $ipAddress")
        }
    }
}
```
