import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogIpTest {
    // todo: make a test that ads a lo filter and ensures that the loopback ip isn't included

    @Test fun testLogIp() {
        LogIp.logAllIPAddresses()
    }

    @Test fun testGetAddresses() {
        val addresses = LogIp.getAddresses(excludeInterfaces = emptyList())
        assertTrue(addresses.contains("127.0.0.1"))
    }

    @Test fun getInterfaces() {
        val interfaces = LogIp.getInterfaces()
        assertTrue(interfaces.isNotEmpty())
    }

    @Test fun getInterfaceAddressMap() {
        val map = LogIp.getInterfaceAddressMap(excludeInterfaces = listOf("lo"))
        assertTrue(map.containsKey("lo").not())
    }
}
