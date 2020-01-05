package org.knowm.xchange.binance.dto.marketdata;

import org.knowm.xchange.dto.marketdata.CandlestickPeriod;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

public enum KlineInterval {
    m1("1m", MINUTES.toMillis(1)),
    m3("3m", MINUTES.toMillis(3)),
    m5("5m", MINUTES.toMillis(5)),
    m15("15m", MINUTES.toMillis(15)),
    m30("30m", MINUTES.toMillis(30)),

    h1("1h", HOURS.toSeconds(1)),
    h2("2h", HOURS.toSeconds(2)),
    h4("4h", HOURS.toSeconds(4)),
    h6("6h", HOURS.toSeconds(6)),
    h8("8h", HOURS.toSeconds(8)),
    h12("12h", HOURS.toSeconds(12)),

    d1("1d", DAYS.toSeconds(1)),
    d3("3d", DAYS.toSeconds(3)),

    w1("1w", DAYS.toSeconds(7)),

    M1("1M", DAYS.toSeconds(30));

    private final String code;
    private final Long seconds;

    private KlineInterval(final String code, final Long seconds) {
        this.seconds = seconds;
        this.code = code;
    }

    public Long getSeconds() {
        return seconds;
    }

    public String code() {
        return code;
    }

    public static KlineInterval ofCandlestickPeriod(
            final CandlestickPeriod period) {
        return Arrays.stream(KlineInterval.values())
                .filter(klineInterval -> klineInterval.getSeconds() == period.getSeconds())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException());
    }
}
