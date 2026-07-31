package com.farm.workers.service;

import com.farm.workers.azure.ServiceBusService;
import com.farm.workers.dto.WorkerDto;
import com.farm.workers.exception.ConflictException;
import com.farm.workers.exception.ResourceNotFoundException;
import com.farm.workers.model.Worker;
import com.farm.workers.repository.WorkerRepository;
import com.farm.workers.util.WorkerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Unit Testing                               ║
 * ║  [Fact] / [Test]              →  @Test                  ║
 * ║  [Theory] / [TestCase]        →  @ParameterizedTest     ║
 * ║  Assert.Equal(exp, act)       →  assertThat(act)        ║
 * ║                                    .isEqualTo(exp)      ║
 * ║  Mock<IWorkerRepo>()          →  @Mock WorkerRepo       ║
 * ║  mock.Setup(...).Returns(...) →  when(...).thenReturn(.)║
 * ║  mock.Verify(...)             →  verify(mock, times(1)) ║
 * ║                                    .method(...)         ║
 * ║  MSTest / xUnit / NUnit       →  JUnit 5               ║
 * ║  Moq / NSubstitute            →  Mockito               ║
 * ║  FluentAssertions             →  AssertJ               ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @ExtendWith(MockitoExtension.class) initializes @Mock and @InjectMocks
 * ≈ [SetUp] AutoMock or manual Mock<T> initialization in .NET
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

    // @InjectMocks creates the class under test and injects all @Mocks
    // ≈  new WorkerService(mockRepo, mockMapper, mockServiceBus)
    @InjectMocks
    private WorkerService workerService;

    private Worker testWorker;
    private WorkerDto.CreateRequest createRequest;

    /**
     * @BeforeEach runs before each test  ≈  [SetUp] or constructor in .NET
     */
    @BeforeEach
    void setUp() {
        testWorker = Worker.builder()
                .id("worker-123")
                .firstName("Ion")
                .lastName("Popescu")
                .nationalId("RO123456")
                .nationality("RO")
                .email("ion.popescu@example.com")
                .status(Worker.WorkerStatus.ACTIVE)
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        createRequest = WorkerDto.CreateRequest.builder()
                .firstName("Ion")
                .lastName("Popescu")
                .nationalId("RO123456")
                .nationality("RO")
                .email("ion.popescu@example.com")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // getWorkerById tests
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getWorkerById returns worker when found")
    void getWorkerById_WhenWorkerExists_ReturnsWorkerResponse() {
        // ARRANGE  ≈  // Arrange in AAA pattern (.NET)
        // when(...).thenReturn(...)  ≈  mockRepo.Setup(r => r.FindById(id)).Returns(worker)
        when(workerRepository.findById("worker-123"))
                .thenReturn(Optional.of(testWorker));

        WorkerDto.Response expectedResponse = WorkerDto.Response.builder()
                .id("worker-123")
                .firstName("Ion")
                .fullName("Ion Popescu")
                .build();

        when(workerMapper.toResponse(testWorker)).thenReturn(expectedResponse);

        // ACT  ≈  // Act
        WorkerDto.Response result = workerService.getWorkerById("worker-123");

        // ASSERT using AssertJ  ≈  FluentAssertions in .NET
        // assertThat(x).isNotNull()  ≈  result.Should().NotBeNull()
        // assertThat(x).isEqualTo(y) ≈  result.Should().Be(y)
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("worker-123");
        assertThat(result.getFullName()).isEqualTo("Ion Popescu");

        // Verify the repository was called exactly once
        // ≈  mockRepo.Verify(r => r.FindByIdAsync("worker-123"), Times.Once())
        verify(workerRepository, times(1)).findById("worker-123");
    }

    @Test
    @DisplayName("getWorkerById throws ResourceNotFoundException when not found")
    void getWorkerById_WhenWorkerNotFound_ThrowsResourceNotFoundException() {
        // ARRANGE
        when(workerRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());  // ≈ returning null / Task<T?> in .NET

        // ASSERT + ACT using assertThatThrownBy
        // ≈  act.Should().Throw<ResourceNotFoundException>() in FluentAssertions
        assertThatThrownBy(() -> workerService.getWorkerById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Worker");

        // Verify mapper was never called (short-circuit on not found)
        verifyNoInteractions(workerMapper);
    }

    // ─────────────────────────────────────────────────────────
    // createWorker tests
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createWorker succeeds with valid request")
    void createWorker_WithValidRequest_ReturnsCreatedWorker() {
        // ARRANGE
        when(workerRepository.existsByNationalId("RO123456")).thenReturn(false);
        when(workerRepository.existsByEmailIgnoreCase("ion.popescu@example.com")).thenReturn(false);
        when(workerMapper.toEntity(createRequest)).thenReturn(testWorker);
        when(workerRepository.save(any(Worker.class))).thenReturn(testWorker);

        WorkerDto.Response expectedResponse = WorkerDto.Response.builder()
                .id("worker-123").firstName("Ion").build();
        when(workerMapper.toResponse(testWorker)).thenReturn(expectedResponse);

        // Don't fail if Service Bus is called
        doNothing().when(serviceBusService).publishWorkerCreatedEvent(any(Worker.class));

        // ACT
        WorkerDto.Response result = workerService.createWorker(createRequest);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("worker-123");

        // Verify the service bus event was published
        verify(serviceBusService, times(1)).publishWorkerCreatedEvent(any(Worker.class));
        verify(workerRepository, times(1)).save(any(Worker.class));
    }

    @Test
    @DisplayName("createWorker throws ConflictException when nationalId already exists")
    void createWorker_WithDuplicateNationalId_ThrowsConflictException() {
        // ARRANGE
        // Simulate a duplicate national ID in the database
        when(workerRepository.existsByNationalId("RO123456")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> workerService.createWorker(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RO123456");

        // Verify save was NEVER called (guard clause worked)
        verify(workerRepository, never()).save(any(Worker.class));

        // Verify Service Bus was NOT called either
        verifyNoInteractions(serviceBusService);
    }

    @Test
    @DisplayName("getAllWorkers returns mapped summaries")
    void getAllWorkers_ReturnsMappedSummaries() {
        // ARRANGE
        Worker worker2 = Worker.builder().id("w2").firstName("Maria").lastName("Ionescu").build();
        when(workerRepository.findAll()).thenReturn(List.of(testWorker, worker2));

        WorkerDto.Summary s1 = WorkerDto.Summary.builder().id("worker-123").fullName("Ion Popescu").build();
        WorkerDto.Summary s2 = WorkerDto.Summary.builder().id("w2").fullName("Maria Ionescu").build();
        when(workerMapper.toSummary(testWorker)).thenReturn(s1);
        when(workerMapper.toSummary(worker2)).thenReturn(s2);

        // ACT
        List<WorkerDto.Summary> result = workerService.getAllWorkers();

        // ASSERT
        // hasSize(2) ≈  result.Should().HaveCount(2) in FluentAssertions
        assertThat(result).hasSize(2);
        assertThat(result).extracting(WorkerDto.Summary::getFullName)
                .containsExactly("Ion Popescu", "Maria Ionescu");
    }

    @Test
    @DisplayName("deactivateWorker sets status to INACTIVE")
    void deactivateWorker_WhenWorkerExists_SetsStatusInactive() {
        // ARRANGE
        when(workerRepository.findById("worker-123")).thenReturn(Optional.of(testWorker));
        when(workerRepository.save(any(Worker.class))).thenReturn(testWorker);

        // ACT
        workerService.deactivateWorker("worker-123");

        // ASSERT - verify the saved worker had INACTIVE status
        // ArgumentCaptor  ≈  Moq Capture / Capture.In<T>() in NSubstitute
        var captor = org.mockito.ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository).save(captor.capture());

        Worker savedWorker = captor.getValue();
        assertThat(savedWorker.getStatus()).isEqualTo(Worker.WorkerStatus.INACTIVE);
    }
}
