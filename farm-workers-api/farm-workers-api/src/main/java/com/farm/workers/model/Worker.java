package com.farm.workers.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.CosmosUniqueKey;
import com.azure.spring.data.cosmos.core.mapping.CosmosUniqueKeyPolicy;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Domain Model / Entity                      ║
 * ║  [Table("workers")] / EF entity  →  @Container          ║
 * ║  [Key] / int Id                  →  @Id (String uuid)   ║
 * ║  public string Name { get; set; }→  @Data (Lombok)       ║
 * ║  new Worker { Name = "..." }     →  Worker.builder()     ║
 * ║                                      .name("...")        ║
 * ║                                      .build()           ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Lombok annotations explained:
 * @Data        = generates getters, setters, equals, hashCode, toString
 * @Builder     = generates fluent builder pattern
 * @NoArgsConstructor = generates default constructor (needed by CosmosDB)
 * @AllArgsConstructor = generates constructor with all fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "workers")   // CosmosDB container name
@CosmosUniqueKeyPolicy(uniqueKeys = {
    @CosmosUniqueKey(paths = {"/nationalId"})
})
public class Worker {

    /**
     * @Id maps to CosmosDB document "id" field
     * Java uses String UUIDs for NoSQL (vs int/Guid in .NET EF)
     * Set in service layer: UUID.randomUUID().toString()
     */
    @Id
    private String id;

    /**
     * @PartitionKey is CRITICAL in CosmosDB for performance
     * ≈ choosing the right partition strategy in .NET CosmosDB SDK
     * We partition by nationality to distribute workers geographically
     */
    @PartitionKey
    private String nationality;

    private String firstName;
    private String lastName;
    private String nationalId;          // National ID / passport number
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private WorkerStatus status;        // enum (see below)
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String notes;

    // Embedded document (CosmosDB strength - no JOIN needed!)
    // ≈ [Owned] entities in EF Core, or navigation property stored inline
    private List<Certification> certifications;

    private String createdBy;           // From JWT claims (Azure Entra ID)
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;

    /**
     * Inner enum  ≈  enum in C#
     * Java enums are full classes and can have methods/fields
     */
    public enum WorkerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION
    }

    /**
     * Embedded document class  ≈  [Owned] complex type in EF Core
     * Stored as nested JSON inside the worker document in CosmosDB
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certification {
        private String certificationName;   // e.g., "Pesticide Handling"
        private String issuingBody;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private boolean isValid;
    }
}
