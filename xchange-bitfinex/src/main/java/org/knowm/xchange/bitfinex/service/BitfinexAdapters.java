package org.knowm.xchange.bitfinex.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knowm.xchange.bitfinex.v1.BitfinexOrderType;
import org.knowm.xchange.bitfinex.v1.BitfinexUtils;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexAccountFeesResponse;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexBalancesResponse;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexDepositWithdrawalHistoryResponse;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexTradingFeeResponse;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.*;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexAccountInfosResponse;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderFlags;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexTradeResponse;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexPublicTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.TimeFrame;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.CandlestickPeriod;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.marketdata.Trades.TradeSortType;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.trade.*;
import org.knowm.xchange.utils.DateUtils;
import org.knowm.xchange.utils.jackson.CurrencyPairDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BitfinexAdapters {

  public static final Logger log = LoggerFactory.getLogger(BitfinexAdapters.class);

  private static final AtomicBoolean warnedStopLimit = new AtomicBoolean();

  private BitfinexAdapters() {}

  /**
   * Each element in the response array contains a set of currencies that are at a given fee tier.
   * The API returns the fee per currency in each tier and does not make any promises that they are
   * all the same, so this adapter will use the fee per currency instead of the fee per tier.
   */
  public static Map<CurrencyPair, Fee> adaptDynamicTradingFees(
          final BitfinexTradingFeeResponse[] responses, final List<CurrencyPair> currencyPairs) {
    final Map<CurrencyPair, Fee> result = new HashMap<>();
    for (final BitfinexTradingFeeResponse response : responses) {
      final BitfinexTradingFeeResponse.BitfinexTradingFeeResponseRow[] responseRows =
          response.getTradingFees();
      for (final BitfinexTradingFeeResponse.BitfinexTradingFeeResponseRow responseRow : responseRows) {
        final Currency currency = Currency.getInstance(responseRow.getCurrency());
        final BigDecimal percentToFraction = BigDecimal.ONE.divide(BigDecimal.ONE.scaleByPowerOfTen(2));
        final Fee fee =
            new Fee(
                responseRow.getMakerFee().multiply(percentToFraction),
                responseRow.getTakerFee().multiply(percentToFraction));
        for (final CurrencyPair pair : currencyPairs) {
          // Fee to trade for a currency is the fee to trade currency pairs with this base.
          // Fee is typically assessed in units counter.
          if (pair.base.equals(currency)) {
            if (result.put(pair, fee) != null) {
              throw new IllegalStateException(
                  "Fee for currency pair " + pair + " is overspecified");
            }
          }
        }
      }
    }
    return result;
  }

  public static String adaptBitfinexCurrency(final String bitfinexSymbol) {
    return bitfinexSymbol.toUpperCase();
  }

  public static String adaptOrderType(final OrderType type) {
    switch (type) {
      case BID:
      case EXIT_BID:
        return "buy";
      case ASK:
      case EXIT_ASK:
        return "sell";
    }

    throw new IllegalArgumentException(String.format("Unexpected type of order: %s", type));
  }

  public static BitfinexOrderType adaptOrderFlagsToType(final Set<Order.IOrderFlags> flags) {
    if (flags.contains(BitfinexOrderFlags.MARGIN)) {
      if (flags.contains(BitfinexOrderFlags.FILL_OR_KILL)) {
        return BitfinexOrderType.MARGIN_FILL_OR_KILL;
      } else if (flags.contains(BitfinexOrderFlags.TRAILING_STOP)) {
        return BitfinexOrderType.MARGIN_TRAILING_STOP;
      } else if (flags.contains(BitfinexOrderFlags.STOP)) {
        return BitfinexOrderType.MARGIN_STOP;
      } else {
        return BitfinexOrderType.MARGIN_LIMIT;
      }
    } else {
      if (flags.contains(BitfinexOrderFlags.FILL_OR_KILL)) {
        return BitfinexOrderType.FILL_OR_KILL;
      } else if (flags.contains(BitfinexOrderFlags.TRAILING_STOP)) {
        return BitfinexOrderType.TRAILING_STOP;
      } else if (flags.contains(BitfinexOrderFlags.STOP)) {
        return BitfinexOrderType.STOP;
      } else {
        return BitfinexOrderType.LIMIT;
      }
    }
  }

  public static CurrencyPair adaptCurrencyPair(final String bitfinexSymbol) {
    final String tradableIdentifier;
    final String transactionCurrency;
    final int startIndex =
        bitfinexSymbol.startsWith("t") && Character.isUpperCase(bitfinexSymbol.charAt(1)) ? 1 : 0;
    if (bitfinexSymbol.contains(":")) {
      // ie 'dusk:usd' or 'btc:cnht'
      final int idx = bitfinexSymbol.indexOf(":");
      tradableIdentifier = bitfinexSymbol.substring(startIndex, idx);
      transactionCurrency = bitfinexSymbol.substring(idx + 1);
    } else {
      tradableIdentifier = bitfinexSymbol.substring(startIndex, startIndex + 3);
      transactionCurrency = bitfinexSymbol.substring(startIndex + 3);
    }

    return new CurrencyPair(
        adaptBitfinexCurrency(tradableIdentifier), adaptBitfinexCurrency(transactionCurrency));
  }

  public static OrderStatus adaptOrderStatus(final BitfinexOrderStatusResponse order) {

    if (order.isCancelled()) {
        return OrderStatus.CANCELED;
    } else if (order.getExecutedAmount().compareTo(BigDecimal.ZERO) == 0) {
        return OrderStatus.NEW;
    } else if (order.getExecutedAmount().compareTo(order.getOriginalAmount()) < 0) {
        return OrderStatus.PARTIALLY_FILLED;
    } else if (order.getExecutedAmount().compareTo(order.getOriginalAmount()) == 0) {
        return OrderStatus.FILLED;
    } else {
        return null;
    }
  }

  public static String adaptCurrencyPair(final CurrencyPair pair) {
    return BitfinexUtils.toPairString(pair);
  }

  public static OrderBook adaptOrderBook(final BitfinexDepth btceDepth, final CurrencyPair currencyPair) {

    final OrdersContainer asksOrdersContainer =
        adaptOrders(btceDepth.getAsks(), currencyPair, OrderType.ASK);
    final OrdersContainer bidsOrdersContainer =
        adaptOrders(btceDepth.getBids(), currencyPair, OrderType.BID);

    return new OrderBook(
        new Date(Math.max(asksOrdersContainer.getTimestamp(), bidsOrdersContainer.getTimestamp())),
        asksOrdersContainer.getLimitOrders(),
        bidsOrdersContainer.getLimitOrders());
  }

  public static OrdersContainer adaptOrders(
          final BitfinexLevel[] bitfinexLevels, final CurrencyPair currencyPair, final OrderType orderType) {

    BigDecimal maxTimestamp = new BigDecimal(Long.MIN_VALUE);
    final List<LimitOrder> limitOrders = new ArrayList<>(bitfinexLevels.length);

    for (final BitfinexLevel bitfinexLevel : bitfinexLevels) {
      if (bitfinexLevel.getTimestamp().compareTo(maxTimestamp) > 0) {
        maxTimestamp = bitfinexLevel.getTimestamp();
      }

      final Date timestamp = convertBigDecimalTimestampToDate(bitfinexLevel.getTimestamp());
      limitOrders.add(
          adaptOrder(
              bitfinexLevel.getAmount(),
              bitfinexLevel.getPrice(),
              currencyPair,
              orderType,
              timestamp));
    }

    final long maxTimestampInMillis = maxTimestamp.multiply(new BigDecimal(1000L)).longValue();
    return new OrdersContainer(maxTimestampInMillis, limitOrders);
  }

  public static LimitOrder adaptOrder(
          final BigDecimal originalAmount,
          final BigDecimal price,
          final CurrencyPair currencyPair,
          final OrderType orderType,
          final Date timestamp) {

    return new LimitOrder(orderType, originalAmount, currencyPair, "", timestamp, price);
  }

  public static List<FixedRateLoanOrder> adaptFixedRateLoanOrders(
          final BitfinexLendLevel[] orders, final String currency, final String orderType, final String id) {

    final List<FixedRateLoanOrder> loanOrders = new ArrayList<>(orders.length);

    for (final BitfinexLendLevel order : orders) {
      if ("yes".equalsIgnoreCase(order.getFrr())) {
        continue;
      }

      // Bid orderbook is reversed order. Insert at reversed indices
      if (orderType.equalsIgnoreCase("loan")) {
        loanOrders.add(
            0,
            adaptFixedRateLoanOrder(
                currency, order.getAmount(), order.getPeriod(), orderType, id, order.getRate()));
      } else {
        loanOrders.add(
            adaptFixedRateLoanOrder(
                currency, order.getAmount(), order.getPeriod(), orderType, id, order.getRate()));
      }
    }

    return loanOrders;
  }

  public static FixedRateLoanOrder adaptFixedRateLoanOrder(
          final String currency,
          final BigDecimal amount,
          final int dayPeriod,
          final String direction,
          final String id,
          final BigDecimal rate) {

    final OrderType orderType = direction.equalsIgnoreCase("loan") ? OrderType.BID : OrderType.ASK;

    return new FixedRateLoanOrder(orderType, currency, amount, dayPeriod, id, null, rate);
  }

  public static List<FloatingRateLoanOrder> adaptFloatingRateLoanOrders(
          final BitfinexLendLevel[] orders, final String currency, final String orderType, final String id) {

    final List<FloatingRateLoanOrder> loanOrders = new ArrayList<>(orders.length);

    for (final BitfinexLendLevel order : orders) {
      if ("no".equals(order.getFrr())) {
        continue;
      }

      // Bid orderbook is reversed order. Insert at reversed indices
      if (orderType.equalsIgnoreCase("loan")) {
        loanOrders.add(
            0,
            adaptFloatingRateLoanOrder(
                currency, order.getAmount(), order.getPeriod(), orderType, id, order.getRate()));
      } else {
        loanOrders.add(
            adaptFloatingRateLoanOrder(
                currency, order.getAmount(), order.getPeriod(), orderType, id, order.getRate()));
      }
    }

    return loanOrders;
  }

  public static FloatingRateLoanOrder adaptFloatingRateLoanOrder(
          final String currency,
          final BigDecimal amount,
          final int dayPeriod,
          final String direction,
          final String id,
          final BigDecimal rate) {

    final OrderType orderType = direction.equalsIgnoreCase("loan") ? OrderType.BID : OrderType.ASK;

    return new FloatingRateLoanOrder(orderType, currency, amount, dayPeriod, id, null, rate);
  }

  public static Trade adaptTrade(final BitfinexTrade trade, final CurrencyPair currencyPair) {

    final OrderType orderType = trade.getType().equals("buy") ? OrderType.BID : OrderType.ASK;
    final BigDecimal amount = trade.getAmount();
    final BigDecimal price = trade.getPrice();
    final Date date =
        DateUtils.fromMillisUtc(trade.getTimestamp() * 1000L); // Bitfinex uses Unix timestamps
    final String tradeId = String.valueOf(trade.getTradeId());
    return new Trade(orderType, amount, currencyPair, price, date, tradeId);
  }

  public static Trades adaptTrades(final BitfinexTrade[] trades, final CurrencyPair currencyPair) {

    final List<Trade> tradesList = new ArrayList<>(trades.length);
    long lastTradeId = 0;
    for (final BitfinexTrade trade : trades) {
      final long tradeId = trade.getTradeId();
      if (tradeId > lastTradeId) {
        lastTradeId = tradeId;
      }
      tradesList.add(adaptTrade(trade, currencyPair));
    }
    return new Trades(tradesList, lastTradeId, TradeSortType.SortByID);
  }

  public static Ticker adaptTicker(final BitfinexTicker bitfinexTicker, final CurrencyPair currencyPair) {

    final BigDecimal last = bitfinexTicker.getLast_price();
    final BigDecimal bid = bitfinexTicker.getBid();
    final BigDecimal ask = bitfinexTicker.getAsk();
    final BigDecimal high = bitfinexTicker.getHigh();
    final BigDecimal low = bitfinexTicker.getLow();
    final BigDecimal volume = bitfinexTicker.getVolume();

    final Date timestamp = DateUtils.fromMillisUtc((long) (bitfinexTicker.getTimestamp() * 1000L));

    return new Ticker.Builder()
        .currencyPair(currencyPair)
        .last(last)
        .bid(bid)
        .ask(ask)
        .high(high)
        .low(low)
        .volume(volume)
        .timestamp(timestamp)
        .build();
  }

  public static List<Wallet> adaptWallets(final BitfinexBalancesResponse[] response) {

    final Map<String, Map<String, BigDecimal[]>> walletsBalancesMap = new HashMap<>();

    // for each currency we have multiple balances types: exchange, trading, deposit.
    // each of those may be partially frozen/available
    for (final BitfinexBalancesResponse balance : response) {
      final String walletId = balance.getType();

      if (!walletsBalancesMap.containsKey(walletId)) {
        walletsBalancesMap.put(walletId, new HashMap<>());
      }
      final Map<String, BigDecimal[]> balancesByCurrency =
          walletsBalancesMap.get(walletId); // {total, available}

      final String currencyName = adaptBitfinexCurrency(balance.getCurrency());
      BigDecimal[] balanceDetail = balancesByCurrency.get(currencyName);
      if (balanceDetail == null) {
        balanceDetail = new BigDecimal[] {balance.getAmount(), balance.getAvailable()};
      } else {
        balanceDetail[0] = balanceDetail[0].add(balance.getAmount());
        balanceDetail[1] = balanceDetail[1].add(balance.getAvailable());
      }
      balancesByCurrency.put(currencyName, balanceDetail);
    }

    final List<Wallet> wallets = new ArrayList<>();
    for (final Entry<String, Map<String, BigDecimal[]>> walletData : walletsBalancesMap.entrySet()) {
      final Map<String, BigDecimal[]> balancesByCurrency = walletData.getValue();

      final List<Balance> balances = new ArrayList<>(balancesByCurrency.size());
      for (final Entry<String, BigDecimal[]> entry : balancesByCurrency.entrySet()) {
        final String currencyName = entry.getKey();
        final BigDecimal[] balanceDetail = entry.getValue();
        final BigDecimal balanceTotal = balanceDetail[0];
        final BigDecimal balanceAvailable = balanceDetail[1];
        balances.add(
            new Balance(Currency.getInstance(currencyName), balanceTotal, balanceAvailable));
      }
      wallets.add(Wallet.Builder.from(balances).id(walletData.getKey()).build());
    }

    return wallets;
  }

  public static OpenOrders adaptOrders(final BitfinexOrderStatusResponse[] activeOrders) {

    final List<LimitOrder> limitOrders = new ArrayList<>();
    final List<Order> hiddenOrders = new ArrayList<>();

    for (final BitfinexOrderStatusResponse order : activeOrders) {

      final OrderType orderType = order.getSide().equalsIgnoreCase("buy") ? OrderType.BID : OrderType.ASK;
      final OrderStatus status = adaptOrderStatus(order);
      final CurrencyPair currencyPair = adaptCurrencyPair(order.getSymbol());
      final Date timestamp = convertBigDecimalTimestampToDate(order.getTimestamp());

      final Supplier<LimitOrder> limitOrderCreator =
          () ->
              new LimitOrder(
                  orderType,
                  order.getOriginalAmount(),
                  currencyPair,
                  String.valueOf(order.getId()),
                  timestamp,
                  order.getPrice(),
                  order.getAvgExecutionPrice(),
                  order.getExecutedAmount(),
                  null,
                  status);

      final Supplier<StopOrder> stopOrderCreator =
          () ->
              new StopOrder(
                  orderType,
                  order.getOriginalAmount(),
                  currencyPair,
                  String.valueOf(order.getId()),
                  timestamp,
                  order.getPrice(),
                  null,
                  order.getAvgExecutionPrice(),
                  order.getExecutedAmount(),
                  status);

      LimitOrder limitOrder = null;
      StopOrder stopOrder = null;

      final Optional<BitfinexOrderType> bitfinexOrderType =
          Arrays.stream(BitfinexOrderType.values())
              .filter(v -> v.getValue().equals(order.getType()))
              .findFirst();

      if (bitfinexOrderType.isPresent()) {
        switch (bitfinexOrderType.get()) {
          case FILL_OR_KILL:
            limitOrder = limitOrderCreator.get();
            limitOrder.addOrderFlag(BitfinexOrderFlags.FILL_OR_KILL);
            break;
          case MARGIN_FILL_OR_KILL:
            limitOrder = limitOrderCreator.get();
            limitOrder.addOrderFlag(BitfinexOrderFlags.FILL_OR_KILL);
            limitOrder.addOrderFlag(BitfinexOrderFlags.MARGIN);
            break;
          case MARGIN_LIMIT:
            limitOrder = limitOrderCreator.get();
            limitOrder.addOrderFlag(BitfinexOrderFlags.MARGIN);
            break;
          case MARGIN_STOP:
            stopOrder = stopOrderCreator.get();
            stopOrder.addOrderFlag(BitfinexOrderFlags.STOP);
            stopOrder.addOrderFlag(BitfinexOrderFlags.MARGIN);
            break;
          case MARGIN_STOP_LIMIT:
            stopLimitWarning();
            stopOrder = stopOrderCreator.get();
            stopOrder.addOrderFlag(BitfinexOrderFlags.STOP);
            stopOrder.addOrderFlag(BitfinexOrderFlags.MARGIN);
            break;
          case MARGIN_TRAILING_STOP:
            limitOrder = limitOrderCreator.get();
            limitOrder.addOrderFlag(BitfinexOrderFlags.TRAILING_STOP);
            limitOrder.addOrderFlag(BitfinexOrderFlags.MARGIN);
            break;
          case STOP:
            stopOrder = stopOrderCreator.get();
            stopOrder.addOrderFlag(BitfinexOrderFlags.STOP);
            break;
          case STOP_LIMIT:
            stopLimitWarning();
            stopOrder = stopOrderCreator.get();
            stopOrder.addOrderFlag(BitfinexOrderFlags.STOP);
            break;
          case TRAILING_STOP:
            limitOrder = limitOrderCreator.get();
            limitOrder.addOrderFlag(BitfinexOrderFlags.TRAILING_STOP);
            break;
          case LIMIT:
            limitOrder = limitOrderCreator.get();
            break;
          case MARGIN_MARKET:
          case MARKET:
            log.warn("Unexpected market order on book. Defaulting to limit order");
            limitOrder = limitOrderCreator.get();
            break;
          default:
            log.warn(
                "Unhandled Bitfinex order type [{}]. Defaulting to limit order", order.getType());
            limitOrder = limitOrderCreator.get();
            break;
        }
      } else {
        log.warn("Unknown Bitfinex order type [{}]. Defaulting to limit order", order.getType());
        limitOrder = limitOrderCreator.get();
      }

      if (limitOrder != null) {
        limitOrders.add(limitOrder);
      } else if (stopOrder != null) {
        hiddenOrders.add(stopOrder);
      }
    }

    return new OpenOrders(limitOrders, hiddenOrders);
  }

  private static void stopLimitWarning() {
    if (warnedStopLimit.compareAndSet(false, true)) {
      log.warn(
          "Found a stop-limit order. Bitfinex v1 API does not return limit prices for stop-limit "
              + "orders so these are returned as stop-at-market orders. This warning will only appear "
              + "once.");
    }
  }

  public static UserTrades adaptTradeHistory(final BitfinexTradeResponse[] trades, final String symbol) {

    final List<UserTrade> pastTrades = new ArrayList<>(trades.length);
    final CurrencyPair currencyPair = adaptCurrencyPair(symbol);

    for (final BitfinexTradeResponse trade : trades) {
      final OrderType orderType = trade.getType().equalsIgnoreCase("buy") ? OrderType.BID : OrderType.ASK;
      final Date timestamp = convertBigDecimalTimestampToDate(trade.getTimestamp());
      final BigDecimal fee = trade.getFeeAmount() == null ? null : trade.getFeeAmount().negate();
      pastTrades.add(
          new UserTrade(
              orderType,
              trade.getAmount(),
              currencyPair,
              trade.getPrice(),
              timestamp,
              trade.getTradeId(),
              trade.getOrderId(),
              fee,
              Currency.getInstance(trade.getFeeCurrency())));
    }

    return new UserTrades(pastTrades, TradeSortType.SortByTimestamp);
  }

  public static UserTrades adaptTradeHistoryV2(
          final List<org.knowm.xchange.bitfinex.v2.dto.trade.Trade> trades) {

    final List<UserTrade> pastTrades = new ArrayList<>(trades.size());

    for (final org.knowm.xchange.bitfinex.v2.dto.trade.Trade trade : trades) {
      final OrderType orderType = trade.getExecAmount().signum() >= 0 ? OrderType.BID : OrderType.ASK;
      final BigDecimal amount =
          trade.getExecAmount().signum() == -1
              ? trade.getExecAmount().negate()
              : trade.getExecAmount();
      final BigDecimal fee = trade.getFee() != null ? trade.getFee().negate() : null;
      pastTrades.add(
          new UserTrade.Builder()
              .type(orderType)
              .originalAmount(amount)
              .currencyPair(adaptCurrencyPair(trade.getSymbol()))
              .price(trade.getExecPrice())
              .timestamp(trade.getTimestamp())
              .id(trade.getId())
              .orderId(trade.getOrderId())
              .feeAmount(fee)
              .feeCurrency(Currency.getInstance(trade.getFeeCurrency()))
              .build());
    }

    return new UserTrades(pastTrades, TradeSortType.SortByTimestamp);
  }

  private static Date convertBigDecimalTimestampToDate(final BigDecimal timestamp) {

    final BigDecimal timestampInMillis = timestamp.multiply(new BigDecimal("1000"));
    return new Date(timestampInMillis.longValue());
  }

  public static ExchangeMetaData adaptMetaData(
          final List<CurrencyPair> currencyPairs, final ExchangeMetaData metaData) {

    final Map<CurrencyPair, CurrencyPairMetaData> pairsMap = metaData.getCurrencyPairs();
    final Map<Currency, CurrencyMetaData> currenciesMap = metaData.getCurrencies();

    // Remove pairs that are no-longer in use
    pairsMap.keySet().retainAll(currencyPairs);

    // Remove currencies that are no-longer in use
    final Set<Currency> currencies =
        currencyPairs.stream()
            .flatMap(pair -> Stream.of(pair.base, pair.counter))
            .collect(Collectors.toSet());
    currenciesMap.keySet().retainAll(currencies);

    // Add missing pairs and currencies
    for (final CurrencyPair c : currencyPairs) {
      if (!pairsMap.containsKey(c)) {
        pairsMap.put(c, null);
      }

      if (!currenciesMap.containsKey(c.base)) {
        currenciesMap.put(
            c.base,
            new CurrencyMetaData(
                2,
                null)); // When missing, add default meta-data with scale of 2 (Bitfinex's minimal
        // scale)
      }
      if (!currenciesMap.containsKey(c.counter)) {
        currenciesMap.put(c.counter, new CurrencyMetaData(2, null));
      }
    }

    return metaData;
  }

  /**
   * Flipped order of arguments to avoid type-erasure clash with {@link #adaptMetaData(List,
   * ExchangeMetaData)}
   *
   * @param exchangeMetaData The exchange metadata provided from bitfinex.json.
   * @param symbolDetails The symbol data fetced from Bitfinex.
   * @return The combined result.
   */
  public static ExchangeMetaData adaptMetaData(
          final ExchangeMetaData exchangeMetaData,
          final List<BitfinexSymbolDetail> symbolDetails,
          final Map<CurrencyPair, BigDecimal> lastPrices) {

    final Map<CurrencyPair, CurrencyPairMetaData> currencyPairs =
        exchangeMetaData.getCurrencyPairs();
    symbolDetails
        .parallelStream()
        .forEach(
            bitfinexSymbolDetail -> {
              final CurrencyPair currencyPair = adaptCurrencyPair(bitfinexSymbolDetail.getPair());

              // Infer price-scale from last and price-precision
              final BigDecimal last = lastPrices.get(currencyPair);

              if (last != null) {
                final int pricePercision = bitfinexSymbolDetail.getPrice_precision();
                final int priceScale = last.scale() + (pricePercision - last.precision());

                final CurrencyPairMetaData newMetaData =
                    new CurrencyPairMetaData(
                        currencyPairs.get(currencyPair) == null
                            ? null
                            : currencyPairs
                                .get(currencyPair)
                                .getTradingFee(), // Take tradingFee from static metaData if exists
                        bitfinexSymbolDetail.getMinimum_order_size(),
                        bitfinexSymbolDetail.getMaximum_order_size(),
                        priceScale,
                        null);
                currencyPairs.put(currencyPair, newMetaData);
              }
            });
    return exchangeMetaData;
  }

  public static ExchangeMetaData adaptMetaData(
          final BitfinexAccountFeesResponse accountFeesResponse, final ExchangeMetaData metaData) {
    final Map<Currency, CurrencyMetaData> currencies = metaData.getCurrencies();
    final Map<Currency, BigDecimal> withdrawFees = accountFeesResponse.getWithdraw();
    withdrawFees.forEach(
        (currency, withdrawalFee) -> {
          final CurrencyMetaData newMetaData =
              new CurrencyMetaData(
                  // Currency should have at least the scale of the withdrawalFee
                  currencies.get(currency) == null
                      ? withdrawalFee.scale()
                      : Math.max(withdrawalFee.scale(), currencies.get(currency).getScale()),
                  withdrawalFee);
          currencies.put(currency, newMetaData);
        });
    return metaData;
  }

  public static ExchangeMetaData adaptMetaData(
          final BitfinexAccountInfosResponse[] bitfinexAccountInfos, final ExchangeMetaData exchangeMetaData) {
    final Map<CurrencyPair, CurrencyPairMetaData> currencyPairs =
        exchangeMetaData.getCurrencyPairs();

    // lets go with the assumption that the trading fees are common across all trading pairs for
    // now.
    // also setting the taker_fee as the trading_fee for now.
    final CurrencyPairMetaData metaData =
        new CurrencyPairMetaData(
            bitfinexAccountInfos[0].getTakerFees().movePointLeft(2), null, null, null, null);
    currencyPairs
        .keySet()
        .parallelStream()
        .forEach(
            currencyPair ->
                currencyPairs.merge(
                    currencyPair,
                    metaData,
                    (oldMetaData, newMetaData) ->
                        new CurrencyPairMetaData(
                            newMetaData.getTradingFee(),
                            oldMetaData.getMinimumAmount(),
                            oldMetaData.getMaximumAmount(),
                            oldMetaData.getPriceScale(),
                            oldMetaData.getFeeTiers())));

    return exchangeMetaData;
  }

  public static List<FundingRecord> adaptFundingHistory(
          final BitfinexDepositWithdrawalHistoryResponse[] bitfinexDepositWithdrawalHistoryResponses) {
    final List<FundingRecord> fundingRecords = new ArrayList<>();
    for (final BitfinexDepositWithdrawalHistoryResponse responseEntry :
        bitfinexDepositWithdrawalHistoryResponses) {
      final String address = responseEntry.getAddress();
      final String description = responseEntry.getDescription();
      final Currency currency = Currency.getInstance(responseEntry.getCurrency());

      FundingRecord.Status status = FundingRecord.Status.resolveStatus(responseEntry.getStatus());
      if (status == null
          && responseEntry
              .getStatus()
              .equalsIgnoreCase("CANCELED")) // there's a spelling mistake in the protocol
      {
          status = FundingRecord.Status.CANCELLED;
      }

      String txnId = null;
      if (status == null || !status.equals(FundingRecord.Status.CANCELLED)) {
        /*
        sometimes the description looks like this (with the txn hash in it):
        "description":"a9d387cf5d9df58ff2ac4a338e0f050fd3857cf78d1dbca4f33619dc4ccdac82","address":"1Enx...

        and sometimes like this (with the address in it as well as the txn hash):
        "description":"3AXVnDapuRiAn73pjKe7gukLSx5813oFyn, txid: aa4057486d5f73747167beb9949a0dfe17b5fc630499a66af075abdaf4986987","address":"3AX...

        and sometimes when cancelled
        "description":"3LFVTLFZoDDzLCcLGDDQ7MNkk4YPe26Yva, expired","address":"3LFV...
         */

        String cleanedDescription =
            description.replace(",", "").replace("txid:", "").trim().toLowerCase();

        // Address will only be present for crypto payments. It will be null for all fiat payments
        if (address != null) {
          cleanedDescription = cleanedDescription.replace(address.toLowerCase(), "").trim();
        }

        // check its just some hex characters, and if so lets assume its the txn hash
        if (cleanedDescription.matches("^(0x)?[0-9a-f]+$")) {
          txnId = cleanedDescription;
        }
      }

      final FundingRecord fundingRecordEntry =
          new FundingRecord(
              address,
              responseEntry.getTimestamp(),
              currency,
              responseEntry.getAmount(),
              String.valueOf(responseEntry.getId()),
              txnId,
              responseEntry.getType(),
              status,
              null,
              null,
              description);

      fundingRecords.add(fundingRecordEntry);
    }
    return fundingRecords;
  }

  public static class OrdersContainer {

    private final long timestamp;
    private final List<LimitOrder> limitOrders;

    /**
     * Constructor
     *
     * @param timestamp The timestamp for the data fetched.
     * @param limitOrders The orders.
     */
    public OrdersContainer(final long timestamp, final List<LimitOrder> limitOrders) {

      this.timestamp = timestamp;
      this.limitOrders = limitOrders;
    }

    public long getTimestamp() {

      return timestamp;
    }

    public List<LimitOrder> getLimitOrders() {

      return limitOrders;
    }
  }

  ////// v2

  public static String adaptCurrencyPairsToTickersParam(final Collection<CurrencyPair> currencyPairs) {
    return currencyPairs == null || currencyPairs.isEmpty()
        ? "ALL"
        : currencyPairs.stream()
            .map(BitfinexAdapters::adaptCurrencyPair)
            .collect(Collectors.joining(","));
  }

  public static Ticker adaptTicker(
          final org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexTicker bitfinexTicker) {

    final BigDecimal last = bitfinexTicker.getLastPrice();
    final BigDecimal bid = bitfinexTicker.getBid();
    final BigDecimal bidSize = bitfinexTicker.getBidSize();
    final BigDecimal ask = bitfinexTicker.getAsk();
    final BigDecimal askSize = bitfinexTicker.getAskSize();
    final BigDecimal high = bitfinexTicker.getHigh();
    final BigDecimal low = bitfinexTicker.getLow();
    final BigDecimal volume = bitfinexTicker.getVolume();

    final CurrencyPair currencyPair =
        CurrencyPairDeserializer.getCurrencyPairFromString(bitfinexTicker.getSymbol().substring(1));

    return new Ticker.Builder()
        .currencyPair(currencyPair)
        .last(last)
        .bid(bid)
        .ask(ask)
        .high(high)
        .low(low)
        .volume(volume)
        .bidSize(bidSize)
        .askSize(askSize)
        .build();
  }

  public static Trade adaptPublicTrade(final BitfinexPublicTrade trade, final CurrencyPair currencyPair) {

    final OrderType orderType = trade.getType();
    final BigDecimal amount = trade.getAmount();
    final BigDecimal price = trade.getPrice();
    final Date date = DateUtils.fromMillisUtc(trade.getTimestamp());
    final String tradeId = String.valueOf(trade.getTradeId());
    return new Trade(
        orderType, amount == null ? null : amount.abs(), currencyPair, price, date, tradeId);
  }

  public static Trades adaptPublicTrades(final BitfinexPublicTrade[] trades, final CurrencyPair currencyPair) {

    final List<Trade> tradesList = new ArrayList<>(trades.length);
    long lastTradeId = 0;
    for (final BitfinexPublicTrade trade : trades) {
      final long tradeId = trade.getTradeId();
      if (tradeId > lastTradeId) {
        lastTradeId = tradeId;
      }
      tradesList.add(adaptPublicTrade(trade, currencyPair));
    }
    return new Trades(tradesList, lastTradeId, TradeSortType.SortByID);
  }

    public static String adaptTimeFrame(final TimeFrame timeFrame) {
        return timeFrame.getCode();
    }
}
