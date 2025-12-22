package com.onlineshop.order.saga;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.repository.SagaStateRepository;
import com.onlineshop.order.saga.compensation.CompensationHandler;
import com.onlineshop.order.saga.compensation.CompensationService;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CompensationServiceTest {

    @Mock
    private CompensationHandler compensationHandler;

    @Mock
    private SagaStateRepository sagaStateRepository;

    @InjectMocks
    private CompensationService compensationService;

    @Captor
    private ArgumentCaptor<SagaState> sagaStateCaptor;

    private Order testOrder;
    private SagaState testSagaState;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORDER-TEST-01")
                .customerId(123L)
                .totalAmount(BigDecimal.valueOf(100.00))
                .shippingAddress("Test Address")
                .build();

        testSagaState = SagaState.builder()
                .id(1L)
                .order(testOrder)
                .status(SagaStatus.FAILED)
                .build();
    }

    @Test
    void testCompensateAfterFailure_Success() {
        // Given
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        // When
        compensationService.compensateAfterFailure(testOrder);

        // Then
        verify(compensationHandler).executeCompensation(testOrder);
        verify(sagaStateRepository).save(sagaStateCaptor.capture());
        
        SagaState savedState = sagaStateCaptor.getValue();
        assertEquals(SagaStatus.COMPENSATED, savedState.getStatus());
        assertNull(savedState.getErrorMessage());
    }

    @Test
    void testCompensateAfterFailure_WithException() {
        // Given
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));
        doThrow(new RuntimeException("Compensation failed")).when(compensationHandler).executeCompensation(testOrder);

        // When
        compensationService.compensateAfterFailure(testOrder);

        // Then
        verify(compensationHandler).executeCompensation(testOrder);
        verify(sagaStateRepository).save(sagaStateCaptor.capture());
        
        SagaState savedState = sagaStateCaptor.getValue();
        assertEquals(SagaStatus.COMPENSATION_FAILED, savedState.getStatus());
        assertTrue(savedState.getErrorMessage().contains("Compensation failed"));
    }

    @Test
    void testCompensateAfterFailure_SagaStateNotFound() {
        // Given
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            compensationService.compensateAfterFailure(testOrder);
        });
        
        assertTrue(exception.getMessage().contains("Saga state not found"));
        verify(compensationHandler, never()).executeCompensation(any());
        verify(sagaStateRepository, never()).save(any());
    }

    @Test
    void testCompensateAfterFailure_WithSpecificError() {
        // Given
        testSagaState.setErrorMessage("Original failure message");
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));
        doThrow(new IllegalArgumentException("Invalid compensation")).when(compensationHandler).executeCompensation(testOrder);

        // When
        compensationService.compensateAfterFailure(testOrder);

        // Then
        verify(compensationHandler).executeCompensation(testOrder);
        verify(sagaStateRepository).save(sagaStateCaptor.capture());
        
        SagaState savedState = sagaStateCaptor.getValue();
        assertEquals(SagaStatus.COMPENSATION_FAILED, savedState.getStatus());
        assertTrue(savedState.getErrorMessage().contains("Compensation failed"));
        assertTrue(savedState.getErrorMessage().contains("Invalid compensation"));
    }

    @Test
    void testCompensateAfterFailure_MultipleExecutions() {
        // Given
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        // When
        compensationService.compensateAfterFailure(testOrder);
        compensationService.compensateAfterFailure(testOrder);

        // Then
        verify(compensationHandler, times(2)).executeCompensation(testOrder);
        verify(sagaStateRepository, times(2)).save(any());
    }

    @Test
    void testCompensateAfterFailure_WithDifferentSagaStates() {
        // Given different saga states
        SagaState completedState = SagaState.builder()
                .id(2L)
                .order(testOrder)
                .status(SagaStatus.COMPLETED)
                .build();
                
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(completedState));

        // When
        compensationService.compensateAfterFailure(testOrder);

        // Then
        verify(compensationHandler).executeCompensation(testOrder);
        verify(sagaStateRepository).save(sagaStateCaptor.capture());
        
        SagaState savedState = sagaStateCaptor.getValue();
        assertEquals(SagaStatus.COMPENSATED, savedState.getStatus());
    }
}
