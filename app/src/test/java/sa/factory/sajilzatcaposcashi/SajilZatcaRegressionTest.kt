package sa.factory.sajilzatcaposcashi

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.sajilzatcaposcashi.data.repository.SajilZatcaRepositoryImpl
import sa.factory.sajilzatcaposcashi.engine.*
import sa.factory.sajilzatcaposcashi.hardware.HardwareSensorDriver
import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem

class SajilZatcaRegressionTest {

    private val sampleInvoice = ZatcaInvoiceData(
        invoiceNumber = "INV-2026-9901",
        sellerName = "سجل لحلول نقاط البيع الذكية",
        vatRegistrationNumber = "310123456700003",
        invoiceTimestamp = "2026-11-08T11:30:00Z",
        invoiceTotalWithVat = 230.00,
        vatTotal = 30.00
    )

    @Test
    fun testCheckoutCalculationAndVat15() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = SajilZatcaRepositoryImpl(sensorDriver)

        val item1 = SajilZatcaItem("t1", "قارئ باركود ليزري", "طراز مكتبي 2D", 100.0, "معتمد", "العتاد")
        val item2 = SajilZatcaItem("t2", "طابعة إيصالات حرارية", "سرعة 250 مم/ثانية", 200.0, "معتمد", "العتاد")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testZatcaTlvStructureAndLength() {
        val engine = ZatcaPhase2InvoiceProtocolEngine()
        val tlvBytes = engine.buildTlvPayload(sampleInvoice)

        assertTrue("TLV bytes must be non-empty", tlvBytes.isNotEmpty())
        assertEquals("First byte must be Tag 1 (Seller Name)", 1, tlvBytes[0].toInt())
    }

    @Test
    fun testZatcaBase64Encoding() {
        val engine = ZatcaPhase2InvoiceProtocolEngine()
        val rawBytes = "ZATCA_PHASE_2_TEST".toByteArray(Charsets.UTF_8)
        val base64 = engine.encodeBase64(rawBytes)

        assertEquals("WkFUQ0FfUEhBU0VfMl9URVNU", base64)
    }

    @Test
    fun testZatcaSha256InvoiceHashDeterminism() {
        val engine = ZatcaPhase2InvoiceProtocolEngine()
        val hash1 = engine.computeInvoiceHash(sampleInvoice)
        val hash2 = engine.computeInvoiceHash(sampleInvoice)

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
    }

    @Test
    fun testZatcaVatNumberValidationValid() {
        val engine = ZatcaPhase2InvoiceProtocolEngine()
        val result = engine.validateAndProcess(sampleInvoice)

        assertTrue("Valid ZATCA 15-digit VAT number must pass", result.isValid)
        assertTrue("Errors must be empty", result.errors.isEmpty())
        assertTrue("TLV Base64 string must be generated", result.tlvBase64.isNotEmpty())
    }

    @Test
    fun testZatcaVatNumberValidationInvalid() {
        val engine = ZatcaPhase2InvoiceProtocolEngine()
        val invalidInvoice = sampleInvoice.copy(vatRegistrationNumber = "1234567890")
        val result = engine.validateAndProcess(invalidInvoice)

        assertFalse("Invalid VAT number must be rejected", result.isValid)
        assertTrue("Must report VAT number error", result.errors.any { it.contains("VAT") })
    }

    @Test
    fun testZatcaUbl21XmlGeneration() {
        val engine = ZatcaPhase2InvoiceProtocolEngine()
        val xml = engine.generateUbl21Xml(sampleInvoice)

        assertTrue("XML must contain root Invoice element", xml.contains("<Invoice"))
        assertTrue("XML must contain invoice number", xml.contains(sampleInvoice.invoiceNumber))
        assertTrue("XML must declare SAR currency", xml.contains("currencyID=\"SAR\""))
        assertTrue("XML must contain TaxTotal", xml.contains("<cac:TaxTotal>"))
    }

    @Test
    fun testEscPosReceiptByteStreamCommands() {
        val engine = EscPosThermalPrinterEngine()
        val items = listOf(
            ReceiptLineItem("طابعة حرارية", 1, 200.0),
            ReceiptLineItem("ورق حراري 80 مم", 2, 15.0)
        )
        val bytes = engine.buildReceiptByteStream(
            storeName = "متجر سجل التجاري",
            vatNumber = "310123456700003",
            invoiceNumber = "INV-001",
            timestamp = "2026-11-08 11:30",
            items = items,
            zatcaQrBase64 = "AQZ...TEST_QR..."
        )

        assertTrue("Receipt byte stream must be non-empty", bytes.isNotEmpty())
        assertEquals("First byte must be ESC (0x1B)", 0x1B.toByte(), bytes[0])
        assertEquals("Second byte must be @ (0x40)", 0x40.toByte(), bytes[1])
    }

    @Test
    fun testEscPosPrinterTelemetryEvaluation() {
        val engine = EscPosThermalPrinterEngine()

        val stateOk = engine.evaluatePrinterStatus(0x00, 35.0, 10)
        assertEquals(PaperStatus.OK, stateOk.paperStatus)
        assertFalse(stateOk.isOverheated)

        val stateNearEnd = engine.evaluatePrinterStatus(0x04, 38.0, 20)
        assertEquals(PaperStatus.NEAR_END, stateNearEnd.paperStatus)

        val stateEmpty = engine.evaluatePrinterStatus(0x08, 70.0, 50)
        assertEquals(PaperStatus.OUT, stateEmpty.paperStatus)
        assertTrue(stateEmpty.isOverheated)
    }

    @Test
    fun testHardwareSensorDriverBinding() = runBlocking {
        val driver = HardwareSensorDriver()
        assertNotNull(driver.getZatcaEngine())
        assertNotNull(driver.getPrinterEngine())

        val firstFrame = driver.observePeripheralStream().first()
        assertEquals("ESC/POS Thermal Receipt Printer + Barcode Scanner", firstFrame.peripheralName)
        assertEquals("CONNECTING", firstFrame.connectionState)
    }
}
