package org.knowm.xchange.bitfinex.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.BitfinexErrorAdapter;
import org.knowm.xchange.bitfinex.dto.BitfinexException;
import org.knowm.xchange.bitfinex.v1.BitfinexUtils;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexDepth;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexLendDepth;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexCandle;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexTicker;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.TimeFrame;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Candlestick;
import org.knowm.xchange.dto.marketdata.CandlestickPeriod;
import org.knowm.xchange.dto.marketdata.LoanOrderBook;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.trade.FixedRateLoanOrder;
import org.knowm.xchange.dto.trade.FloatingRateLoanOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;
import org.knowm.xchange.service.marketdata.params.Params;

/**
 * Implementation of the market data service for Bitfinex
 *
 * <ul>
 *   <li>Provides access to various market data values
 * </ul>
 */
public class BitfinexMarketDataService extends BitfinexMarketDataServiceRaw
        implements MarketDataService {

    /**
     * Constructor
     *
     * @param exchange
     */
    public BitfinexMarketDataService(final Exchange exchange) {

        super(exchange);
    }

    @Override
    public Ticker getTicker(final CurrencyPair currencyPair, final Object... args) throws IOException {
        // return getTickerV1(currencyPair, args);
        return getTickerV2(currencyPair, args);
    }

    private Ticker getTickerV1(final CurrencyPair currencyPair, final Object... args) throws IOException {
        try {
            return BitfinexAdapters.adaptTicker(
                    getBitfinexTicker(BitfinexUtils.toPairString(currencyPair)), currencyPair);
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    private Ticker getTickerV2(final CurrencyPair currencyPair, final Object... args) throws IOException {
        try {
            return BitfinexAdapters.adaptTicker(getBitfinexTickerV2(currencyPair));
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    /**
     * @param args If two integers are provided, then those count as limit bid and limit ask count
     */
    @Override
    public OrderBook getOrderBook(final CurrencyPair currencyPair, final Object... args) throws IOException {
        try {
            // null will cause fetching of full order book, the default behavior in XChange
            Integer limitBids = null;
            Integer limitAsks = null;

            if (args != null && args.length == 2) {
                final Object arg0 = args[0];
                if (!(arg0 instanceof Integer)) {
                    throw new ExchangeException("Argument 0 must be an Integer!");
                } else {
                    limitBids = (Integer) arg0;
                }
                final Object arg1 = args[1];
                if (!(arg1 instanceof Integer)) {
                    throw new ExchangeException("Argument 1 must be an Integer!");
                } else {
                    limitAsks = (Integer) arg1;
                }
            }

            final BitfinexDepth bitfinexDepth =
                    getBitfinexOrderBook(BitfinexUtils.toPairString(currencyPair), limitBids, limitAsks);

            final OrderBook orderBook = BitfinexAdapters.adaptOrderBook(bitfinexDepth, currencyPair);

            return orderBook;
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    public LoanOrderBook getLendOrderBook(final String currency, final Object... args) throws IOException {
        try {
            // According to API docs, default is 50
            int limitBids = 50;
            int limitAsks = 50;

            if (args != null && args.length == 2) {
                final Object arg0 = args[0];
                if (!(arg0 instanceof Integer)) {
                    throw new ExchangeException("Argument 0 must be an Integer!");
                } else {
                    limitBids = (Integer) arg0;
                }
                final Object arg1 = args[1];
                if (!(arg1 instanceof Integer)) {
                    throw new ExchangeException("Argument 1 must be an Integer!");
                } else {
                    limitAsks = (Integer) arg1;
                }
            }

            final BitfinexLendDepth bitfinexLendDepth = getBitfinexLendBook(currency, limitBids, limitAsks);

            final List<FixedRateLoanOrder> fixedRateAsks =
                    BitfinexAdapters.adaptFixedRateLoanOrders(
                            bitfinexLendDepth.getAsks(), currency, "ask", "");
            final List<FixedRateLoanOrder> fixedRateBids =
                    BitfinexAdapters.adaptFixedRateLoanOrders(
                            bitfinexLendDepth.getBids(), currency, "bid", "");
            final List<FloatingRateLoanOrder> floatingRateAsks =
                    BitfinexAdapters.adaptFloatingRateLoanOrders(
                            bitfinexLendDepth.getAsks(), currency, "ask", "");
            final List<FloatingRateLoanOrder> floatingRateBids =
                    BitfinexAdapters.adaptFloatingRateLoanOrders(
                            bitfinexLendDepth.getBids(), currency, "bid", "");

            return new LoanOrderBook(
                    null, fixedRateAsks, fixedRateBids, floatingRateAsks, floatingRateBids);
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    @Override
    public Trades getTrades(final CurrencyPair currencyPair, final Object... args) throws IOException {
        // return getTradesV1(currencyPair, args);
        return getTradesV2(currencyPair, args);
    }

    /**
     * @param currencyPair The CurrencyPair for which to query trades.
     * @param args         One argument may be supplied which is the timestamp after which trades should be
     *                     collected. Trades before this time are not reported. The argument may be of type
     *                     java.util.Date or Number (milliseconds since Jan 1, 1970)
     */
    private Trades getTradesV1(final CurrencyPair currencyPair, final Object... args) throws IOException {
        try {
            long lastTradeTime = 0;
            if (args != null && args.length == 1) {
                // parameter 1, if present, is the last trade timestamp
                if (args[0] instanceof Number) {
                    final Number arg = (Number) args[0];
                    lastTradeTime =
                            arg.longValue() / 1000; // divide by 1000 to convert to unix timestamp (seconds)
                } else if (args[0] instanceof Date) {
                    final Date arg = (Date) args[0];
                    lastTradeTime =
                            arg.getTime() / 1000; // divide by 1000 to convert to unix timestamp (seconds)
                } else {
                    throw new IllegalArgumentException(
                            "Extra argument #1, the last trade time, must be a Date or Long (millisecond timestamp) (was "
                                    + args[0].getClass()
                                    + ")");
                }
            }
            final BitfinexTrade[] trades =
                    getBitfinexTrades(BitfinexUtils.toPairString(currencyPair), lastTradeTime);

            return BitfinexAdapters.adaptTrades(trades, currencyPair);
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    /**
     * @param currencyPair The CurrencyPair for which to query public trades.
     * @param args         Upto 4 numeric arguments may be supplied limitTrades, startTimestamp (Unix
     *                     millisecs), endTimestamp (Unix millisecs), sort -1 / 1 (if = 1 it sorts results returned
     *                     with old > new)
     */
    private Trades getTradesV2(final CurrencyPair currencyPair, final Object... args) throws IOException {
        try {
            int limitTrades = 1000;
            long startTimestamp = 0;
            long endTimestamp = 0;
            int sort = -1;

            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Number) {
                        final Number arg = (Number) args[i];
                        switch (i) {
                            case 0:
                                limitTrades = arg.intValue();
                                break;
                            case 1:
                                startTimestamp = arg.longValue();
                                break;
                            case 2:
                                endTimestamp = arg.longValue();
                                break;
                            case 3:
                                sort = arg.intValue();
                                break;
                        }
                    } else {
                        throw new IllegalArgumentException(
                                "Extra argument #" + i + " must be an int/long was: " + args[i].getClass());
                    }
                }
            }

            return BitfinexAdapters.adaptPublicTrades(
                    getBitfinexPublicTrades(currencyPair, limitTrades, startTimestamp, endTimestamp, sort),
                    currencyPair);
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    @Override
    public List<Ticker> getTickers(final Params params) throws IOException {
        try {
            final BitfinexTicker[] bitfinexTickers =
                    params instanceof CurrencyPairsParam
                            ? getBitfinexTickers(((CurrencyPairsParam) params).getCurrencyPairs())
                            : getBitfinexTickers(null);

            return Arrays.stream(bitfinexTickers)
                    .map(BitfinexAdapters::adaptTicker)
                    .collect(Collectors.toList());
        } catch (final BitfinexException e) {
            throw BitfinexErrorAdapter.adapt(e);
        }
    }

    @Override
    public Candlestick getCandlestick(
            final CurrencyPair currencyPair,
            final CandlestickPeriod period,
            final Object... args) throws IOException {
        final int limitTrades = 1000;
        final long startTimestamp = 0;
        final long endTimestamp = 0;
        final int sort = -1;

        return getCandle(
                currencyPair,
                TimeFrame.ofCandlestickPeriod(period),
                limitTrades,
                startTimestamp,
                endTimestamp,
                sort)
                .toCandlestick(currencyPair);
    }

    @Override
    public List<Candlestick> getCandlesticks(
            final CurrencyPair currencyPair,
            final CandlestickPeriod period,
            final Params params) throws IOException {
        final int limitTrades = 1000;
        final long startTimestamp = 0;
        final long endTimestamp = 0;
        final int sort = -1;

        return Arrays.stream(getCandles(
                currencyPair,
                TimeFrame.ofCandlestickPeriod(period),
                limitTrades,
                startTimestamp,
                endTimestamp,
                sort))
                .map(candle -> candle.toCandlestick(currencyPair))
                .collect(Collectors.toList());
    }
}
