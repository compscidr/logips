import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.NetworkInterface
import java.net.SocketException

object LogIp {
    val defaultExcludeInterfaces = listOf("docker", "virbr", "veth", "tailscale", "dummy", "tun", "lo")

    fun logAllIpAddresses(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = false,
    ) {
        val interfaceNameMap = getInterfaceNameAddressMap(logger, excludeInterfaces, excludeDownInterfaces)
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
    }

    fun getAddresses(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): List<String> {
        val addresses = ArrayList<String>()
        val interfaces = getInterfaces(logger, excludeInterfaces, excludeDownInterfaces)
        interfaces.forEach {
            val inetAddresses = it.inetAddresses.toList()
            for (inetAddress in inetAddresses) {
                addresses.add(inetAddress.hostAddress)
            }
        }
        return addresses
    }

    fun getInterfaceNames(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): List<String> {
        val interfaces = getInterfaces(logger, excludeInterfaces, excludeDownInterfaces)
        val interfaceNames = mutableListOf<String>()
        interfaces.forEach {
            interfaceNames.add(it.name)
        }
        return interfaceNames
    }

    fun getInterfaces(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): List<NetworkInterface> {
        val interfaceList = mutableListOf<NetworkInterface>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
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
                interfaceList.add(networkInterface)
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
        return interfaceList
    }

    fun getInterfaceNameAddressMap(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): Map<String, List<String>> {
        val interfaceAddressMap = mutableMapOf<String, List<String>>()
        val interfaces = getInterfaces(logger, excludeInterfaces, excludeDownInterfaces)
        interfaces.forEach { netIf ->
            val inetAddresses = netIf.inetAddresses.toList()
            interfaceAddressMap[netIf.name] = inetAddresses.map { it.hostAddress }
        }
        return interfaceAddressMap
    }

    fun getInterfacesMatching(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        includeInterfaces: List<String>,
        excludeDownInterfaces: Boolean = true,
    ): List<NetworkInterface> {
        val interfaceList = mutableListOf<NetworkInterface>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger.warn("No network interfaces found")
                return interfaceList
            }
            for (networkInterface in interfaces) {
                var matched = false
                for (includeInterface in includeInterfaces) {
                    if (networkInterface.displayName.contains(includeInterface)) {
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    continue
                }
                if (networkInterface.isUp.not() && excludeDownInterfaces) {
                    continue
                }
                interfaceList.add(networkInterface)
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
        return interfaceList
    }

    fun getInterfaceNamesMatching(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        includeInterfaces: List<String>,
        excludeDownInterfaces: Boolean = true,
    ): List<String> {
        val interfaces = getInterfacesMatching(logger, includeInterfaces, excludeDownInterfaces)
        val interfaceNames = mutableListOf<String>()
        interfaces.forEach {
            interfaceNames.add(it.name)
        }
        return interfaceNames
    }

    fun getAddressesMatching(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        includeInterfaces: List<String>,
        excludeDownInterfaces: Boolean = true,
    ): List<String> {
        val addresses = ArrayList<String>()
        val interfaces = getInterfacesMatching(logger, includeInterfaces, excludeDownInterfaces)
        interfaces.forEach {
            val inetAddresses = it.inetAddresses.toList()
            for (inetAddress in inetAddresses) {
                addresses.add(inetAddress.hostAddress)
            }
        }
        return addresses
    }

    fun getInterfaceNameAddressMapMatching(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        includeInterfaces: List<String>,
        excludeDownInterfaces: Boolean = true,
    ): Map<String, List<String>> {
        val interfaceAddressMap = mutableMapOf<String, List<String>>()
        val interfaces = getInterfacesMatching(logger, includeInterfaces, excludeDownInterfaces)
        interfaces.forEach { netIf ->
            val inetAddresses = netIf.inetAddresses.toList()
            interfaceAddressMap[netIf.name] = inetAddresses.map { it.hostAddress }
        }
        return interfaceAddressMap
    }
}
