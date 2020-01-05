package org.knowm.xchange.bitfinex.v2;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.knowm.xchange.bitfinex.v2.dto.BitfinexExceptionV2;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexCandle;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexPublicFundingTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexPublicTrade;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.BitfinexTicker;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.Status;

@Path("v2")
@Produces(MediaType.APPLICATION_JSON)
public interface Bitfinex {

  @GET
  @Path("tickers")
  BitfinexTicker[] getTickers(@QueryParam("symbols") String symbols)
      throws IOException, BitfinexExceptionV2;

    // https://docs.bitfinex.com/reference#rest-public-candles
    @GET
    @Path("candles/trade:{timeFrame}:{symbol}/last")
    BitfinexCandle getTradingCandleLast(
            @PathParam("symbol") String symbol,
            @PathParam("timeFrame") String timeFrame,
            @QueryParam("limit") int limit,
            @QueryParam("start") long startTimestamp,
            @QueryParam("end") long endTimestamp,
            @QueryParam("sort") int sort)
            throws IOException, BitfinexExceptionV2;

    // https://docs.bitfinex.com/reference#rest-public-candles
    @GET
    @Path("candles/trade:{timeFrame}:{symbol}/hist")
    BitfinexCandle[] getTradingCandlesHist(
            @PathParam("symbol") String symbol,
            @PathParam("timeFrame") String timeFrame,
            @QueryParam("limit") int limit,
            @QueryParam("start") long startTimestamp,
            @QueryParam("end") long endTimestamp,
            @QueryParam("sort") int sort)
            throws IOException, BitfinexExceptionV2;

    // https://docs.bitfinex.com/reference#rest-public-candles
    @GET
    @Path("candles/trade:{timeFrame}:{symbol}:p{period}/last")
    BitfinexCandle getFundingCandleLast(
            @PathParam("symbol") String symbol,
            @PathParam("timeFrame") String timeFrame,
            @PathParam("period") int period,
            @QueryParam("limit") int limit,
            @QueryParam("start") long startTimestamp,
            @QueryParam("end") long endTimestamp,
            @QueryParam("sort") int sort)
            throws IOException, BitfinexExceptionV2;

    // https://docs.bitfinex.com/reference#rest-public-candles
    @GET
    @Path("candles/trade:{timeFrame}:{symbol}:p{period}/hist")
    BitfinexCandle[] getFundingCandlesHist(
            @PathParam("symbol") String symbol,
            @PathParam("timeFrame") String timeFrame,
            @PathParam("period") int period,
            @QueryParam("limit") int limit,
            @QueryParam("start") long startTimestamp,
            @QueryParam("end") long endTimestamp,
            @QueryParam("sort") int sort)
            throws IOException, BitfinexExceptionV2;

    // https://docs.bitfinex.com/reference#rest-public-candles
    @GET
    @Path("candles/trade:{timeFrame}:{symbol}:a{aggr}:p{periodStart}:p{periodEnd}/last")
    BitfinexCandle getAggregateFundingCandleLast(
            @PathParam("symbol") String symbol,
            @PathParam("timeFrame") String timeFrame,
            @PathParam("aggr") int aggr,
            @PathParam("periodStart") int periodStart,
            @PathParam("periodEnd") int periodEnd,
            @QueryParam("limit") int limit,
            @QueryParam("start") long startTimestamp,
            @QueryParam("end") long endTimestamp,
            @QueryParam("sort") int sort)
            throws IOException, BitfinexExceptionV2;

    // https://docs.bitfinex.com/reference#rest-public-candles
    @GET
    @Path("candles/trade:{timeFrame}:{symbol}:a{aggr}:p{periodStart}:p{periodEnd}/hist")
    BitfinexCandle[] getAggregateFundingCandlesHist(
            @PathParam("symbol") String symbol,
            @PathParam("timeFrame") String timeFrame,
            @PathParam("aggr") int aggr,
            @PathParam("periodStart") int periodStart,
            @PathParam("periodEnd") int periodEnd,
            @QueryParam("limit") int limit,
            @QueryParam("start") long startTimestamp,
            @QueryParam("end") long endTimestamp,
            @QueryParam("sort") int sort)
            throws IOException, BitfinexExceptionV2;

  @GET
  @Path("status/{type}")
  List<Status> getStatus(@PathParam("type") String type, @QueryParam("keys") String symbols)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("/trades/{symbol}/hist")
  BitfinexPublicFundingTrade[] getPublicFundingTrades(
      @PathParam("symbol") String fundingSymbol,
      @QueryParam("limit") int limit,
      @QueryParam("start") long startTimestamp,
      @QueryParam("end") long endTimestamp,
      @QueryParam("sort") int sort)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("/trades/{symbol}/hist")
  BitfinexPublicTrade[] getPublicTrades(
      @PathParam("symbol") String fundingSymbol,
      @QueryParam("limit") int limit,
      @QueryParam("start") long startTimestamp,
      @QueryParam("end") long endTimestamp,
      @QueryParam("sort") int sort)
      throws IOException, BitfinexExceptionV2;
}
