package com.orderflow.order.invoice.services;

import com.orderflow.order.invoice.entities.InvoiceCounter;
import com.orderflow.order.invoice.repositories.InvoiceCounterRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvoiceNumberGeneratorTest {

    @Test
    void financialYearRunsAprilToMarch() {
        assertThat(InvoiceNumberGenerator.financialYear(LocalDate.of(2026, 11, 22))).isEqualTo("2026-27");
        assertThat(InvoiceNumberGenerator.financialYear(LocalDate.of(2027, 3, 31))).isEqualTo("2026-27");
        assertThat(InvoiceNumberGenerator.financialYear(LocalDate.of(2027, 4, 1))).isEqualTo("2027-28");
        assertThat(InvoiceNumberGenerator.financialYear(LocalDate.of(2026, 1, 1))).isEqualTo("2025-26");
        assertThat(InvoiceNumberGenerator.financialYear(LocalDate.of(2099, 4, 1))).isEqualTo("2099-00");
    }

    @Test
    void numberIsBuiltFromCounterAndZeroPadded() {
        InvoiceCounterRepository counters = mock(InvoiceCounterRepository.class);
        InvoiceCounter counter = mock(InvoiceCounter.class);
        when(counter.next()).thenReturn(42L);
        when(counters.lockByFinancialYear("2026-27")).thenReturn(Optional.of(counter));

        String number = new InvoiceNumberGenerator(counters).next(LocalDate.of(2026, 11, 22));

        assertThat(number).isEqualTo("INV-2026-27-000042");
        verify(counters).createIfMissing("2026-27");
    }
}
