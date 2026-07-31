package com.farm.workers.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import com.farm.workers.model.Worker;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Repository Pattern                         ║
 * ║  IRepository<Worker> / DbSet<Worker>  →  CosmosRepository║
 * ║  _context.Workers.FindAsync(id)       →  findById(id)   ║
 * ║  _context.Workers.AddAsync(w)         →  save(w)        ║
 * ║  _context.Workers.Remove(w)           →  delete(w)      ║
 * ║  LINQ queries                         →  @Query (SQL-like)║
 * ║                                          or method names ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Spring Data automatically implements this interface at runtime.
 * You define the interface, Spring generates the implementation.
 * ≈ EF Core DbContext but you don't write the implementation.
 *
 * CosmosRepository<Worker, String>:
 *   - Worker = entity type
 *   - String = ID type (we use String UUIDs for CosmosDB)
 *
 * Method naming convention generates queries automatically:
 * findByNationality(String n) → SELECT * FROM c WHERE c.nationality = @n
 * findByStatusAndNationality(...) → WHERE status = ? AND nationality = ?
 */
@Repository  // ≈ [Repository] attribute or just marking it as a data layer component
public interface WorkerRepository extends CosmosRepository<Worker, String> {

    /**
     * Spring Data derives the query from the method name.
     * findBy[FieldName] → generates CosmosDB SQL automatically
     * ≈ _context.Workers.Where(w => w.nationality == nationality)
     */
    List<Worker> findByNationality(String nationality);

    /**
     * Optional<T> ≈ nullable reference type or Task<T?> in C#
     * Forces caller to handle the "not found" case explicitly
     */
    Optional<Worker> findByNationalId(String nationalId);

    /**
     * findByEmailIgnoreCase adds case-insensitive comparison
     * ≈ .Where(w => w.Email.ToLower() == email.ToLower())
     */
    Optional<Worker> findByEmailIgnoreCase(String email);

    List<Worker> findByStatus(Worker.WorkerStatus status);

    /**
     * @Query uses CosmosDB SQL syntax (similar to SQL, runs against JSON)
     * @param0 = first parameter, @param1 = second, etc.
     * ≈ FromSqlRaw() or a LINQ expression in EF Core
     */
    @Query("SELECT * FROM workers w WHERE CONTAINS(LOWER(w.firstName), LOWER(@param0)) " +
           "OR CONTAINS(LOWER(w.lastName), LOWER(@param0))")
    List<Worker> searchByName(String searchTerm);

    /**
     * exists* methods return boolean - very efficient (no data transfer)
     * ≈ _context.Workers.AnyAsync(w => w.nationalId == id)
     */
    boolean existsByNationalId(String nationalId);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * countBy* methods return long
     * ≈ _context.Workers.CountAsync(w => w.nationality == n)
     */
    long countByNationality(String nationality);

    long countByStatus(Worker.WorkerStatus status);
}
