package com.farm.workers.controller;

import com.farm.workers.dto.WorkerDto;
import com.farm.workers.exception.ResourceNotFoundException;
import com.farm.workers.model.Worker;
import com.farm.workers.service.WorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Controller Integration Tests               ║
 * ║  WebApplicationFactory<Program>  →  @WebMvcTest         ║
 * ║  _client.GetAsync("/api/workers") →  mockMvc.perform(   ║
 * ║                                         get("/api/..."))║
 * ║  response.StatusCode              →  .andExpect(        ║
 * ║    .Should().Be(200)                   status().isOk()) ║
 * ║  [Authorize] bypassed in tests    →  @WithMockUser      ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @WebMvcTest loads ONLY the web layer (controllers, filters, security)
 * All services/repos are NOT loaded - must be @MockBean
 * ≈ WebApplicationFactory with fakes/stubs for the service layer
 *
 * MockMvc lets you make HTTP requests without a real HTTP server
 * ≈ HttpClient with TestServer in ASP.NET Core integration tests
 */
@WebMvcTest(WorkerController.class)
@DisplayName("WorkerController Integration Tests")
class WorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;  // ≈ HttpClient in .NET integration tests

    @Autowired
    private ObjectMapper objectMapper;  // Jackson JSON serializer

    // @MockBean adds a Mockito mock to the Spring context
    // ≈ services.AddScoped<IWorkerService>(_ => Mock.Of<IWorkerService>())
    @MockBean
    private WorkerService workerService;

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/workers
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /workers returns 200 with list")
    @WithMockUser  // Simulate an authenticated user  ≈  ClaimsPrincipal with mock identity
    void getAllWorkers_ReturnsOkWithList() throws Exception {
        // ARRANGE
        var summary = WorkerDto.Summary.builder()
                .id("w1").fullName("Ion Popescu").nationality("RO")
                .status(Worker.WorkerStatus.ACTIVE).build();
        when(workerService.getAllWorkers()).thenReturn(List.of(summary));

        // ACT + ASSERT
        // mockMvc.perform(get(...))  ≈  await _client.GetAsync("/api/v1/workers")
        mockMvc.perform(get("/api/v1/workers")
                        .contentType(MediaType.APPLICATION_JSON))
                // .andExpect(status().isOk())  ≈  response.StatusCode.Should().Be(200)
                .andExpect(status().isOk())
                // .andExpect(jsonPath("$[0].fullName"))  ≈  checking JSON response body
                .andExpect(jsonPath("$[0].fullName").value("Ion Popescu"))
                .andExpect(jsonPath("$[0].nationality").value("RO"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /workers returns 401 without auth")
    void getAllWorkers_WithoutAuth_Returns401() throws Exception {
        // No @WithMockUser - unauthenticated request
        mockMvc.perform(get("/api/v1/workers"))
                .andExpect(status().isUnauthorized());  // 401
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/workers/{id}
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /workers/{id} returns 404 when not found")
    @WithMockUser
    void getWorker_WhenNotFound_Returns404() throws Exception {
        // ARRANGE - service throws not found exception
        when(workerService.getWorkerById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Worker", "id", "nonexistent"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/workers/nonexistent"))
                .andExpect(status().isNotFound())  // 404
                // ProblemDetail JSON structure
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").exists());
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/workers
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /workers returns 201 with valid payload")
    @WithMockUser(authorities = "SCOPE_workers.write")  // User with required scope
    void createWorker_WithValidRequest_Returns201() throws Exception {
        // ARRANGE
        var request = WorkerDto.CreateRequest.builder()
                .firstName("Ion")
                .lastName("Popescu")
                .nationalId("RO123456")
                .nationality("RO")
                .email("ion@example.com")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        var response = WorkerDto.Response.builder()
                .id("new-id-123")
                .firstName("Ion")
                .lastName("Popescu")
                .build();

        when(workerService.createWorker(any(WorkerDto.CreateRequest.class)))
                .thenReturn(response);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/workers")
                        .with(csrf())  // Required for Spring Security CSRF (even if disabled, best practice in tests)
                        .contentType(MediaType.APPLICATION_JSON)
                        // objectMapper.writeValueAsString() ≈ JsonSerializer.Serialize() in .NET
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())  // 201
                .andExpect(jsonPath("$.id").value("new-id-123"))
                .andExpect(jsonPath("$.firstName").value("Ion"));
    }

    @Test
    @DisplayName("POST /workers returns 400 with invalid payload (missing required fields)")
    @WithMockUser(authorities = "SCOPE_workers.write")
    void createWorker_WithInvalidRequest_Returns400() throws Exception {
        // ARRANGE - missing required fields
        var invalidRequest = "{ \"firstName\": \"Ion\" }";  // Missing many required fields

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/workers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())  // 400
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /workers returns 403 without required scope")
    @WithMockUser  // Authenticated but no SCOPE_workers.write
    void createWorker_WithoutWriteScope_Returns403() throws Exception {
        var request = WorkerDto.CreateRequest.builder()
                .firstName("Ion").lastName("Popescu")
                .nationalId("RO123456").nationality("RO")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        mockMvc.perform(post("/api/v1/workers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());  // 403
    }
}
