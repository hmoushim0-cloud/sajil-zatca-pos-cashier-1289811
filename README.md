# سجل زاتكا ونقاط البيع الذكية - Sajil ZATCA POS Cashier (`sajil-zatca-pos-cashier-1289811`)
> منظومة نقاط البيع والفواتير الإلكترونية المتوافقة مع متطلبات هيئة الزكاة والضريبة والجمارك (ZATCA Phase 2 FATOORA)

- **Project ID**: `#1289811`
- **Commercial Archetype**: `pos_cashier`
- **Bound Hardware Peripheral**: `ESC/POS Thermal Receipt Printer + Barcode Scanner`
- **Package Name**: `sa.factory.sajilzatcaposcashi`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]

### Overview of Completed Fixes & Features
1. **ZATCA Phase 2 FATOORA E-Invoicing Engine (`ZatcaPhase2InvoiceProtocolEngine.kt`)**:
   - Implemented standard 8-Tag TLV (Tag-Length-Value) binary encoding for tax QR code generation (Seller Name, 15-digit VAT number, Timestamp, Total with VAT, VAT amount, SHA-256 Invoice Hash, ECDSA Signature, and Public Key).
   - Embedded canonical UBL 2.1 XML Simplified Tax Invoice descriptor generation (`urn:oasis:names:specification:ubl:schema:xsd:Invoice-2`).
   - Integrated deterministic SHA-256 invoice hashing and self-contained RFC 4648 Base64 serialization.

2. **ESC/POS Thermal Receipt Printer Protocol Engine (`EscPosThermalPrinterEngine.kt`)**:
   - Implemented standard Epson ESC/POS byte sequence generation (`ESC @` initialize, `ESC a` alignment, `GS !` double font sizing, `ESC E` bold emphasis, and `GS V` partial paper cut).
   - Formatted itemized commercial receipt layouts with Arabic UTF-8 text formatting and tax breakdown.
   - Built hardware health telemetry decoder evaluating paper sensors (`OK`, `NEAR_END`, `OUT`), thermal head operating temperature, and print buffer backlog.

3. **Hardware Driver & MVVM Integration (`HardwareSensorDriver.kt` & `SajilZatcaViewModel.kt`)**:
   - Bound dual-engine protocol pipeline into real-time telemetry Flow streaming live printer health metrics and ZATCA compliance status.
   - Modernized UI dividers (`HorizontalDivider`), resolved Compose Char syntax hazards, and corrected missing typography imports (`FontWeight`, `sp`).
   - Relocated and expanded unit regression test suite into `app/src/test/java` with 10 comprehensive test cases (`SajilZatcaRegressionTest.kt`).

4. **Resilience & Build Infrastructure**:
   - Added complete Gradle 8.10.2 wrapper with high-availability CDN distribution URL (`mirrors.cloud.tencent.com`).
   - Added high-availability Maven mirror (`maven.aliyun.com/repository/public`) to `settings.gradle.kts` to eliminate upstream 504 timeouts.

### Step-by-Step Instructions to Build and Test

```bash
# 1. Clean and build the Android Debug APK
./gradlew :app:assembleDebug

# 2. Run the 10-case regression unit test suite
./gradlew testDebugUnitTest

# 3. Verify test report
cat app/build/reports/tests/testDebugUnitTest/index.html
```
