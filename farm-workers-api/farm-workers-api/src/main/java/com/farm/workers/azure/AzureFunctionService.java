package com.farm.workers.azure;

import com.farm.workers.dto.WorkRecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Calling Azure Functions via HTTP           ║
 * ║  HttpClient / IHttpClientFactory  →  WebClient          ║
 * ║                                                          ║
 * ║  WebClient is Spring's non-blocking HTTP client          ║
 * ║  ≈ HttpClient with async/await in .NET                  ║
 * ║                                                          ║
 * ║  .block() = wait for result synchronously               ║
 * ║  ≈ .GetAwaiter().GetResult() or .Result in .NET         ║
 * ║  (avoid in production - use reactive chain instead)     ║
 * ║                                                          ║
 * ║  This service calls Azure Functions that have:          ║
 * ║  - HttpTrigger: for on-demand computation               ║
 * ║  - CosmosDB trigger: reacts to document changes         ║
 * ║  - Queue trigger: processes queue messages              ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureFunctionService {

    private final WebClient webClient;  // Injected from WebClientConfig @Bean

    @Value("${azure.functions.base-url}")
    private String functionsBaseUrl;

    @Value("${azure.functions.api-key}")
    private String functionsApiKey;

    /**
     * Call Azure Function: ComputeSeasonPayroll (HttpTrigger)
     *
     * This offloads heavy computation to a Function
     * ≈ calling a microservice or background processor in .NET
     *
     * The Azure Function is triggered by HTTP and returns aggregated payroll
     */
    public WorkRecordDto.SeasonSummary computeSeasonPayroll(String workerId, String season) {
        log.info("Calling Azure Function: ComputeSeasonPayroll worker={} season={}", workerId, season);

        try {
            // WebClient fluent API  ≈  httpClient.GetFromJsonAsync<T>(url) in .NET
            return webClient.get()
                    .uri(functionsBaseUrl + "/api/ComputeSeasonPayroll"
                         + "?workerId=" + workerId
                         + "&season=" + season)
                    .header("x-functions-key", functionsApiKey)  // Azure Function auth key
                    .retrieve()
                    // .onStatus() ≈ handling non-2xx status codes
                    .onStatus(
                        status -> status.is4xxClientError(),
                        response -> Mono.error(new RuntimeException("Function call failed: " + response.statusCode()))
                    )
                    .bodyToMono(WorkRecordDto.SeasonSummary.class)  // Deserialize JSON response
                    .timeout(Duration.ofSeconds(30))                // Timeout  ≈  CancellationToken
                    .block();                                        // Synchronous wait
        } catch (Exception e) {
            log.error("Failed to call ComputeSeasonPayroll function", e);
            throw new RuntimeException("Failed to compute season payroll", e);
        }
    }

    /**
     * Call Azure Function: TriggerBackgroundCheck (HttpTrigger)
     * Initiates async background check for a new worker
     * The Function itself runs asynchronously (fire-and-forget from our perspective)
     */
    public void triggerBackgroundCheck(String workerId, String nationalId, String nationality) {
        log.info("Triggering background check for worker: {}", workerId);

        var requestBody = Map.of(
            "workerId", workerId,
            "nationalId", nationalId,
            "nationality", nationality
        );

        try {
            // POST request with JSON body  ≈  httpClient.PostAsJsonAsync() in .NET
            webClient.post()
                    .uri(functionsBaseUrl + "/api/TriggerBackgroundCheck")
                    .header("x-functions-key", functionsApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                        result -> log.info("Background check triggered for: {}", workerId),
                        error -> log.error("Failed to trigger background check", error)
                        // subscribe() = truly fire-and-forget  ≈  _ = Task.Run(...)
                    );

        } catch (Exception e) {
            log.error("Failed to trigger background check for worker: {}", workerId, e);
        }
    }

    /**
     * Call Azure Function: GeneratePayslip (HttpTrigger)
     * Returns a URL to the generated PDF payslip stored in Azure Blob Storage
     */
    public String generatePayslip(String workerId, String season) {
        log.info("Generating payslip for worker: {} season: {}", workerId, season);

        try {
            var response = webClient.post()
                    .uri(functionsBaseUrl + "/api/GeneratePayslip")
                    .header("x-functions-key", functionsApiKey)
                    .bodyValue(Map.of("workerId", workerId, "season", season))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return response != null ? (String) response.get("payslipUrl") : null;

        } catch (Exception e) {
            log.error("Failed to generate payslip", e);
            throw new RuntimeException("Payslip generation failed", e);
        }
    }
}
