import org.junit.jupiter.api.Test

class LogIpTest {

    // todo: make a test that ads a lo filter and ensures that the loopback ip isn't included

    @Test fun testLogIp() {
        LogIp.logAllIPAddresses()
    }
}