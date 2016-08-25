package com.parallax.http.client;

import com.parallax.jpa.model.LastTx;
import com.parallax.dto.multiaddr.MultiaddrResponse;
import com.parallax.dto.txdetail.TxDetailResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Blockchain API client.
 */
public interface ChainzApi {

    /**
     * Blockchain "multiaddr" API endpoint - async.
     * @param query type of request, should be "multiaddr"
     * @param addresses an array of public addresses separated by pipe
     * @param numberOfTxs result set size
     * @param apiKey api key
     * @return "multiaddr" API response
     */
    @GET("slr/api.dws")
    Observable<MultiaddrResponse> getPublicAddrInfoRx(@Query("q") String query,
                                                      @Query("active") String addresses,
                                                      @Query("n") int numberOfTxs,
                                                      @Query("key") String apiKey);

    /**
     * Blockchain "multiaddr" API endpoint - sync.
     * @param query type of request, should be "multiaddr"
     * @param addresses an array of public addresses separated by pipe
     * @param numberOfTxs result set size
     * @param apiKey api key
     * @return "multiaddr" API response
     */
    @GET("slr/api.dws")
    Call<MultiaddrResponse> getPublicAddrInfo(@Query("q") String query,
                                              @Query("active") String addresses,
                                              @Query("n") int numberOfTxs,
                                              @Query("key") String apiKey);

    /**
     * Transaction detailes blockchain API.
     * @param coin blockchain currency, should be "slr"
     * @param txId transaction hash
     * @param jsonFormat we need json response back, so should be true
     * @return transaction details info from blockchain API
     */
    @GET("explorer/tx.data.dws")
    Call<TxDetailResponse> getTransactionDetail(@Query("coin") String coin,
                                                @Query("id") String txId,
                                                @Query("fmt.js") boolean jsonFormat);

    /**
     * Blockchain API call for current balance for a public address.
     * @param query type of request, should be "getbalance"
     * @param address public address
     * @param key api key
     * @return current balance info from blockchain API
     */
    @GET("slr/api.dws")
    Call<BigDecimal> getBalance(@Query("q") String query,
                                @Query("a") String address,
                                @Query("key") String key);

    /**
     * Blockchain API call for last transactions.
     * @param query type of request, should be "lasttxs"
     * @param key api key
     * @return fresh transaction info from blockchain API
     */
    @GET("slr/api.dws")
    Call<List<LastTx>> getLastTransactions(@Query("q") String query,
                                           @Query("key") String key);

}
