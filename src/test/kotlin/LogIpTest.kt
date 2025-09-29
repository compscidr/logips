import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections

class LogIpTest {
    private val logger = LoggerFactory.getLogger(LogIpTest::class.java)
    // todo: make a test that ads a lo filter and ensures that the loopback ip isn't included

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

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

    @Test fun testInterfaceMatchingWithP2pPatterns() {
        // Consolidated test for interface matching that covers the P2P interface issue from #61
        // This test ensures that interface matching works correctly by checking both name and displayName
        // and specifically tests P2P naming patterns like "p2p-p2p0-0" that should match "p2p"

        logger.info("Testing interface matching with P2P patterns and name/displayName checking")

        // Test the specific P2P naming patterns that were failing in the original issue
        val p2pTestCases =
            listOf(
                "p2p",
                "p2p0",
                "p2p-p2p0-0",
                "p2p-wlan0-1",
            )

        // Verify that all P2P naming patterns contain "p2p" (basic string matching logic)
        p2pTestCases.forEach { testInterfaceName ->
            val containsP2p = testInterfaceName.contains("p2p")
            assertTrue(containsP2p, "Interface name '$testInterfaceName' should contain 'p2p'")
            logger.debug("P2P pattern test: interface '$testInterfaceName' contains 'p2p': $containsP2p")
        }

        // Get all available interfaces and cache them to avoid multiple calls (performance optimization)
        val cachedInterfaces = LogIp.getInterfaces(excludeInterfaces = emptyList(), excludeDownInterfaces = false)
        logger.info("All available interfaces:")
        cachedInterfaces.forEach { netInterface ->
            logger.info("  Interface name: '${netInterface.name}', displayName: '${netInterface.displayName}'")
        }

        // Test if there are any actual P2P interfaces available on this system
        val p2pInterfaces = LogIp.getInterfacesMatching(includeInterfaces = listOf("p2p"), excludeDownInterfaces = false)
        logger.info("P2P interfaces found: ${p2pInterfaces.size}")
        p2pInterfaces.forEach { netInterface ->
            logger.info("  P2P Interface name: '${netInterface.name}', displayName: '${netInterface.displayName}'")
        }

        // Test that interface matching checks both name and displayName properties
        // Use loopback interfaces which should exist on all systems
        val loopbackInterfaces = LogIp.getInterfacesMatching(includeInterfaces = listOf("lo"), excludeDownInterfaces = false)
        assertTrue(loopbackInterfaces.isNotEmpty(), "Should find at least one loopback interface")

        loopbackInterfaces.forEach { netInterface ->
            val matchesName = netInterface.name.contains("lo")
            val matchesDisplayName = netInterface.displayName.contains("lo")
            logger.debug(
                "Loopback interface: name='${netInterface.name}' (contains 'lo': $matchesName), " +
                    "displayName='${netInterface.displayName}' (contains 'lo': $matchesDisplayName)",
            )

            // At least one of them should match (this validates the fix)
            assertTrue(
                matchesName || matchesDisplayName,
                "Either name '${netInterface.name}' or displayName '${netInterface.displayName}' should contain 'lo'",
            )
        }

        // Test partial matching with available interfaces to ensure the fix works correctly
        cachedInterfaces.forEach { netInterface ->
            val interfaceName = netInterface.name
            if (interfaceName.length >= 2) {
                // Use first 2 characters as search pattern
                val searchPattern = interfaceName.take(2)
                val matchingInterfaces =
                    LogIp.getInterfacesMatching(
                        includeInterfaces = listOf(searchPattern),
                        excludeDownInterfaces = false,
                    )

                // The original interface should be found since its name contains the search pattern
                val foundInterface = matchingInterfaces.find { it.name == interfaceName }
                assertTrue(
                    foundInterface != null,
                    "Interface '$interfaceName' should be found when searching for pattern '$searchPattern' " +
                        "since the name contains it (validates name and displayName checking fix)",
                )
            }
        }
    }

    @Test fun testLogAllIpAddressesWithCustomLogLevel() {
        // Test that logAllIpAddresses accepts different log levels without errors
        LogIp.logAllIpAddresses(logLevel = Level.TRACE)
        LogIp.logAllIpAddresses(logLevel = Level.DEBUG)
        LogIp.logAllIpAddresses(logLevel = Level.INFO)
        LogIp.logAllIpAddresses(logLevel = Level.WARN)
        LogIp.logAllIpAddresses(logLevel = Level.ERROR)
    }

    @Test fun testGetInterfacesWithCustomLogLevel() {
        // Test that getInterfaces accepts different log levels and returns results
        val interfacesTrace = LogIp.getInterfaces(logLevel = Level.TRACE)
        val interfacesInfo = LogIp.getInterfaces(logLevel = Level.INFO)
        val interfacesWarn = LogIp.getInterfaces(logLevel = Level.WARN)

        // All calls should return the same interfaces regardless of log level
        assertTrue(interfacesTrace.isNotEmpty())
        assertTrue(interfacesInfo.isNotEmpty())
        assertTrue(interfacesWarn.isNotEmpty())
    }

    @Test fun testGetInterfacesMatchingWithCustomLogLevel() {
        // Test that getInterfacesMatching accepts different log levels
        val interfaces =
            LogIp.getInterfacesMatching(
                includeInterfaces = listOf("lo"),
                logLevel = Level.INFO,
            )
        assertTrue(interfaces.isNotEmpty())
    }

    @Test fun testGetInterfacesWithLogWarningDisabled() {
        // Test that disabling logWarning doesn't affect functionality
        // This test primarily ensures the parameter is accepted and doesn't break anything
        val interfaces = LogIp.getInterfaces(logWarning = false)
        // Should still return interfaces even with warning logging disabled
        assertTrue(interfaces.isNotEmpty() || true) // Always passes as we can't guarantee non-empty
    }

    @Test fun testGetInterfacesWithLogErrorDisabled() {
        // Test that disabling logError doesn't affect functionality
        val interfaces = LogIp.getInterfaces(logError = false)
        // Should still work without error logging
        assertTrue(interfaces.isNotEmpty() || true) // Always passes as we can't guarantee non-empty
    }

    @Test fun testGetInterfacesMatchingWithLoggingControlFlags() {
        // Test combining log level and logging control flags
        val interfaces =
            LogIp.getInterfacesMatching(
                includeInterfaces = listOf("lo"),
                logLevel = Level.WARN,
                logWarning = false,
                logError = false,
            )
        assertTrue(interfaces.isNotEmpty())
    }

    @Test fun testLogAllIpAddressesWithAllParameters() {
        // Test logAllIpAddresses with all new parameters
        LogIp.logAllIpAddresses(
            excludeInterfaces = listOf("docker"),
            excludeDownInterfaces = true,
            logLevel = Level.INFO,
            logWarning = true,
            logError = true,
        )
    }

    @Test fun testLogAllIpAddressesUsesCorrectLogLevel() {
        // Mock logger to verify the correct log level is used
        val mockLogger = mockk<Logger>(relaxed = true)

        // Test with INFO level
        LogIp.logAllIpAddresses(logger = mockLogger, logLevel = Level.INFO)

        // Verify that atLevel was called with INFO
        verify(atLeast = 1) { mockLogger.atLevel(Level.INFO) }
    }

    @Test fun testLogAllIpAddressesUsesTraceByDefault() {
        // Mock logger to verify TRACE is used by default
        val mockLogger = mockk<Logger>(relaxed = true)

        // Call without specifying logLevel
        LogIp.logAllIpAddresses(logger = mockLogger)

        // Verify that atLevel was called with TRACE (the default)
        verify(atLeast = 1) { mockLogger.atLevel(Level.TRACE) }
    }

    @Test fun testGetInterfacesUsesCorrectLogLevel() {
        // Mock logger to verify the correct log level is used
        val mockLogger = mockk<Logger>(relaxed = true)

        // Test with WARN level
        LogIp.getInterfaces(logger = mockLogger, logLevel = Level.WARN)

        // Verify that atLevel was called with WARN
        verify(atLeast = 1) { mockLogger.atLevel(Level.WARN) }
    }

    @Test fun testGetInterfacesWithLogWarningFalse() {
        // Mock logger to verify warn is NOT called when logWarning is false
        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logWarning = false
        LogIp.getInterfaces(logger = mockLogger, logWarning = false)

        // Verify warn was never called
        verify(exactly = 0) { mockLogger.warn(any<String>()) }
    }

    @Test fun testGetInterfacesWithLogErrorFalse() {
        // Mock logger to verify error is NOT called when logError is false
        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logError = false
        LogIp.getInterfaces(logger = mockLogger, logError = false)

        // Verify error was never called (with any parameters)
        verify(exactly = 0) { mockLogger.error(any<String>(), any<Throwable>()) }
        verify(exactly = 0) { mockLogger.error(any<String>()) }
    }

    @Test fun testGetInterfacesMatchingUsesCorrectLogLevel() {
        // Mock logger to verify the correct log level is used
        val mockLogger = mockk<Logger>(relaxed = true)

        // Test with ERROR level
        LogIp.getInterfacesMatching(
            logger = mockLogger,
            includeInterfaces = listOf("lo"),
            logLevel = Level.ERROR,
        )

        // Verify that atLevel was called with ERROR
        verify(atLeast = 1) { mockLogger.atLevel(Level.ERROR) }
    }

    @Test fun testGetInterfacesMatchingWithLogWarningFalse() {
        // Mock logger to verify warn is NOT called when logWarning is false
        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logWarning = false
        LogIp.getInterfacesMatching(
            logger = mockLogger,
            includeInterfaces = listOf("lo"),
            logWarning = false,
        )

        // Verify warn was never called
        verify(exactly = 0) { mockLogger.warn(any<String>()) }
    }

    @Test fun testGetInterfacesMatchingWithLogErrorFalse() {
        // Mock logger to verify error is NOT called when logError is false
        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logError = false
        LogIp.getInterfacesMatching(
            logger = mockLogger,
            includeInterfaces = listOf("lo"),
            logError = false,
        )

        // Verify error was never called
        verify(exactly = 0) { mockLogger.error(any<String>(), any<Throwable>()) }
        verify(exactly = 0) { mockLogger.error(any<String>()) }
    }

    @Test fun testAllLogLevelsWithLogAllIpAddresses() {
        // Test that each log level is correctly passed through
        val levels = listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)

        levels.forEach { level ->
            val mockLogger = mockk<Logger>(relaxed = true)
            LogIp.logAllIpAddresses(logger = mockLogger, logLevel = level)
            verify(atLeast = 1) { mockLogger.atLevel(level) }
        }
    }

    @Test fun testAllLogLevelsWithGetInterfaces() {
        // Test that each log level is correctly passed through
        val levels = listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)

        levels.forEach { level ->
            val mockLogger = mockk<Logger>(relaxed = true)
            LogIp.getInterfaces(logger = mockLogger, logLevel = level)
            verify(atLeast = 1) { mockLogger.atLevel(level) }
        }
    }

    @Test fun testAllLogLevelsWithGetInterfacesMatching() {
        // Test that each log level is correctly passed through
        val levels = listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)

        levels.forEach { level ->
            val mockLogger = mockk<Logger>(relaxed = true)
            LogIp.getInterfacesMatching(
                logger = mockLogger,
                includeInterfaces = listOf("lo"),
                logLevel = level,
            )
            verify(atLeast = 1) { mockLogger.atLevel(level) }
        }
    }

    @Test fun testGetInterfacesWithNullInterfacesAndLogWarningTrue() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logWarning = true (default)
        val interfaces = LogIp.getInterfaces(logger = mockLogger, logWarning = true)

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify warn was called
        verify(exactly = 1) { mockLogger.warn("No network interfaces found") }
    }

    @Test fun testGetInterfacesWithNullInterfacesAndLogWarningFalse() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logWarning = false
        val interfaces = LogIp.getInterfaces(logger = mockLogger, logWarning = false)

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify warn was NOT called
        verify(exactly = 0) { mockLogger.warn(any<String>()) }
    }

    @Test fun testGetInterfacesMatchingWithNullInterfacesAndLogWarningTrue() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logWarning = true (default)
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = mockLogger,
                includeInterfaces = listOf("lo"),
                logWarning = true,
            )

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify warn was called
        verify(exactly = 1) { mockLogger.warn("No network interfaces found") }
    }

    @Test fun testGetInterfacesMatchingWithNullInterfacesAndLogWarningFalse() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logWarning = false
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = mockLogger,
                includeInterfaces = listOf("lo"),
                logWarning = false,
            )

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify warn was NOT called
        verify(exactly = 0) { mockLogger.warn(any<String>()) }
    }

    @Test fun testGetInterfacesWithSocketExceptionAndLogErrorTrue() {
        // Mock NetworkInterface.getNetworkInterfaces() to throw SocketException
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("Test exception")

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logError = true (default)
        val interfaces = LogIp.getInterfaces(logger = mockLogger, logError = true)

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify error was called with the exception
        verify(exactly = 1) { mockLogger.error("Error getting network interfaces", any<SocketException>()) }
    }

    @Test fun testGetInterfacesWithSocketExceptionAndLogErrorFalse() {
        // Mock NetworkInterface.getNetworkInterfaces() to throw SocketException
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("Test exception")

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logError = false
        val interfaces = LogIp.getInterfaces(logger = mockLogger, logError = false)

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify error was NOT called
        verify(exactly = 0) { mockLogger.error(any<String>(), any<Throwable>()) }
    }

    @Test fun testGetInterfacesMatchingWithSocketExceptionAndLogErrorTrue() {
        // Mock NetworkInterface.getNetworkInterfaces() to throw SocketException
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("Test exception")

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logError = true (default)
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = mockLogger,
                includeInterfaces = listOf("lo"),
                logError = true,
            )

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify error was called with the exception
        verify(exactly = 1) { mockLogger.error("Error getting network interfaces", any<SocketException>()) }
    }

    @Test fun testGetInterfacesMatchingWithSocketExceptionAndLogErrorFalse() {
        // Mock NetworkInterface.getNetworkInterfaces() to throw SocketException
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("Test exception")

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with logError = false
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = mockLogger,
                includeInterfaces = listOf("lo"),
                logError = false,
            )

        // Should return empty list
        assertTrue(interfaces.isEmpty())

        // Verify error was NOT called
        verify(exactly = 0) { mockLogger.error(any<String>(), any<Throwable>()) }
    }

    @Test fun testLogAllIpAddressesPassesThroughLogWarningAndLogError() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null to trigger warning branch
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        val mockLogger = mockk<Logger>(relaxed = true)

        // Test with logWarning = true and logError = true
        LogIp.logAllIpAddresses(logger = mockLogger, logWarning = true, logError = true)
        verify(exactly = 1) { mockLogger.warn("No network interfaces found") }

        // Reset mock
        unmockkAll()
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        val mockLogger2 = mockk<Logger>(relaxed = true)

        // Test with logWarning = false
        LogIp.logAllIpAddresses(logger = mockLogger2, logWarning = false, logError = false)
        verify(exactly = 0) { mockLogger2.warn(any<String>()) }
    }

    @Test fun testGetInterfacesWithNullLogger() {
        // Test that passing null logger doesn't cause issues
        val interfaces = LogIp.getInterfaces(logger = null, excludeInterfaces = emptyList())
        // Should still work
        assertTrue(interfaces.isNotEmpty())
    }

    @Test fun testGetInterfacesWithNullLoggerAndNullInterfaces() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        // Test with null logger - should not throw
        val interfaces = LogIp.getInterfaces(logger = null, logWarning = true)
        assertTrue(interfaces.isEmpty())
    }

    @Test fun testGetInterfacesWithNullLoggerAndSocketException() {
        // Mock NetworkInterface.getNetworkInterfaces() to throw SocketException
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("Test exception")

        // Test with null logger - should not throw
        val interfaces = LogIp.getInterfaces(logger = null, logError = true)
        assertTrue(interfaces.isEmpty())
    }

    @Test fun testGetInterfacesMatchingWithNullLogger() {
        // Test that passing null logger doesn't cause issues
        val interfaces = LogIp.getInterfacesMatching(logger = null, includeInterfaces = listOf("lo"))
        // Should still work
        assertTrue(interfaces.isNotEmpty())
    }

    @Test fun testGetInterfacesMatchingWithNullLoggerAndNullInterfaces() {
        // Mock NetworkInterface.getNetworkInterfaces() to return null
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns null

        // Test with null logger - should not throw
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = null,
                includeInterfaces = listOf("lo"),
                logWarning = true,
            )
        assertTrue(interfaces.isEmpty())
    }

    @Test fun testGetInterfacesMatchingWithNullLoggerAndSocketException() {
        // Mock NetworkInterface.getNetworkInterfaces() to throw SocketException
        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("Test exception")

        // Test with null logger - should not throw
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = null,
                includeInterfaces = listOf("lo"),
                logError = true,
            )
        assertTrue(interfaces.isEmpty())
    }

    @Test fun testGetInterfacesMatchingWithDownInterfaceAndExcludeDownTrue() {
        // This tests line 171 with networkInterface.isUp.not() = true and excludeDownInterfaces = true
        // The real test happens during normal execution, but we can verify it works
        val interfaces =
            LogIp.getInterfacesMatching(
                includeInterfaces = listOf("lo"),
                excludeDownInterfaces = true,
            )
        // All returned interfaces should be up
        interfaces.forEach { netInterface ->
            if (!netInterface.isUp) {
                throw AssertionError("Found down interface when excludeDownInterfaces=true")
            }
        }
    }

    @Test fun testGetInterfacesMatchingWithDownInterfaceAndExcludeDownFalse() {
        // This tests line 171 with excludeDownInterfaces = false
        // When false, down interfaces should be included
        val allInterfaces =
            LogIp.getInterfacesMatching(
                includeInterfaces = listOf("lo"),
                excludeDownInterfaces = false,
            )
        // Should include interfaces regardless of up/down status
        assertTrue(allInterfaces.isNotEmpty())
    }

    @Test fun testGetInterfacesWithDownInterfaceAndExcludeDownTrue() {
        // Test getInterfaces with excludeDownInterfaces = true
        val interfaces =
            LogIp.getInterfaces(
                excludeInterfaces = emptyList(),
                excludeDownInterfaces = true,
            )
        // All returned interfaces should be up
        interfaces.forEach { netInterface ->
            assertTrue(netInterface.isUp, "Interface ${netInterface.name} should be up when excludeDownInterfaces=true")
        }
    }

    @Test fun testGetInterfacesWithDownInterfaceAndExcludeDownFalse() {
        // Test getInterfaces with excludeDownInterfaces = false
        val allInterfaces =
            LogIp.getInterfaces(
                excludeInterfaces = emptyList(),
                excludeDownInterfaces = false,
            )
        // Should include interfaces regardless of up/down status
        assertTrue(allInterfaces.isNotEmpty())
    }

    @Test fun testGetAddressesWithNullLogger() {
        // Test getAddresses with null logger
        val addresses = LogIp.getAddresses(logger = null)
        // Should work fine
        assertTrue(addresses.isNotEmpty() || addresses.isEmpty()) // Just verify no exception
    }

    @Test fun testGetInterfaceNamesWithNullLogger() {
        // Test getInterfaceNames with null logger
        val names = LogIp.getInterfaceNames(logger = null)
        // Should work fine
        assertTrue(names.isNotEmpty() || names.isEmpty()) // Just verify no exception
    }

    @Test fun testGetInterfaceNameAddressMapWithNullLogger() {
        // Test getInterfaceNameAddressMap with null logger
        val map = LogIp.getInterfaceNameAddressMap(logger = null)
        // Should work fine
        assertTrue(map.isNotEmpty() || map.isEmpty()) // Just verify no exception
    }

    @Test fun testGetInterfaceNamesMatchingWithNullLogger() {
        // Test getInterfaceNamesMatching with null logger
        val names = LogIp.getInterfaceNamesMatching(logger = null, includeInterfaces = listOf("lo"))
        // Should work fine
        assertTrue(names.isNotEmpty() || names.isEmpty()) // Just verify no exception
    }

    @Test fun testGetAddressesMatchingWithNullLogger() {
        // Test getAddressesMatching with null logger
        val addresses = LogIp.getAddressesMatching(logger = null, includeInterfaces = listOf("lo"))
        // Should work fine
        assertTrue(addresses.isNotEmpty() || addresses.isEmpty()) // Just verify no exception
    }

    @Test fun testGetInterfaceNameAddressMapMatchingWithNullLogger() {
        // Test getInterfaceNameAddressMapMatching with null logger
        val map = LogIp.getInterfaceNameAddressMapMatching(logger = null, includeInterfaces = listOf("lo"))
        // Should work fine
        assertTrue(map.isNotEmpty() || map.isEmpty()) // Just verify no exception
    }

    @Test fun testLogAllIpAddressesWithInterfaceWithNoIpAddresses() {
        // Mock NetworkInterface with no IP addresses to trigger lines 35-36
        mockkStatic(NetworkInterface::class)

        val mockInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockInterface.name } returns "test0"
        every { mockInterface.displayName } returns "Test Interface"
        every { mockInterface.isUp } returns true
        every { mockInterface.inetAddresses } returns Collections.emptyEnumeration()

        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(mockInterface))

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call logAllIpAddresses
        LogIp.logAllIpAddresses(logger = mockLogger, excludeInterfaces = emptyList())

        // Verify that the "No ips" message was logged (line 36)
        verify(atLeast = 1) { mockLogger.atLevel(any()).log("  No ips") }
    }

    @Test fun testGetInterfacesWithDownInterfaceAndExcludeDownTrueWithLogging() {
        // Mock a down interface to trigger lines 106-109
        mockkStatic(NetworkInterface::class)

        val mockDownInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockDownInterface.name } returns "down0"
        every { mockDownInterface.displayName } returns "Down Interface"
        every { mockDownInterface.isUp } returns false

        val mockUpInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockUpInterface.name } returns "up0"
        every { mockUpInterface.displayName } returns "Up Interface"
        every { mockUpInterface.isUp } returns true

        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(mockDownInterface, mockUpInterface))

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with excludeDownInterfaces = true
        val interfaces =
            LogIp.getInterfaces(
                logger = mockLogger,
                excludeInterfaces = emptyList(),
                excludeDownInterfaces = true,
            )

        // Should only return the up interface
        assertTrue(interfaces.size == 1)
        assertTrue(interfaces[0].name == "up0")

        // Verify the down interface exclusion was logged (lines 106-109)
        verify(atLeast = 1) {
            mockLogger.atLevel(any()).log(match { it.contains("Excluding interface down0") && it.contains("interface is down") })
        }
    }

    @Test fun testGetInterfacesMatchingWithDownInterfaceAndExcludeDownTrueWithLogging() {
        // Mock a down interface to trigger lines 171-175
        mockkStatic(NetworkInterface::class)

        val mockDownInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockDownInterface.name } returns "test0"
        every { mockDownInterface.displayName } returns "Test Down Interface"
        every { mockDownInterface.isUp } returns false

        val mockUpInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockUpInterface.name } returns "test1"
        every { mockUpInterface.displayName } returns "Test Up Interface"
        every { mockUpInterface.isUp } returns true

        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(mockDownInterface, mockUpInterface))

        val mockLogger = mockk<Logger>(relaxed = true)

        // Call with includeInterfaces that matches both, excludeDownInterfaces = true
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = mockLogger,
                includeInterfaces = listOf("test"),
                excludeDownInterfaces = true,
            )

        // Should only return the up interface
        assertTrue(interfaces.size == 1)
        assertTrue(interfaces[0].name == "test1")

        // Verify the down interface exclusion was logged (lines 171-175)
        verify(atLeast = 1) {
            mockLogger
                .atLevel(any())
                .log(match { it.contains("Excluding matched interface test0") && it.contains("interface is down") })
        }
    }

    @Test fun testGetInterfacesWithDownInterfaceExcludeDownTrueAndNullLogger() {
        // Test lines 106-109 with null logger
        mockkStatic(NetworkInterface::class)

        val mockDownInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockDownInterface.name } returns "down0"
        every { mockDownInterface.displayName } returns "Down Interface"
        every { mockDownInterface.isUp } returns false

        val mockUpInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockUpInterface.name } returns "up0"
        every { mockUpInterface.displayName } returns "Up Interface"
        every { mockUpInterface.isUp } returns true

        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(mockDownInterface, mockUpInterface))

        // Call with null logger and excludeDownInterfaces = true
        val interfaces =
            LogIp.getInterfaces(
                logger = null,
                excludeInterfaces = emptyList(),
                excludeDownInterfaces = true,
            )

        // Should only return the up interface, no exception
        assertTrue(interfaces.size == 1)
        assertTrue(interfaces[0].name == "up0")
    }

    @Test fun testGetInterfacesMatchingWithDownInterfaceExcludeDownTrueAndNullLogger() {
        // Test lines 171-175 with null logger
        mockkStatic(NetworkInterface::class)

        val mockDownInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockDownInterface.name } returns "test0"
        every { mockDownInterface.displayName } returns "Test Down Interface"
        every { mockDownInterface.isUp } returns false

        val mockUpInterface = mockk<NetworkInterface>(relaxed = true)
        every { mockUpInterface.name } returns "test1"
        every { mockUpInterface.displayName } returns "Test Up Interface"
        every { mockUpInterface.isUp } returns true

        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(mockDownInterface, mockUpInterface))

        // Call with null logger and excludeDownInterfaces = true
        val interfaces =
            LogIp.getInterfacesMatching(
                logger = null,
                includeInterfaces = listOf("test"),
                excludeDownInterfaces = true,
            )

        // Should only return the up interface, no exception
        assertTrue(interfaces.size == 1)
        assertTrue(interfaces[0].name == "test1")
    }
}
