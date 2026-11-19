package com.library.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimeProviderTest {

    @Test
    @DisplayName("TimeProvider returns predictable instant and date from fixed clock")
    void shouldReturnPredictableTimeFromClock() {
        Instant fixedInstant = Instant.parse("2026-01-15T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        TimeProvider timeProvider = new TimeProvider(fixedClock);

        assertThat(timeProvider.now()).isEqualTo(fixedInstant);
        assertThat(timeProvider.today()).isEqualTo(LocalDate.of(2026, 1, 15));
    }
}
