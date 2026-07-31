package com.farm.workers.util;

import com.farm.workers.dto.WorkRecordDto;
import com.farm.workers.dto.WorkerDto;
import com.farm.workers.model.Worker;
import com.farm.workers.model.WorkRecord;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.Period;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Object Mapping                             ║
 * ║  AutoMapper Profile        →  @Mapper (MapStruct)        ║
 * ║  CreateMap<Src, Dest>()    →  @Mapping annotations       ║
 * ║  .ForMember(d => d.X, ...) →  @Mapping(target="x", ...) ║
 * ║  _mapper.Map<DestType>(src) →  workerMapper.toResponse() ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * MapStruct generates the implementation at COMPILE TIME (not runtime).
 * This means:
 * - Type-safe (errors caught at compile time, not runtime)
 * - Fast (no reflection, just generated Java code)
 * - Debuggable (you can read the generated code in target/classes)
 *
 * componentModel = "spring" → generated class gets @Component
 * so Spring can inject it with @Autowired / constructor injection
 *
 * ≈ services.AddAutoMapper(typeof(WorkerProfile)) in .NET startup
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    // IGNORE = don't overwrite target fields when source is null
    // ≈ AutoMapper's .ForAllMembers(opt => opt.Condition(src => src != null))
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface WorkerMapper {

    /**
     * Maps CreateRequest → Worker entity
     *
     * @Mapping(target = "x", ignore = true) - skip this field (we set it in service)
     * @Mapping(target = "x", expression = "java(...)") - computed value
     */
    @Mapping(target = "id", ignore = true)          // Generated in service layer
    @Mapping(target = "status", constant = "PENDING_VERIFICATION")  // Default status
    @Mapping(target = "createdAt", ignore = true)   // Set in service
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "certifications", ignore = true)
    Worker toEntity(WorkerDto.CreateRequest request);

    /**
     * Maps Worker entity → Response DTO
     * Note computed fields: fullName, age
     */
    @Mapping(target = "fullName",
             expression = "java(worker.getFirstName() + \" \" + worker.getLastName())")
    @Mapping(target = "age",
             expression = "java(calculateAge(worker.getDateOfBirth()))")
    WorkerDto.Response toResponse(Worker worker);

    /**
     * Maps Worker → lightweight Summary DTO
     */
    @Mapping(target = "fullName",
             expression = "java(worker.getFirstName() + \" \" + worker.getLastName())")
    @Mapping(target = "activeSeasons", ignore = true)  // Populated separately if needed
    WorkerDto.Summary toSummary(Worker worker);

    /**
     * Updates an existing entity from UpdateRequest - partial update pattern
     * ≈ _mapper.Map(updateRequest, existingWorker) in AutoMapper
     *
     * @MappingTarget tells MapStruct to update the existing object, not create new one
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nationalId", ignore = true)
    @Mapping(target = "nationality", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "certifications", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(WorkerDto.UpdateRequest request, @MappingTarget Worker worker);

    // ─── WorkRecord mappings ──────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workerName", ignore = true)  // Set in service after looking up worker
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "totalPay", ignore = true)    // Calculated in service
    @Mapping(target = "supervisorId", ignore = true)
    @Mapping(target = "paymentProcessed", constant = "false")
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WorkRecord toEntity(WorkRecordDto.CreateRequest request);

    WorkRecordDto.Response toResponse(WorkRecord workRecord);

    /**
     * Default method  ≈  extension method / default interface method in C#
     * MapStruct calls this when you use expression = "java(calculateAge(...))"
     */
    default int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return 0;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
