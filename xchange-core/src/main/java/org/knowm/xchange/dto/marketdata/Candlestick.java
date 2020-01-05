package org.knowm.xchange.dto.marketdata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.utils.Assert;
import org.knowm.xchange.utils.DateUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * A class encapsulating the information a "Candlestick" can contain. Some fields can be empty if not
 * provided by the exchange.
 *
 * @author Benno Müller
 */
@JsonDeserialize(builder = Candlestick.Builder.class)
public class Candlestick implements Serializable {

    private static final long serialVersionUID = 1713466418421883524L;

    private final CurrencyPair currencyPair;
    private final BigDecimal open;
    private final BigDecimal close;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal volume;
    private final BigDecimal quoteVolume;
    /** the timestamp of the ticker according to the exchange's server, null if not provided */
    private final Date timestamp;

    /**
     * Constructor
     *
     * @param currencyPair The tradable identifier (e.g. BTC in BTC/USD)
     * @param close Last price
     * @param high High price
     * @param low Low price
     * @param volume 24h volume in base currency
     * @param quoteVolume 24h volume in counter currency
     * @param timestamp - the timestamp of the ticker according to the exchange's server, null if not
     *     provided
     */
    private Candlestick(
            final CurrencyPair currencyPair,
            final BigDecimal open,
            final BigDecimal close,
            final BigDecimal high,
            final BigDecimal low,
            final BigDecimal volume,
            final BigDecimal quoteVolume,
            final Date timestamp) {
        this.open = open;
        this.currencyPair = currencyPair;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.quoteVolume = quoteVolume;
        this.timestamp = timestamp;
    }

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public BigDecimal getQuoteVolume() {
        if (quoteVolume == null && volume != null && close != null) {
            return volume.multiply(close);
        }
        return quoteVolume;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Ticker [currencyPair="
                + currencyPair
                + ", open=" + open
                + ", last=" + close
                + ", high=" + high
                + ", low=" + low
                + ", volume=" + volume
                + ", quoteVolume=" + quoteVolume
                + ", timestamp=" + DateUtils.toMillisNullSafe(timestamp)
                + "]";
    }

    /**
     * Builder to provide the following to {@link Ticker}:
     *
     * <ul>
     *   <li>Provision of fluent chained construction interface
     * </ul>
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private CurrencyPair currencyPair;
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal volume;
        private BigDecimal quoteVolume;
        private Date timestamp;

        // Prevent repeat builds
        private boolean isBuilt = false;

        public Candlestick build() {
            validateState();

            final Candlestick candlestick = new Candlestick(
                    currencyPair,
                    open,
                    close,
                    high,
                    low,
                    volume,
                    quoteVolume,
                    timestamp);

            isBuilt = true;

            return candlestick;
        }

        private void validateState() {
            if (isBuilt) {
                throw new IllegalStateException("The entity has been built");
            }
        }

        public Candlestick.Builder currencyPair(final CurrencyPair currencyPair) {
            Assert.notNull(currencyPair, "Null currencyPair");
            this.currencyPair = currencyPair;
            return this;
        }

        public Candlestick.Builder open(final BigDecimal open) {
            this.open = open;
            return this;
        }

        public Candlestick.Builder close(final BigDecimal close) {
            this.close = close;
            return this;
        }

        public Candlestick.Builder high(final BigDecimal high) {
            this.high = high;
            return this;
        }

        public Candlestick.Builder low(final BigDecimal low) {
            this.low = low;
            return this;
        }

        public Candlestick.Builder volume(final BigDecimal volume) {
            this.volume = volume;
            return this;
        }

        public Candlestick.Builder quoteVolume(final BigDecimal quoteVolume) {
            this.quoteVolume = quoteVolume;
            return this;
        }

        public Candlestick.Builder timestamp(final Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }
    }

}
