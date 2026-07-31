package com.farm.workers.controller;

import com.farm.workers.dto.WorkerDto;
import com.farm.workers.model.Worker;
import com.farm.workers.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Web API Controller                         ║
 * ║  [ApiController]         →  @RestController             ║
 * ║  [Route("api/workers")]  →  @RequestMapping(...)        ║
 * ║  [HttpGet]               →  @GetMapping                 ║
 * ║  [HttpPost]              →  @PostMapping                ║
 * ║  [HttpPut("{id}")]       →  @PutMapping("/{id}")        ║
 * ║  [HttpDelete("{id}")]    →  @DeleteMapping("/{id}")     ║
 * ║  [FromBody] WorkerDto r  →  @RequestBody WorkerDto r    ║
 * ║  [FromRoute] string id   →  @PathVariable String id     ║
 * ║  [FromQuery] string q    →  @RequestParam String q      ║
 * ║  IActionResult           →  ResponseEntity<T>           ║
 * ║  Ok(result)              →  ResponseEntity.ok(result)   ║
 * ║  CreatedAtAction(...)    →  ResponseEntity.created(...) ║
 * ║  NotFound()              →  ResponseEntity.notFound()   ║
 * ║  [Authorize]             →  @PreAuthorize("isAuth()..") ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @RestController = @Controller + @ResponseBody on every method
 * (returns JSON automatically for every method, just like [ApiController])
 *
 * ResponseEntity<T> = typed wrapper that lets you set status code + headers
 * (more explicit than just returning T, similar to IActionResult in .NET)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workers")  // ≈ [Route("api/v1/workers")]
@RequiredArgsConstructor
@Tag(name = "Workers", description = "Farm seasonal workers management")
@SecurityRequirement(name = "bearerAuth")  // Swagger UI shows "Authorize" button
public class WorkerController {

    private final WorkerService workerService;

    /**
     * GET /api/v1/workers
     * ≈ [HttpGet] public IActionResult GetAll()
     *
     * @PreAuthorize = [Authorize] attribute in .NET
     * "isAuthenticated()" = any valid JWT token required
     * "hasAuthority('SCOPE_workers.read')" = specific scope/role required
     */
    @GetMapping
    @Operation(summary = "List all workers", description = "Returns a summary list of all farm workers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkerDto.Summary>> getAllWorkers(
            @RequestParam(required = false) String search,          // ≈ [FromQuery] string? search
            @RequestParam(required = false) Worker.WorkerStatus status) {

        List<WorkerDto.Summary> workers;

        if (search != null && !search.isBlank()) {
            workers = workerService.searchWorkers(search);
        } else if (status != null) {
            workers = workerService.getWorkersByStatus(status);
        } else {
            workers = workerService.getAllWorkers();
        }

        // ResponseEntity.ok() = 200 OK with body
        // ≈ return Ok(workers);
        return ResponseEntity.ok(workers);
    }

    /**
     * GET /api/v1/workers/{id}
     * ≈ [HttpGet("{id}")] public IActionResult GetById(string id)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get worker by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkerDto.Response> getWorker(
            @PathVariable String id) {  // ≈ [FromRoute] string id

        // ResourceNotFoundException is caught by GlobalExceptionHandler
        // and converted to 404 automatically (≈ ProblemDetails in .NET)
        WorkerDto.Response worker = workerService.getWorkerById(id);
        return ResponseEntity.ok(worker);
    }

    /**
     * POST /api/v1/workers
     * ≈ [HttpPost] public IActionResult Create([FromBody] CreateWorkerRequest request)
     *
     * @Valid triggers Jakarta Bean Validation (≈ ModelState.IsValid in .NET)
     * If validation fails, returns 400 with error details automatically
     */
    @PostMapping
    @Operation(summary = "Register a new farm worker")
    @PreAuthorize("hasAuthority('SCOPE_workers.write')")  // Requires specific Entra ID app role
    public ResponseEntity<WorkerDto.Response> createWorker(
            @Valid @RequestBody WorkerDto.CreateRequest request) {  // ≈ [FromBody]

        WorkerDto.Response created = workerService.createWorker(request);

        // 201 Created with Location header  ≈  CreatedAtAction() in .NET
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    /**
     * PUT /api/v1/workers/{id}
     * ≈ [HttpPut("{id}")] public IActionResult Update(string id, [FromBody] UpdateRequest req)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update worker details")
    @PreAuthorize("hasAuthority('SCOPE_workers.write')")
    public ResponseEntity<WorkerDto.Response> updateWorker(
            @PathVariable String id,
            @Valid @RequestBody WorkerDto.UpdateRequest request) {

        WorkerDto.Response updated = workerService.updateWorker(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/workers/{id}
     * Note: This is a soft-delete (status change), not physical delete
     * ≈ [HttpDelete("{id}")] public IActionResult Deactivate(string id)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a worker (soft delete)")
    @PreAuthorize("hasAuthority('SCOPE_workers.admin')")  // Admin role required
    public ResponseEntity<Void> deactivateWorker(
            @PathVariable String id) {

        workerService.deactivateWorker(id);

        // 204 No Content  ≈  return NoContent();
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/workers/nationality/{nationality}
     * Custom route example
     */
    @GetMapping("/nationality/{nationality}")
    @Operation(summary = "Get workers by nationality")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkerDto.Summary>> getByNationality(
            @PathVariable @Parameter(description = "ISO country code e.g. RO, BG, PL") String nationality) {

        List<WorkerDto.Summary> workers = workerService.getWorkersByStatus(Worker.WorkerStatus.ACTIVE);
        // Note: in real app you'd call a dedicated service method
        return ResponseEntity.ok(workers);
    }
}
