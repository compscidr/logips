import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.NetworkInterface
import java.net.SocketException

object LogIp {
    val defaultExcludeInterfaces = listOf("docker", "virbr", "veth", "tailscale", "dummy", "tun", "lo")

    /**
     * Helper function to check if a network interface matches a given pattern.
     * Checks both the displayName and name properties of the interface.
     *
     * @param networkInterface The network interface to check
     * @param pattern The pattern to match against
     * @return true if either displayName or name contains the pattern
     */
    private fun matchesInterfacePattern(
        networkInterface: NetworkInterface,
        pattern: String,
    ): Boolean = networkInterface.displayName.contains(pattern) || networkInterface.name.contains(pattern)

    fun logAllIpAddresses(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = false,
    ) {
        val interfaces = getInterfaces(logger, excludeInterfaces, excludeDownInterfaces)
        for (networkInterface in interfaces) {
            logger.debug("Interface ${networkInterface.name} (${networkInterface.displayName})")
            val inetAddresses = networkInterface.inetAddresses.toList()
            if (inetAddresses.isEmpty()) {
                logger.debug("  No ips")
            } else {
                for (inetAddress in inetAddresses) {
                    logger.debug("  IP ${inetAddress.hostAddress}")
                }
            }
        }
    }

    fun getAddresses(
        logger: Logger? = null,
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
        logger: Logger? = null,
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
        logger: Logger? = null,
        excludeInterfaces: List<String> = defaultExcludeInterfaces,
        excludeDownInterfaces: Boolean = true,
    ): List<NetworkInterface> {
        val interfaceList = mutableListOf<NetworkInterface>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger?.warn("No network interfaces found")
                return interfaceList
            }
            for (networkInterface in interfaces) {
                var excluded = false
                for (excludeInterface in excludeInterfaces) {
                    if (matchesInterfacePattern(networkInterface, excludeInterface)) {
                        logger?.debug(
                            "Excluding interface ${networkInterface.name} (${networkInterface.displayName}) - matches exclusion pattern '$excludeInterface'",
                        )
                        excluded = true
                        break
                    }
                }
                if (excluded) {
                    continue
                }
                if (networkInterface.isUp.not() && excludeDownInterfaces) {
                    logger?.debug("Excluding interface ${networkInterface.name} (${networkInterface.displayName}) - interface is down")
                    continue
                }
                logger?.debug("Including interface ${networkInterface.name} (${networkInterface.displayName})")
                interfaceList.add(networkInterface)
            }
        } catch (e: SocketException) {
            logger?.error("Error getting network interfaces", e)
        }
        return interfaceList
    }

    fun getInterfaceNameAddressMap(
        logger: Logger? = null,
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
        logger: Logger? = null,
        includeInterfaces: List<String>,
        excludeDownInterfaces: Boolean = true,
    ): List<NetworkInterface> {
        val interfaceList = mutableListOf<NetworkInterface>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger?.warn("No network interfaces found")
                return interfaceList
            }
            for (networkInterface in interfaces) {
                var matched = false
                for (includeInterface in includeInterfaces) {
                    if (matchesInterfacePattern(networkInterface, includeInterface)) {
                        logger?.debug(
                            "Interface ${networkInterface.name} (${networkInterface.displayName}) matches inclusion pattern '$includeInterface'",
                        )
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    logger?.debug(
                        "Skipping interface ${networkInterface.name} (${networkInterface.displayName}) - no matching inclusion pattern",
                    )
                    continue
                }
                if (networkInterface.isUp.not() && excludeDownInterfaces) {
                    logger?.debug(
                        "Excluding matched interface ${networkInterface.name} (${networkInterface.displayName}) - interface is down",
                    )
                    continue
                }
                logger?.debug("Including matched interface ${networkInterface.name} (${networkInterface.displayName})")
                interfaceList.add(networkInterface)
            }
        } catch (e: SocketException) {
            logger?.error("Error getting network interfaces", e)
        }
        return interfaceList
    }

    fun getInterfaceNamesMatching(
        logger: Logger? = null,
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
        logger: Logger? = null,
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
        logger: Logger? = null,
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
