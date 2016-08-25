package com.parallax.service;

import com.parallax.dto.multiaddr.Address;
import com.parallax.dto.multiaddr.MultiaddrResponse;
import com.parallax.dto.txdetail.TxDetailResponse;
import com.parallax.http.client.ChainzApi;
import com.parallax.http.client.TxUpdateApi;
import com.parallax.jpa.model.LastTx;
import com.parallax.jpa.model.Transaction;
import com.parallax.jpa.repos.LastTxRepo;
import com.parallax.jpa.repos.TransactionRepo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rx.Observable;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Contains logic to work with the remote blockchain api and backend api.
 */
@Service
public class SolrCoinBlockChainService {

    private static final Log LOG = LogFactory.getLog(SolrCoinBlockChainService.class);
    private static final int TWENTY_SECONDS = 20 * 1000;

    @Value("${chainz.api.url}")
    private String chainzApiUrl;

    @Value("${chainz.api.key}")
    private String chainzApiKey;

    @Value("${be.api.url}")
    private String txUpdateUrl;

    private ChainzApi chainzApi;

    private TxUpdateApi txUpdateApi;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private LastTxRepo lastTxRepo;

    /**
     * Constructs REST clients for both backend and blockchain api.
     */
    @PostConstruct
    public void init() {
        Retrofit chainzApiBuilder = new Retrofit.Builder().baseUrl(chainzApiUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        Retrofit txUpdateApiBuilder = new Retrofit.Builder().baseUrl(txUpdateUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        chainzApi = chainzApiBuilder.create(ChainzApi.class);
        txUpdateApi = txUpdateApiBuilder.create(TxUpdateApi.class);
    }

    /**
     * Returns total transaction count for all supplied public keys by querying "multiaddr" blockchain API.
     * @param addrs array of public keys
     * @return total transaction number
     */
    public int getTotalTransactionsCount(List<String> addrs) {
        // compose query parameter for "multiaddr" blockchain api
        String multiAddr = addrs.stream().reduce((s, s2) -> s.concat("|").concat(s2)).get();
        try {
            // get summary info for all transactions for public addresses
            MultiaddrResponse multiaddrResponse = chainzApi.getPublicAddrInfo("multiaddr", multiAddr, 0, chainzApiKey).execute().body();
            // compute total transaction count
            int totalTxs = multiaddrResponse.getAddresses().stream().mapToInt(Address::getN_tx).sum();
            return totalTxs;
        } catch (IOException e) {
            LOG.error("Couldn't get total transaction count for: " + multiAddr, e);
            return -1;
        }
    }

    /**
     * Calls "multiaddr" blockchain API to get all transactions for supplied public addresses.
     * @param addrs addrs array of public keys
     * @param numTxs number of transactions to return
     * @return deserialized blockchain "multiaddr" API response
     */
    public Observable<MultiaddrResponse> getTransactions(List<String> addrs, int numTxs) {
        // compose query parameter for "multiaddr" blockchain api
        String multiAddr = addrs.stream().reduce((s, s2) -> s.concat("|").concat(s2)).get();
        // return "multiaddr" blockchain response
        return chainzApi.getPublicAddrInfoRx("multiaddr", multiAddr, numTxs, chainzApiKey);
    }

    /**
     * Fetches current balanse from the blockchain API.
     * @param addr public address to get balance for
     * @return current balance
     */
    public BigDecimal getBalance(String addr) {
        try {
            return chainzApi.getBalance("getbalance", addr, chainzApiKey).execute().body();
        } catch (IOException e) {
            LOG.error("Couldn't get balance for addr: " + addr, e);
            return null;
        }
    }

    /**
     * Fetches transaction detailed for a given transaction hash from the blockchain API.
     * @param txId transaction hash
     * @return deserialized transaction details
     */
    public TxDetailResponse getTransactionDetail(String txId) {
        try {
            return chainzApi.getTransactionDetail("slr", txId, true).execute().body();
        } catch (IOException e) {
            LOG.error("Couldn't get tx details for hash: " + txId, e);
            return null;
        }
    }

    /**
     * Fetched last transaction updates from the "lasttxs" blockchain API
     * @return deserialized last transaction response
     */
    public List<LastTx> getLastTransactions() {
        try {
            return chainzApi.getLastTransactions("lasttxs", chainzApiKey).execute().body();
        } catch (IOException e) {
            LOG.error("Couldn't get last transaction details");
        }
        return Collections.emptyList();
    }

    /**
     * Retry mechanism for backend transaction updates flow. If backend hasn't been updated, retries twice with 20 second interval.
     * @param newTransactions new transactions to update backend with
     * @throws Exception if something goes wrong
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(TWENTY_SECONDS))
    public void sendTxUpdates(List<Transaction> newTransactions) throws Exception {
        LOG.info("Attempting to update backend with new transactions");
        Response<String> response = txUpdateApi.sendNewTransactions(newTransactions).execute();
        if (response.isSuccessful()) {
            LOG.info("Transaction update has been successful");
            newTransactions.forEach(transaction -> transaction.setProcessed(true));
            transactionRepo.save(newTransactions);
            LOG.info("Saved updated transactions to local data store, size: " + newTransactions.size());
            return;
        }
        LOG.error("Server response indicates problems: Status = " + response.code() + ", body = " + response.errorBody().string());
        throw new Exception("Unable to update backend with new transaction data");
    }

    /**
     * A recovery mechanism for that case when backend update has failed and subsequent retries have failed as well.
     * Saves failed updates to the database for future processing.
     * @param e exception
     * @param newTransactions failed transactions
     */
    @Recover
    public void saveFailedTxUpdates(Throwable e, List<Transaction> newTransactions) {
        LOG.error("Backend tx update failed: " + e.getMessage());
        newTransactions.forEach(transaction -> transaction.setProcessed(false));
        transactionRepo.save(newTransactions);
        LOG.info("Saved failed transactions to local data store, size: " + newTransactions.size());
    }

}
