package com.parallax.http.client;

import com.parallax.jpa.model.Transaction;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.List;

/**
 * Backend transaction uupdates API.
 */
public interface TxUpdateApi {

    /**
     * Sends transaction updates to the backend.
     * @param transactions an array of transactions with senders and receivers.
     * @return backend response as a string
     */
    @POST("transaction/block_info")
    Call<String> sendNewTransactions(@Body List<Transaction> transactions);

}
