import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogIpTest {
    // todo: make a test that ads a lo filter and ensures that the loopback ip isn't included

    @Test fun testLogIp() {
        LogIp.logAllIpAddresses()
    }

    @Test fun testGetAddresses() {
        val addresses = LogIp.getAddresses(excludeInterfaces = emptyList())
        assertTrue(addresses.contains("127.0.0.1"))
    }

    @Test fun getInterfaces() {
        val interfaces = LogIp.getInterfaces()
        assertTrue(interfaces.isNotEmpty())
    }

    @Test fun getInterfaceNames() {
        val interfaceNames = LogIp.getInterfaceNames(excludeInterfaces = emptyList())
        assertTrue(interfaceNames.contains("lo"))
    }

    @Test fun getInterfaceAddressMap() {
        val map = LogIp.getInterfaceNameAddressMap(excludeInterfaces = listOf("lo"))
        assertFalse(map.containsKey("lo"))
    }

    @Test fun getInterfacesMatching() {
        val interfaces = LogIp.getInterfacesMatching(includeInterfaces = listOf("lo"))
        assertTrue(interfaces.isNotEmpty())
        // All returned interfaces should contain "lo" in their display name
        interfaces.forEach { netInterface ->
            assertTrue(netInterface.displayName.contains("lo"))
        }
    }

    @Test fun getInterfaceNamesMatching() {
        val interfaceNames = LogIp.getInterfaceNamesMatching(includeInterfaces = listOf("lo"))
        assertTrue(interfaceNames.contains("lo"))
    }

    @Test fun getAddressesMatching() {
        val addresses = LogIp.getAddressesMatching(includeInterfaces = listOf("lo"))
        assertTrue(addresses.contains("127.0.0.1"))
    }

    @Test fun getInterfaceNameAddressMapMatching() {
        val map = LogIp.getInterfaceNameAddressMapMatching(includeInterfaces = listOf("lo"))
        assertTrue(map.containsKey("lo"))
        assertTrue(map["lo"]?.contains("127.0.0.1") == true)
    }

    @Test fun getInterfacesMatchingPartialMatch() {
        // Test that partial matching works - if we search for "p2p", it should match "p2p0", "p2p-something-1" but not "p3p"
        val allInterfaces = LogIp.getInterfaceNames(excludeInterfaces = emptyList())

        // Create a mock scenario by using available interfaces
        // If there's an interface that contains certain substring, test matching works
        if (allInterfaces.any { it.contains("eth") }) {
            val matchingInterfaces = LogIp.getInterfaceNamesMatching(includeInterfaces = listOf("eth"))
            // All returned interfaces should contain "eth"
            matchingInterfaces.forEach { name ->
                assertTrue(name.contains("eth"), "Interface name '$name' should contain 'eth'")
            }
        }

        // Test with "lo" which should exist on most systems
        val loInterfaces = LogIp.getInterfaceNamesMatching(includeInterfaces = listOf("lo"))
        loInterfaces.forEach { name ->
            assertTrue(name.contains("lo"), "Interface name '$name' should contain 'lo'")
        }
    }

    @Test fun getInterfacesMatchingMultipleTerms() {
        // Test matching with multiple search terms
        val interfaces = LogIp.getInterfaceNamesMatching(includeInterfaces = listOf("lo", "eth"))

        // Each returned interface should contain at least one of the search terms
        interfaces.forEach { name ->
            assertTrue(
                name.contains("lo") || name.contains("eth"),
                "Interface name '$name' should contain either 'lo' or 'eth'",
            )
        }
    }

    @Test fun getInterfacesMatchingEmpty() {
        // Test with empty search terms should return empty list
        val interfaces = LogIp.getInterfacesMatching(includeInterfaces = emptyList())
        assertTrue(interfaces.isEmpty())
    }
}
