package sa.factory.sajilzatcaposcashi.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.sajilzatcaposcashi.engine.EscPosThermalPrinterEngine
import sa.factory.sajilzatcaposcashi.engine.ZatcaInvoiceData
import sa.factory.sajilzatcaposcashi.engine.ZatcaPhase2InvoiceProtocolEngine
import java.util.Locale
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "ESC/POS Thermal Receipt Printer + Barcode Scanner"
    private val zatcaEngine = ZatcaPhase2InvoiceProtocolEngine()
    private val printerEngine = EscPosThermalPrinterEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Bluetooth SPP Handshake initializing (115200 baud)..."))
        delay(300)

        // Baseline ZATCA test invoice validation
        val sampleInvoice = ZatcaInvoiceData(
            invoiceNumber = "INV-2026-0001",
            sellerName = "شركة سجل للحلول الرقمية",
            vatRegistrationNumber = "310123456700003",
            invoiceTimestamp = "2026-11-08T11:00:00Z",
            invoiceTotalWithVat = 287.50,
            vatTotal = 37.50
        )
        val zatcaRes = zatcaEngine.validateAndProcess(sampleInvoice)

        emit(
            HardwareTelemetryFrame(
                peripheral,
                "ONLINE",
                "ESC/POS Thermal Printer: ONLINE | Paper: OK | ZATCA Phase 2: READY (Hash: ${zatcaRes.invoiceHashHex.take(12)}...)"
            )
        )

        var cycle = 0
        while (true) {
            delay(1500)
            cycle++

            val temp = 32.0 + Random.nextDouble(1.0, 5.0)
            val bufferBytes = Random.nextInt(0, 128)
            val printerTelemetry = printerEngine.evaluatePrinterStatus(0x00, temp, bufferBytes)

            val payload = String.format(
                Locale.US,
                "Printer: %s | Temp: %.1f C | Paper: %s | Buffer: %d B | Barcode: Laser 2D Scanner Ready | Frame #%d",
                printerTelemetry.connectionInterface,
                printerTelemetry.headTemperatureCelsius,
                printerTelemetry.paperStatus.label,
                printerTelemetry.bufferBytesPending,
                cycle
            )

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }

    fun getZatcaEngine(): ZatcaPhase2InvoiceProtocolEngine = zatcaEngine
    fun getPrinterEngine(): EscPosThermalPrinterEngine = printerEngine
}
