package org.knowm.xchange.bitfinex.v2.dto.marketdata;

import org.knowm.xchange.dto.marketdata.CandlestickPeriod;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Benno Müller
 */
public enum TimeFrame {

    m1("1m", MINUTES.toSeconds(1)),
    m5("1m", MINUTES.toSeconds(1)),
    m15("1m", MINUTES.toSeconds(1)),
    m30("1m", MINUTES.toSeconds(1)),
    h1("1h", HOURS.toSeconds(1)),
    h3("1h", HOURS.toSeconds(1)),
    h6("1h", HOURS.toSeconds(1)),
    h12("1h", HOURS.toSeconds(1)),
    D1("1D", DAYS.toSeconds(1)),
    D7("1D", DAYS.toSeconds(1)),
    D14("1D", DAYS.toSeconds(1)),
    M1("1M", DAYS.toSeconds(30));

    private final String code;
    private final long seconds;

    TimeFrame(final String code, final long seconds) {
        this.code = code;
        this.seconds = seconds;
    }

    public String getCode() {
        return code;
    }

    public long getSeconds() {
        return seconds;
    }

    public static TimeFrame ofCandlestickPeriod(
            final CandlestickPeriod period) {
        return Arrays.stream(TimeFrame.values())
                .filter(klineInterval -> klineInterval.getSeconds() == period.getSeconds())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException());
    }
}
