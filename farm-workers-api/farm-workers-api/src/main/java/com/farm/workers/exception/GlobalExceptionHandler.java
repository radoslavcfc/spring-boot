package com.farm.workers.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Global Exception Handling                  ║
 * ║                                                          ║
 * ║  .NET approach:                                          ║
 * ║  app.UseExceptionHandler(...)                           ║
 * ║  services.AddProblemDetails()                           ║
 * ║  throw new NotFoundException("Worker not found")        ║
 * ║    → automatically returns 404 ProblemDetails JSON      ║
 * ║                                                          ║
 * ║  Java approach:                                          ║
 * ║  @RestControllerAdvice class (this file)                ║
 * ║  @ExceptionHandler methods map exceptions → responses   ║
 * ║  Spring 6 supports ProblemDetail (RFC 7807) natively!  ║
 * ║  (same standard as .NET ProblemDetails)                 ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Intercepts exceptions thrown anywhere in @RestController classes
 * ≈ a global exception filter / middleware in ASP.NET Core
 *
 * ProblemDetail = Spring 6's implementation of RFC 7807
 * Same JSON format as .NET's ProblemDetails:
 * {
 *   "type": "https://...",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "Worker not found",
 *   "instance": "/api/v1/workers/abc"
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException → 404 Not Found
     * ≈ catching NotFoundException and returning NotFound() in .NET
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://farm-api.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Handle ConflictException → 409 Conflict
     * ≈ returning Conflict() for duplicate resource
     */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, WebRequest request) {
        log.warn("Conflict: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("https://farm-api.com/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Handle BusinessRuleException → 422 Unprocessable Entity
     * For business logic violations (not validation errors)
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setType(URI.create("https://farm-api.com/errors/business-rule"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Handle @Valid validation failures → 400 Bad Request with field errors
     *
     * ≈ ModelState.IsValid = false in .NET
     * Returns a map of field → error message pairs
     *
     * Example response:
     * {
     *   "status": 400,
     *   "title": "Validation Failed",
     *   "errors": {
     *     "email": "Must be a valid email address",
     *     "firstName": "First name is required"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect all field errors into a map  ≈  ModelState.Values in .NET
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("https://farm-api.com/errors/validation"));
        problem.setProperty("errors", errors);  // Include field-level errors
        problem.setProperty("timestamp", Instant.now());

        log.debug("Validation failed: {}", errors);
        return problem;
    }

    /**
     * Handle authorization failures → 403 Forbidden
     * ≈ [Authorize(Policy = "...")] failing → 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://farm-api.com/errors/forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Catch-all handler → 500 Internal Server Error
     * ≈ the final catch in UseExceptionHandler middleware
     * NEVER expose internal exception details in production
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        // Log full stack trace internally  ≈  ILogger.LogError(ex, ...)
        log.error("Unexpected error processing request: {}", request.getDescription(false), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://farm-api.com/errors/internal"));
        problem.setProperty("timestamp", Instant.now());
        // DO NOT include ex.getMessage() - may leak sensitive info
        return problem;
    }
}
