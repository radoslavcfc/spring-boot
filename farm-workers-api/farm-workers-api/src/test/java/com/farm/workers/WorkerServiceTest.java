package com.farm.workers.service;

import com.farm.workers.dto.WorkerDto;
import com.farm.workers.exception.ConflictException;
import com.farm.workers.exception.ResourceNotFoundException;
import com.farm.workers.model.Worker;
import com.farm.workers.repository.WorkerRepository;
import com.farm.workers.util.WorkerMapper;
import com.farm.workers.azure.ServiceBusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Unit Testing                               ║
 * ║  xUnit / MSTest + Moq  →  JUnit 5 + Mockito             ║
 * ║                                                          ║
 * ║  [Fact] / [Test]         →  @Test                       ║
 * ║  [Theory]                →  @ParameterizedTest          ║
 * ║  Mock<IWorkerRepo>()     →  @Mock WorkerRepository      ║
 * ║  mock.Setup(r => r.X())  →  given(mock.x()).willReturn()║
 * ║  mock.Verify(r => r.X()) →  then(mock).should().x()    ║
 * ║  Assert.Equal(a, b)      →  assertThat(a).isEqualTo(b) ║
 * ║  Assert.Throws<Ex>(...)  →  assertThatThrownBy(...)     ║
 * ║                              .isInstanceOf(Ex.class)   ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @ExtendWith(MockitoExtension.class) = Mockito JUnit 5 integration
 * ≈ [AutoMock] attribute or [InlineAutoData] in .NET
 *
 * AssertJ (assertThat) is more fluent than JUnit's assertions
 * ≈ FluentAssertions library in .NET
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerService Tests")
class WorkerServiceTest {

    // @Mock creates a Mockito mock  ≈  new Mock<IWorkerRepository>() in Moq
    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerMapper workerMapper;

    @Mock
    private ServiceBusService serviceBusService;

    // @InjectMocks creates the class under test and injects all @Mock fields
    // ≈  new WorkerService(mockRepo, mockMapper, mockServiceBus) but automatic
    @InjectMocks
    private WorkerService workerService;

    // Test data builders  ≈  AutoFixture or manual test builders in .NET
    private Worker testWorker;
    private WorkerDto.CreateRequest createRequest;
    private WorkerDto.Response workerResponse;

    @BeforeEach  // ≈ [SetUp] in NUnit or constructor-based setup in xUnit
    void setUp() {
        testWorker = Worker.builder()
                .id("worker-123")
                .firstName("Ion")
                .lastName("Popescu")
                .nationalId("1234567890")
                .nationality("RO")
                .email("ion.popescu@email.com")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .status(Worker.WorkerStatus.ACTIVE)
                .build();

        createRequest = WorkerDto.CreateRequest.builder()
                .firstName("Ion")
                .lastName("Popescu")
                .nationalId("1234567890")
                .nationality("RO")
                .email("ion.popescu@email.com")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .build();

        workerResponse = WorkerDto.Response.builder()
                .id("worker-123")
                .firstName("Ion")
                .lastName("Popescu")
                .fullName("Ion Popescu")
                .status(Worker.WorkerStatus.PENDING_VERIFICATION)
                .build();
    }

    // ─── Nested test classes group related tests ──────────────
    // ≈ inner classes or regions in .NET test files

    @Nested
    @DisplayName("getWorkerById")
    class GetWorkerById {

        @Test
        @DisplayName("should return worker when found")
        void shouldReturnWorkerWhenFound() {
            // Arrange  ─  given() = Mockito BDD style
            // ≈ mockRepo.Setup(r => r.FindAsync("worker-123")).ReturnsAsync(testWorker)
            given(workerRepository.findById("worker-123"))
                    .willReturn(Optional.of(testWorker));
            given(workerMapper.toResponse(testWorker))
                    .willReturn(workerResponse);

            // Act
            WorkerDto.Response result = workerService.getWorkerById("worker-123");

            // Assert  ─  assertThat() = AssertJ fluent assertions
            // ≈ result.Should().NotBeNull().And.Subject.Id.Should().Be("worker-123")
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("worker-123");
            assertThat(result.getFullName()).isEqualTo("Ion Popescu");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // Arrange
            given(workerRepository.findById("nonexistent"))
                    .willReturn(Optional.empty());

            // Assert  ─  assertThatThrownBy
            // ≈ Assert.Throws<ResourceNotFoundException>(() => service.GetById("nonexistent"))
            assertThatThrownBy(() -> workerService.getWorkerById("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Worker")
                    .hasMessageContaining("nonexistent");
        }
    }

    @Nested
    @DisplayName("createWorker")
    class CreateWorker {

        @Test
        @DisplayName("should create worker successfully")
        void shouldCreateWorkerSuccessfully() {
            // Arrange
            given(workerRepository.existsByNationalId("1234567890")).willReturn(false);
            given(workerRepository.existsByEmailIgnoreCase("ion.popescu@email.com")).willReturn(false);
            given(workerMapper.toEntity(createRequest)).willReturn(testWorker);
            given(workerRepository.save(any(Worker.class))).willReturn(testWorker);
            given(workerMapper.toResponse(testWorker)).willReturn(workerResponse);

            // Void mocks don't need stubbing in Mockito (unlike Moq which needs .Setup)
            // serviceBusService.publishWorkerCreatedEvent() is a void method - no setup needed

            // Act
            WorkerDto.Response result = workerService.createWorker(createRequest);

            // Assert
            assertThat(result).isNotNull();

            // Verify interactions  ≈  mockRepo.Verify(r => r.SaveAsync(), Times.Once())
            then(workerRepository).should(times(1)).save(any(Worker.class));
            then(serviceBusService).should(times(1)).publishWorkerCreatedEvent(any(Worker.class));
        }

        @Test
        @DisplayName("should throw ConflictException when national ID already exists")
        void shouldThrowConflictWhenNationalIdExists() {
            // Arrange
            given(workerRepository.existsByNationalId("1234567890")).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> workerService.createWorker(createRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("1234567890");

            // Verify save was NEVER called  ≈  mockRepo.Verify(r => r.SaveAsync(), Times.Never())
            then(workerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw ConflictException when email already exists")
        void shouldThrowConflictWhenEmailExists() {
            given(workerRepository.existsByNationalId(anyString())).willReturn(false);
            given(workerRepository.existsByEmailIgnoreCase("ion.popescu@email.com")).willReturn(true);

            assertThatThrownBy(() -> workerService.createWorker(createRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("email");
        }
    }

    @Nested
    @DisplayName("getAllWorkers")
    class GetAllWorkers {

        @Test
        @DisplayName("should return mapped summaries for all workers")
        void shouldReturnMappedSummaries() {
            // Arrange
            Worker worker2 = Worker.builder().id("worker-456").firstName("Ana").build();
            given(workerRepository.findAll()).willReturn(List.of(testWorker, worker2));

            WorkerDto.Summary summary1 = WorkerDto.Summary.builder().id("worker-123").fullName("Ion Popescu").build();
            WorkerDto.Summary summary2 = WorkerDto.Summary.builder().id("worker-456").fullName("Ana").build();
            given(workerMapper.toSummary(testWorker)).willReturn(summary1);
            given(workerMapper.toSummary(worker2)).willReturn(summary2);

            // Act
            List<WorkerDto.Summary> results = workerService.getAllWorkers();

            // Assert
            // ≈ results.Should().HaveCount(2).And.Contain(s => s.Id == "worker-123")
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WorkerDto.Summary::getId)
                    .containsExactlyInAnyOrder("worker-123", "worker-456");
        }
    }
}
