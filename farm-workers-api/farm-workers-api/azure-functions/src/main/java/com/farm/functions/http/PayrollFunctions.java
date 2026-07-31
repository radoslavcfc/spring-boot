package com.farm.functions.http;

import com.farm.functions.model.SeasonPayrollRequest;
import com.farm.functions.model.SeasonPayrollResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import lombok.extern.java.Log;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Azure Functions HttpTrigger                ║
 * ║                                                          ║
 * ║  .NET:                                                   ║
 * ║  [Function("ComputeSeasonPayroll")]                     ║
 * ║  public IActionResult Run(                              ║
 * ║    [HttpTrigger(AuthorizationLevel.Function,            ║
 * ║      "get", Route = "payroll")] HttpRequest req)        ║
 * ║                                                          ║
 * ║  Java:                                                   ║
 * ║  @FunctionName("ComputeSeasonPayroll")                  ║
 * ║  public HttpResponseMessage run(                        ║
 * ║    @HttpTrigger(...) HttpRequestMessage<...> request)   ║
 * ║                                                          ║
 * ║  Key difference: Java functions live in ANY class        ║
 * ║  (no need to extend FunctionBase or similar)            ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class PayrollFunctions {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();  // Registers Java 8 date/time module

    /**
     * HTTP Trigger: Compute seasonal payroll for a worker
     * Called by the Spring Boot API's AzureFunctionService
     *
     * GET /api/ComputeSeasonPayroll?workerId=xxx&season=2024-HARVEST
     */
    @FunctionName("ComputeSeasonPayroll")
    public HttpResponseMessage computeSeasonPayroll(
            // @HttpTrigger annotation = the binding configuration
            // ≈ [HttpTrigger(AuthorizationLevel.Function, "get")] in .NET
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION,   // Requires x-functions-key header
                route = "ComputeSeasonPayroll"
            ) HttpRequestMessage<Optional<String>> request,

            // ExecutionContext provides logger  ≈  ILogger<T> in .NET
            final ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("ComputeSeasonPayroll function triggered");

        // Extract query params  ≈  request.Query["workerId"] in .NET
        String workerId = request.getQueryParameters().get("workerId");
        String season = request.getQueryParameters().get("season");

        if (workerId == null || season == null) {
            // createResponseBuilder  ≈  Results.BadRequest() / new BadRequestObjectResult()
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("workerId and season query parameters are required")
                    .build();
        }

        try {
            // In a real function: query CosmosDB using output binding or SDK
            // Here we simulate the computation
            SeasonPayrollResponse response = computePayroll(workerId, season, log);

            // HttpStatus.OK  ≈  StatusCodes.Status200OK in .NET
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(response))
                    .build();

        } catch (Exception e) {
            log.severe("Error computing payroll: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error computing payroll")
                    .build();
        }
    }

    /**
     * HTTP Trigger: Generate a PDF payslip and store in Azure Blob Storage
     * Returns the SAS URL to the generated PDF
     */
    @FunctionName("GeneratePayslip")
    public HttpResponseMessage generatePayslip(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,

            // Output binding to Blob Storage - writes the PDF automatically!
            // ≈ [Blob("payslips/{workerId}-{season}.pdf", FileAccess.Write)] Stream out
            @BlobOutput(
                name = "payslipBlob",
                path = "payslips/{workerId}-{season}.pdf",
                connection = "STORAGE_CONNECTION_STRING"
            ) OutputBinding<byte[]> payslipBlob,

            final ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("GeneratePayslip triggered");

        try {
            String body = request.getBody().orElse("{}");
            var requestData = objectMapper.readValue(body, SeasonPayrollRequest.class);

            // Generate PDF bytes (simplified - use iText or Apache PDFBox in reality)
            byte[] pdfBytes = generatePdfBytes(requestData.getWorkerId(), requestData.getSeason());

            // Output binding writes to Blob Storage automatically
            payslipBlob.setValue(pdfBytes);

            // Return the URL (in production, generate a SAS token URL)
            String blobUrl = "https://storageaccount.blob.core.windows.net/payslips/"
                    + requestData.getWorkerId() + "-" + requestData.getSeason() + ".pdf";

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("{\"payslipUrl\": \"" + blobUrl + "\"}")
                    .header("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            log.severe("Error generating payslip: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payslip generation failed")
                    .build();
        }
    }

    /**
     * HTTP Trigger: Trigger a background check for a new worker
     * This is a fire-and-forget from the API's perspective
     * The Function queues a background check job
     */
    @FunctionName("TriggerBackgroundCheck")
    public HttpResponseMessage triggerBackgroundCheck(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,

            // Output binding: write a message to a Service Bus queue
            // ≈ [ServiceBus("background-checks", Connection="...")] IAsyncCollector<T> in .NET
            @ServiceBusQueueOutput(
                name = "backgroundCheckQueue",
                queueName = "background-checks",
                connection = "SERVICEBUS_CONNECTION_STRING"
            ) OutputBinding<String> backgroundCheckQueue,

            final ExecutionContext context) {

        Logger log = context.getLogger();

        try {
            String body = request.getBody().orElse("{}");
            log.info("Triggering background check: " + body);

            // Write to Service Bus queue via output binding
            // The binding handles serialization + sending automatically
            backgroundCheckQueue.setValue(body);

            // Return 202 Accepted (async processing)  ≈  Results.Accepted()
            return request.createResponseBuilder(HttpStatus.ACCEPTED)
                    .body("{\"status\": \"Background check queued\"}")
                    .header("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            log.severe("Error triggering background check: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to queue background check")
                    .build();
        }
    }

    // Placeholder implementations for demo
    private SeasonPayrollResponse computePayroll(String workerId, String season, Logger log) {
        log.info("Computing payroll for worker=" + workerId + " season=" + season);
        // In reality: query CosmosDB work-records container and aggregate
        return SeasonPayrollResponse.builder()
                .workerId(workerId)
                .season(season)
                .totalHoursWorked(new BigDecimal("160.5"))
                .totalEarnings(new BigDecimal("2408.75"))
                .totalDaysWorked(22)
                .build();
    }

    private byte[] generatePdfBytes(String workerId, String season) {
        // In reality: use iText7 or Apache PDFBox to generate a real PDF
        String content = "PAYSLIP - Worker: " + workerId + " Season: " + season;
        return content.getBytes();
    }
}
