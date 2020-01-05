package org.knowm.xchange.bitfinex.v2.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Candlestick;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @see https://docs.bitfinex.com/reference#rest-public-candles
 *
 * @author Benno Müller
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class BitfinexCandle {

    /**
     * millisecond time stamp
     */
    private long mts;

    /**
     * First execution during the time frame
     */
    private BigDecimal open;

    /**
     * Last execution during the time frame
     */
    private BigDecimal close;

    /**
     * Highest execution during the time frame
     */
    private BigDecimal high;

    /**
     * Lowest execution during the timeframe
     */
    private BigDecimal low;

    /**
     * Quantity of symbol traded within the timeframe
     */
    private BigDecimal volume;

    public Candlestick toCandlestick(
            final CurrencyPair currencyPair) {
        return new Candlestick.Builder()
                .currencyPair(currencyPair)
                .open(open)
                .close(close)
                .high(high)
                .low(low)
                .volume(volume)
                .timestamp(new Date(mts))
                .build();
    }

}
