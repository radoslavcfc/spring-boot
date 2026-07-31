package com.farm.workers.service;

import com.farm.workers.azure.ServiceBusService;
import com.farm.workers.dto.WorkerDto;
import com.farm.workers.exception.ConflictException;
import com.farm.workers.exception.ResourceNotFoundException;
import com.farm.workers.model.Worker;
import com.farm.workers.repository.WorkerRepository;
import com.farm.workers.util.WorkerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Service Layer                              ║
 * ║  [ApiController]              →  @RestController        ║
 * ║  Business logic in controller →  @Service class         ║
 * ║  constructor injection        →  @RequiredArgsConstructor║
 * ║   (same pattern, different    →  (Lombok generates the  ║
 * ║    annotation)                     constructor)         ║
 * ║  ILogger<WorkerService>       →  @Slf4j (Lombok)        ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @Service = @Component + semantic meaning (business logic layer)
 *
 * @RequiredArgsConstructor = Lombok generates a constructor for all
 * `final` fields. Spring sees ONE constructor and auto-injects all deps.
 * ≈ Constructor injection in ASP.NET Core DI
 *
 * @Slf4j = Lombok injects `private static final Logger log = ...`
 * Usage: log.info("..."), log.error("...", exception)
 * ≈ ILogger<T> injected via constructor in .NET
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    // All fields are final = immutable after construction = thread-safe
    // ≈ readonly in C#
    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;
    private final ServiceBusService serviceBusService;

    /**
     * Get all workers (summary list)
     * Returns lightweight DTOs - never return entities from API
     */
    public List<WorkerDto.Summary> getAllWorkers() {
        log.info("Fetching all workers");

        // stream()  ≈  IEnumerable / LINQ in C#
        // .map()    ≈  .Select()
        // .collect(Collectors.toList())  ≈  .ToList()
        return workerRepository.findAll()
                .stream()
                .map(workerMapper::toSummary)  // method reference  ≈  w => mapper.ToSummary(w)
                .collect(Collectors.toList());
    }

    /**
     * Get single worker by ID
     *
     * Optional<T>: represents a value that may or may not be present
     * .orElseThrow() = throw exception if empty  ≈  ?? throw new NotFoundException()
     */
    public WorkerDto.Response getWorkerById(String id) {
        log.info("Fetching worker with id: {}", id);  // {} is SLF4J placeholder  ≈  {0} or $"" in C#

        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker", "id", id));

        return workerMapper.toResponse(worker);
    }

    /**
     * Create a new worker
     *
     * Key Java patterns here:
     * - UUID.randomUUID()   ≈  Guid.NewGuid()
     * - Instant.now()       ≈  DateTime.UtcNow
     * - SecurityContextHolder ≈ HttpContext.User in .NET
     */
    public WorkerDto.Response createWorker(WorkerDto.CreateRequest request) {
        log.info("Creating worker with nationalId: {}", request.getNationalId());

        // Uniqueness check - ≈ checking DbUpdateException or explicit validation
        if (workerRepository.existsByNationalId(request.getNationalId())) {
            throw new ConflictException("Worker with national ID already exists: " + request.getNationalId());
        }

        if (request.getEmail() != null && workerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Worker with email already exists: " + request.getEmail());
        }

        // Map DTO → Entity
        Worker worker = workerMapper.toEntity(request);

        // Set fields that aren't mapped (service-layer concerns)
        worker.setId(UUID.randomUUID().toString());  // ≈ Guid.NewGuid().ToString()
        worker.setCreatedAt(Instant.now());
        worker.setUpdatedAt(Instant.now());
        worker.setCreatedBy(getCurrentUserId());     // From JWT token

        Worker saved = workerRepository.save(worker);
        log.info("Created worker with id: {}", saved.getId());

        // Publish event to Azure Service Bus (async notification)
        // ≈ publishing a domain event or calling MediatR
        serviceBusService.publishWorkerCreatedEvent(saved);

        return workerMapper.toResponse(saved);
    }

    /**
     * Update an existing worker
     *
     * Java approach: load → mutate → save
     * ≈ _context.Entry(worker).State = EntityState.Modified
     */
    public WorkerDto.Response updateWorker(String id, WorkerDto.UpdateRequest request) {
        log.info("Updating worker: {}", id);

        Worker existing = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker", "id", id));

        // MapStruct updates existing object in-place
        workerMapper.updateEntityFromRequest(request, existing);
        existing.setUpdatedAt(Instant.now());

        Worker saved = workerRepository.save(existing);
        return workerMapper.toResponse(saved);
    }

    /**
     * Soft delete by changing status
     * In NoSQL, physical delete is often replaced by status flags
     */
    public void deactivateWorker(String id) {
        log.info("Deactivating worker: {}", id);

        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker", "id", id));

        worker.setStatus(Worker.WorkerStatus.INACTIVE);
        worker.setUpdatedAt(Instant.now());
        workerRepository.save(worker);

        log.info("Worker {} deactivated", id);
    }

    /**
     * Search workers by name
     */
    public List<WorkerDto.Summary> searchWorkers(String searchTerm) {
        return workerRepository.searchByName(searchTerm)
                .stream()
                .map(workerMapper::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Filter by status
     */
    public List<WorkerDto.Summary> getWorkersByStatus(Worker.WorkerStatus status) {
        return workerRepository.findByStatus(status)
                .stream()
                .map(workerMapper::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get current user ID from JWT token (set by Spring Security / Azure Entra ID)
     *
     * ≈ User.FindFirstValue(ClaimTypes.NameIdentifier) in .NET
     *
     * SecurityContextHolder is Spring's thread-local security context
     * (automatically populated by the JWT filter on each request)
     */
    private String getCurrentUserId() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null ? authentication.getName() : "system";
        } catch (Exception e) {
            log.warn("Could not extract user from security context", e);
            return "system";
        }
    }
}
