import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.SocketException

object LogIp {
    val defaultExcludeInterfaces = listOf("docker", "virbr", "veth", "tailscale", "dummy", "tun", "lo")

    fun logAllIPAddresses(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
    ) {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger.error("No network interfaces found")
                return
            }
            for (networkInterface in interfaces) {
                var excluded = false
                for (excludeInterface in excludeInterfaces) {
                    if (networkInterface.displayName.contains(excludeInterface)) {
                        excluded = true
                        break
                    }
                }
                if (excluded) {
                    continue
                }
                if (networkInterface.isUp.not()) {
                    continue
                }
                val addresses = networkInterface.inetAddresses.toList()
                if (addresses.isEmpty()) {
                    continue
                }
                logger.trace("Network Interface: ${networkInterface.name}")
                if (addresses.isEmpty()) {
                    logger.trace("  No IP Addresses")
                    continue
                }
                for (inetAddress in addresses) {
                    logger.trace("  IP Address: ${inetAddress.hostAddress}")
                }
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
    }

    fun getAddresses(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): List<String> {
        val addresses = mutableListOf<String>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger.warn("No network interfaces found")
                return addresses
            }
            for (networkInterface in interfaces) {
                var excluded = false
                for (excludeInterface in excludeInterfaces) {
                    if (networkInterface.displayName.contains(excludeInterface)) {
                        excluded = true
                        break
                    }
                }
                if (excluded) {
                    continue
                }
                if (networkInterface.isUp.not() && excludeDownInterfaces) {
                    continue
                }
                val inetAddresses = networkInterface.inetAddresses.toList()
                if (inetAddresses.isEmpty()) {
                    continue
                }
                for (inetAddress in inetAddresses) {
                    addresses.add(inetAddress.hostAddress)
                }
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
        return addresses
    }

    fun getInterfaces(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): List<String> {
        val interfaceList = mutableListOf<String>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger.warn("No network interfaces found")
                return interfaceList
            }
            for (networkInterface in interfaces) {
                var excluded = false
                for (excludeInterface in excludeInterfaces) {
                    if (networkInterface.displayName.contains(excludeInterface)) {
                        excluded = true
                        break
                    }
                }
                if (excluded) {
                    continue
                }
                if (networkInterface.isUp.not() && excludeDownInterfaces) {
                    continue
                }
                interfaceList.add(networkInterface.name)
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
        return interfaceList
    }

    fun getInterfaceAddressMap(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): Map<String, List<String>> {
        val interfaceAddressMap = mutableMapOf<String, List<String>>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger.warn("No network interfaces found")
                return interfaceAddressMap
            }
            for (networkInterface in interfaces) {
                var excluded = false
                for (excludeInterface in excludeInterfaces) {
                    if (networkInterface.displayName.contains(excludeInterface)) {
                        excluded = true
                        break
                    }
                }
                if (excluded) {
                    continue
                }
                if (networkInterface.isUp.not() && excludeDownInterfaces) {
                    continue
                }
                val inetAddresses = networkInterface.inetAddresses.toList()
                interfaceAddressMap[networkInterface.name] = inetAddresses.map { it.hostAddress }
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
        return interfaceAddressMap
    }
}
