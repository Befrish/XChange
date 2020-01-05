package org.knowm.xchange.bitfinex.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.dto.BitfinexException;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexDepth;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexLend;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexLendDepth;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexSymbolDetail;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexTicker;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexCandle;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexPublicFundingTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexPublicTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.Status;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.TimeFrame;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import si.mazi.rescu.HttpStatusIOException;

/**
 * Implementation of the market data service for Bitfinex
 *
 * <ul>
 *   <li>Provides access to various market data values
 * </ul>
 */
public class BitfinexMarketDataServiceRaw extends BitfinexBaseService {

  /**
   * Constructor
   *
   * @param exchange
   */
  public BitfinexMarketDataServiceRaw(final Exchange exchange) {

    super(exchange);
  }

  public BitfinexTicker getBitfinexTicker(final String pair) throws IOException {
    final BitfinexTicker bitfinexTicker = bitfinex.getTicker(pair);
    return bitfinexTicker;
  }

  public BitfinexDepth getBitfinexOrderBook(final String pair, final Integer limitBids, final Integer limitAsks)
      throws IOException {
    final BitfinexDepth bitfinexDepth;
    if (limitBids == null && limitAsks == null) {
      bitfinexDepth = bitfinex.getBook(pair);
    } else {
      bitfinexDepth = bitfinex.getBook(pair, limitBids, limitAsks);
    }
    return bitfinexDepth;
  }

  public BitfinexLendDepth getBitfinexLendBook(final String currency, final int limitBids, final int limitAsks)
      throws IOException {
    final BitfinexLendDepth bitfinexLendDepth = bitfinex.getLendBook(currency, limitBids, limitAsks);
    return bitfinexLendDepth;
  }

  public BitfinexTrade[] getBitfinexTrades(final String pair, final long sinceTimestamp) throws IOException {
    final BitfinexTrade[] bitfinexTrades = bitfinex.getTrades(pair, sinceTimestamp);
    return bitfinexTrades;
  }

  public BitfinexLend[] getBitfinexLends(final String currency, final long sinceTimestamp, final int limitTrades)
      throws IOException {
    final BitfinexLend[] bitfinexLends = bitfinex.getLends(currency, sinceTimestamp, limitTrades);
    return bitfinexLends;
  }

  public Collection<String> getBitfinexSymbols() throws IOException {
    return bitfinex.getSymbols();
  }

  public List<CurrencyPair> getExchangeSymbols() throws IOException {
    final List<CurrencyPair> currencyPairs = new ArrayList<>();
    for (final String symbol : bitfinex.getSymbols()) {
      currencyPairs.add(BitfinexAdapters.adaptCurrencyPair(symbol));
    }
    return currencyPairs;
  }

  public List<BitfinexSymbolDetail> getSymbolDetails() throws IOException {
    return bitfinex.getSymbolsDetails();
  }

  //////// v2

  public org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexTicker[] getBitfinexTickers(
          final Collection<CurrencyPair> currencyPairs) throws IOException {
    return bitfinexV2.getTickers(BitfinexAdapters.adaptCurrencyPairsToTickersParam(currencyPairs));
  }

  public org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexTicker getBitfinexTickerV2(
          final CurrencyPair currencyPair) throws IOException {
    final org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexTicker[] ticker =
        bitfinexV2.getTickers(
            BitfinexAdapters.adaptCurrencyPairsToTickersParam(
                Collections.singletonList(currencyPair)));
    if (ticker.length == 0) {
      throw new BitfinexException("Unknown Symbol");
    } else {
      return ticker[0];
    }
  }

  public BitfinexPublicTrade[] getBitfinexPublicTrades(
          final CurrencyPair currencyPair, final int limitTrades, final long startTimestamp, final long endTimestamp, final int sort)
      throws IOException {
    try {
      return bitfinexV2.getPublicTrades(
          BitfinexAdapters.adaptCurrencyPair(currencyPair),
          limitTrades,
          startTimestamp,
          endTimestamp,
          sort);
    } catch (final HttpStatusIOException e) {
      throw new BitfinexException(e.getHttpBody());
    }
  }

  public BitfinexPublicFundingTrade[] getBitfinexPublicFundingTrades(
          final Currency currency, final int limitTrades, final long startTimestamp, final long endTimestamp, final int sort)
      throws IOException {
    try {
      return bitfinexV2.getPublicFundingTrades(
          "f" + currency.toString(), limitTrades, startTimestamp, endTimestamp, sort);
    } catch (final HttpStatusIOException e) {
      throw new BitfinexException(e.getHttpBody());
    }
  }

  public List<Status> getStatus(final List<CurrencyPair> pairs) throws IOException {
    try {
      return bitfinexV2.getStatus(
          "deriv", BitfinexAdapters.adaptCurrencyPairsToTickersParam(pairs));
    } catch (final HttpStatusIOException e) {
      throw new BitfinexException(e.getHttpBody());
    }
  }

    public BitfinexCandle getCandle(
            final CurrencyPair currencyPair,
            final TimeFrame timeFrame,
            final int limit,
            final long startTimestamp,
            final long endTimestamp,
            final int sort)
            throws IOException {
        return bitfinexV2.getTradingCandleLast(
                BitfinexAdapters.adaptCurrencyPair(currencyPair),
                BitfinexAdapters.adaptTimeFrame(timeFrame),
                limit,
                startTimestamp,
                endTimestamp,
                sort);
    }

    public BitfinexCandle[] getCandles(
            final CurrencyPair currencyPair,
            final TimeFrame timeFrame,
            final int limit,
            final long startTimestamp,
            final long endTimestamp,
            final int sort)
            throws IOException {
        return bitfinexV2.getTradingCandlesHist(
                BitfinexAdapters.adaptCurrencyPair(currencyPair),
                BitfinexAdapters.adaptTimeFrame(timeFrame),
                limit,
                startTimestamp,
                endTimestamp,
                sort);
    }
}
