package org.knowm.xchange.service.marketdata;

import java.io.IOException;
import java.util.List;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Candlestick;
import org.knowm.xchange.dto.marketdata.CandlestickPeriod;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.BaseService;
import org.knowm.xchange.service.marketdata.params.Params;

/**
 * Interface to provide the following to {@link Exchange}:
 *
 * <ul>
 *   <li>Standard methods available to explore the market data
 * </ul>
 *
 * <p>The implementation of this service is expected to be based on a client polling mechanism of
 * some kind
 */
public interface MarketDataService extends BaseService {

  /**
   * Get a ticker representing the current exchange rate
   *
   * @return The Ticker, null if some sort of error occurred. Implementers should log the error.
   * @throws ExchangeException - Indication that the exchange reported some kind of error with the
   *     request or response
   * @throws NotAvailableFromExchangeException - Indication that the exchange does not support the
   *     requested function or data
   * @throws NotYetImplementedForExchangeException - Indication that the exchange supports the
   *     requested function or data, but it has not yet been implemented
   * @throws IOException - Indication that a networking error occurred while fetching JSON data
   */
  default Ticker getTicker(final CurrencyPair currencyPair, final Object... args) throws IOException {
      throw new NotYetImplementedForExchangeException();
  }

  /**
   * Get the tickers representing the current exchange rate for the provided parameters
   *
   * @return The Tickers, null if some sort of error occurred. Implementers should log the error.
   * @throws ExchangeException - Indication that the exchange reported some kind of error with the
   *     request or response
   * @throws NotAvailableFromExchangeException - Indication that the exchange does not support the
   *     requested function or data
   * @throws NotYetImplementedForExchangeException - Indication that the exchange supports the
   *     requested function or data, but it has not yet been implemented
   * @throws IOException - Indication that a networking error occurred while fetching JSON data
   */
  default List<Ticker> getTickers(final Params params) throws IOException {
      throw new NotYetImplementedForExchangeException();
  }

    /**
     * Get the candlesticks representing the last exchange rates for the provided parameters
     *
     * @return The Candlestick, null if some sort of error occurred. Implementers should log the error.
     * @throws ExchangeException - Indication that the exchange reported some kind of error with the
     *     request or response
     * @throws NotAvailableFromExchangeException - Indication that the exchange does not support the
     *     requested function or data
     * @throws NotYetImplementedForExchangeException - Indication that the exchange supports the
     *     requested function or data, but it has not yet been implemented
     * @throws IOException - Indication that a networking error occurred while fetching JSON data
     */
    default Candlestick getCandlestick(
            final CurrencyPair currencyPair,
            final CandlestickPeriod period,
            final Object... args) throws IOException {
        throw new NotYetImplementedForExchangeException();
    }

    /**
     * Get the candlesticks representing the current or historic exchange rates for the provided parameters
     *
     * @return The Tickers, null if some sort of error occurred. Implementers should log the error.
     * @throws ExchangeException - Indication that the exchange reported some kind of error with the
     *     request or response
     * @throws NotAvailableFromExchangeException - Indication that the exchange does not support the
     *     requested function or data
     * @throws NotYetImplementedForExchangeException - Indication that the exchange supports the
     *     requested function or data, but it has not yet been implemented
     * @throws IOException - Indication that a networking error occurred while fetching JSON data
     */
  default List<Candlestick> getCandlesticks(
          final CurrencyPair currencyPair,
          final CandlestickPeriod period,
          final Params params) throws IOException {
      throw new NotYetImplementedForExchangeException();
  }

  /**
   * Get an order book representing the current offered exchange rates (market depth)
   *
   * @param args Optional arguments. Exchange-specific
   * @return The OrderBook, null if some sort of error occurred. Implementers should log the error.
   * @throws ExchangeException - Indication that the exchange reported some kind of error with the
   *     request or response
   * @throws NotAvailableFromExchangeException - Indication that the exchange does not support the
   *     requested function or data
   * @throws NotYetImplementedForExchangeException - Indication that the exchange supports the
   *     requested function or data, but it has not yet been implemented
   * @throws IOException - Indication that a networking error occurred while fetching JSON data
   */
  default OrderBook getOrderBook(final CurrencyPair currencyPair, final Object... args) throws IOException {
      throw new NotYetImplementedForExchangeException();
  }

  /**
   * Get the trades recently performed by the exchange
   *
   * @param args Optional arguments. Exchange-specific
   * @return The Trades, null if some sort of error occurred. Implementers should log the error.
   * @throws ExchangeException - Indication that the exchange reported some kind of error with the
   *     request or response
   * @throws NotAvailableFromExchangeException - Indication that the exchange does not support the
   *     requested function or data
   * @throws NotYetImplementedForExchangeException - Indication that the exchange supports the
   *     requested function or data, but it has not yet been implemented
   * @throws IOException - Indication that a networking error occurred while fetching JSON data
   */
  default Trades getTrades(final CurrencyPair currencyPair, final Object... args) throws IOException {
      throw new NotYetImplementedForExchangeException();
  }
}
