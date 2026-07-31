package com.farm.workers.dto;

import com.farm.workers.model.Worker;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: DTOs                                       ║
 * ║  record WorkerDto(string Name, ...)  →  @Data class     ║
 * ║  [Required] / [MaxLength(100)]       →  @NotBlank       ║
 * ║                                          @Size(max=100) ║
 * ║  Data Annotations                    →  Jakarta Bean    ║
 * ║                                          Validation     ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * DTOs are kept separate from entities (same best practice as .NET)
 * We use nested static classes here to keep all worker DTOs in one file.
 * You could also split them into separate files - team preference.
 */
public class WorkerDto {

    // ─────────────────────────────────────────────────────────
    // REQUEST DTOs (inbound)
    // ─────────────────────────────────────────────────────────

    /**
     * Used for POST /workers (create)
     * ≈ CreateWorkerRequest record in C#
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        private String lastName;

        @NotBlank(message = "National ID is required")
        private String nationalId;

        @NotBlank(message = "Nationality is required")
        private String nationality;

        // @Email  ≈  [EmailAddress] in .NET
        @Email(message = "Must be a valid email address")
        private String email;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phoneNumber;

        // @Past ensures date is in the past  ≈  custom validator in .NET
        @Past(message = "Date of birth must be in the past")
        @NotNull(message = "Date of birth is required")
        private LocalDate dateOfBirth;

        private String emergencyContactName;
        private String emergencyContactPhone;
        private String notes;
    }

    /**
     * Used for PUT /workers/{id} (full update)
     * ≈ UpdateWorkerRequest record in C#
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @NotBlank
        @Size(max = 100)
        private String firstName;

        @NotBlank
        @Size(max = 100)
        private String lastName;

        @Email
        private String email;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
        private String phoneNumber;

        private Worker.WorkerStatus status;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String notes;
    }

    // ─────────────────────────────────────────────────────────
    // RESPONSE DTOs (outbound)
    // ─────────────────────────────────────────────────────────

    /**
     * Used for API responses
     * ≈ WorkerResponse / WorkerViewModel in .NET
     * Note: We never return the full entity - control what's exposed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String firstName;
        private String lastName;
        private String fullName;        // Computed in mapper
        private String nationalId;
        private String nationality;
        private String email;
        private String phoneNumber;
        private LocalDate dateOfBirth;
        private int age;                // Computed in mapper
        private Worker.WorkerStatus status;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String notes;
        private List<Worker.Certification> certifications;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;
    }

    /**
     * Lightweight response for list endpoints
     * ≈ WorkerSummary in .NET - avoid over-fetching
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String id;
        private String fullName;
        private String nationality;
        private String email;
        private Worker.WorkerStatus status;
        private int activeSeasons;
    }

    /**
     * Paginated list response wrapper
     * ≈ PagedResult<T> or PaginatedList<T> in .NET
     *
     * Java generics syntax: List<T>  ≈  List<T> in C# (same concept!)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse {
        private List<Summary> items;
        private long totalCount;
        private int pageNumber;
        private int pageSize;
        private int totalPages;
        private boolean hasNextPage;
        private boolean hasPreviousPage;
    }
}
