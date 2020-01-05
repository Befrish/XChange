package org.knowm.xchange.binance.service;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.BinanceErrorAdapter;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.marketdata.BinanceAggTrades;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.dto.marketdata.BinancePriceQuantity;
import org.knowm.xchange.binance.dto.marketdata.BinanceTicker24h;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.Candlestick;
import org.knowm.xchange.dto.marketdata.CandlestickPeriod;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.marketdata.Trades.TradeSortType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.Params;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class BinanceMarketDataService extends BinanceMarketDataServiceRaw
    implements MarketDataService {

  public BinanceMarketDataService(final Exchange exchange) {

    super(exchange);
  }

  @Override
  public OrderBook getOrderBook(final CurrencyPair pair, final Object... args) throws IOException {
    try {
      int limitDepth = 100;

      if (args != null && args.length == 1) {
        final Object arg0 = args[0];
        if (!(arg0 instanceof Integer)) {
          throw new ExchangeException("Argument 0 must be an Integer!");
        } else {
          limitDepth = (Integer) arg0;
        }
      }
      final BinanceOrderbook binanceOrderbook = getBinanceOrderbook(pair, limitDepth);
      return convertOrderBook(binanceOrderbook, pair);
    } catch (final BinanceException e) {
      throw BinanceErrorAdapter.adapt(e);
    }
  }

  public static OrderBook convertOrderBook(final BinanceOrderbook ob, final CurrencyPair pair) {
    final List<LimitOrder> bids =
        ob.bids.entrySet().stream()
            .map(e -> new LimitOrder(OrderType.BID, e.getValue(), pair, null, null, e.getKey()))
            .collect(toList());
    final List<LimitOrder> asks =
        ob.asks.entrySet().stream()
            .map(e -> new LimitOrder(OrderType.ASK, e.getValue(), pair, null, null, e.getKey()))
            .collect(toList());
    return new OrderBook(null, asks, bids);
  }

  @Override
  public Ticker getTicker(final CurrencyPair pair, final Object... args) throws IOException {
    try {
      return ticker24h(pair).toTicker();
    } catch (final BinanceException e) {
      throw BinanceErrorAdapter.adapt(e);
    }
  }

  @Override
  public List<Ticker> getTickers(final Params params) throws IOException {
    try {
      return ticker24h().stream().map(BinanceTicker24h::toTicker).collect(toList());
    } catch (final BinanceException e) {
      throw BinanceErrorAdapter.adapt(e);
    }
  }

    @Override
    public Candlestick getCandlestick(
            final CurrencyPair currencyPair,
            final CandlestickPeriod period,
            final Object... args) throws IOException {
        return lastKline(currencyPair, KlineInterval.ofCandlestickPeriod(period))
                .toCandlestick();
    }

    @Override
    public List<Candlestick> getCandlesticks(
            final CurrencyPair pair,
            final CandlestickPeriod period,
            final Params params) throws IOException {
        return klines(pair, KlineInterval.ofCandlestickPeriod(period))
                .stream()
                .map(BinanceKline::toCandlestick)
                .collect(Collectors.toList());
    }

    /**
   * optional parameters provided in the args array:
   *
   * <ul>
   *   <li>0: Long fromId optional, ID to get aggregate trades from INCLUSIVE.
   *   <li>1: Long startTime optional, Timestamp in ms to get aggregate trades from INCLUSIVE.
   *   <li>2: Long endTime optional, Timestamp in ms to get aggregate trades until INCLUSIVE.
   *   <li>3: Integer limit optional, Default 500; max 500.
   * </ul>
   *
   * <p>
   */
  @Override
  public Trades getTrades(final CurrencyPair pair, final Object... args) throws IOException {
    try {
      final Long fromId = tradesArgument(args, 0, Long::valueOf);
      final Long startTime = tradesArgument(args, 1, Long::valueOf);
      final Long endTime = tradesArgument(args, 2, Long::valueOf);
      final Integer limit = tradesArgument(args, 3, Integer::valueOf);
      final List<BinanceAggTrades> aggTrades =
          binance.aggTrades(BinanceAdapters.toSymbol(pair), fromId, startTime, endTime, limit);
      final List<Trade> trades =
          aggTrades.stream()
              .map(
                  at ->
                      new Trade(
                          BinanceAdapters.convertType(at.buyerMaker),
                          at.quantity,
                          pair,
                          at.price,
                          at.getTimestamp(),
                          Long.toString(at.aggregateTradeId)))
              .collect(toList());
      return new Trades(trades, TradeSortType.SortByTimestamp);
    } catch (final BinanceException e) {
      throw BinanceErrorAdapter.adapt(e);
    }
  }

  private <T extends Number> T tradesArgument(
          final Object[] args, final int index, final Function<String, T> converter) {
    if (index >= args.length) {
      return null;
    }
    final Object arg = args[index];
    if (arg == null) {
      return null;
    }
    final String argStr = arg.toString();
    try {
      return converter.apply(argStr);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Argument on index " + index + " is not a number: " + argStr, e);
    }
  }

  public List<Ticker> getAllBookTickers() throws IOException {
    final List<BinancePriceQuantity> binanceTickers = tickerAllBookTickers();
    return BinanceAdapters.adaptPriceQuantities(binanceTickers);
  }
}
