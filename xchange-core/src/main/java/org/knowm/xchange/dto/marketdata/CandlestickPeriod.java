package org.knowm.xchange.dto.marketdata;

import java.util.Objects;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Period for Candlesticks in seconds.
 *
 * @author Benno Müller
 */
public class CandlestickPeriod {

    private final long seconds;

    public CandlestickPeriod(final long seconds) {
        this.seconds = seconds;
    }

    public static CandlestickPeriod minutes(final long minutes) {
        return new CandlestickPeriod(MINUTES.toSeconds(minutes));
    }

    public static CandlestickPeriod hours(final long hours) {
        return new CandlestickPeriod(HOURS.toSeconds(hours));
    }

    public static CandlestickPeriod days(final long days) {
        return new CandlestickPeriod(DAYS.toSeconds(days));
    }

    public long getSeconds() {
        return seconds;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CandlestickPeriod that = (CandlestickPeriod) o;
        return seconds == that.seconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seconds);
    }

    @Override
    public String toString() {
        return "CandlestickPeriod [" +
                "seconds=" + seconds +
                ']';
    }
}
