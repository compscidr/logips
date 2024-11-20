import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.SocketException

object LogIp {
    fun logAllIPAddresses(
        logger: Logger = LoggerFactory.getLogger(LogIp::class.java),
        excludeInterfaces: List<String> = listOf("docker", "virbr", "veth", "tailscale", "dummy", "tun"),
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
                for (inetAddress in addresses) {
                    logger.trace("  IP Address: ${inetAddress.hostAddress}")
                }
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
    }
}
