package com.parallax.http;

import com.parallax.dto.multiaddr.MultiaddrResponse;
import com.parallax.dto.multiaddr.Tx;
import com.parallax.dto.txdetail.TxDetailResponse;
import com.parallax.jpa.model.PublicKey;
import com.parallax.jpa.model.Recipient;
import com.parallax.jpa.model.Sender;
import com.parallax.jpa.model.Transaction;
import com.parallax.jpa.repos.PublicKeyRepo;
import com.parallax.jpa.repos.TransactionRepo;
import com.parallax.service.SolrCoinBlockChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class provides endpoints as spedified in the BE requests API document. It contains "get info", "get all info" and
 * "set public keys" endpoints. Please refer to the release notes for the usage examples.
 */
@Controller
public class BEClientApiEndpoints {

    @Autowired
    private PublicKeyRepo publicKeyRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private SolrCoinBlockChainService solrCoinBlockChainService;

    /**
     * Sets and updates a list of public keys that will be tracked by the transaction auto update mechanism. It consumes and produces json.
     * @param publicKeysList an array of public keys as a request body payload. Example: ["8LDw7vt9ixFuWwAnKKdDJ3ggsSTLxMJyoM"]
     * @return json object with the following info: transactions_processed (total number of transactions for all provided public keys),
     * keys_processed (total number of public keys processed), status (should be "OK").
     */
    @Transactional
    @RequestMapping(value = "/set-public-keys",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> setPublicKeysList(@RequestBody(required = false) List<String> publicKeysList) {
        // clean all existing public keys from the db
        publicKeyRepo.deleteAll();
        int transactions = 0;
        long publicKeysProcessedSize = 0;
        if (publicKeysList != null && !publicKeysList.isEmpty()) {
            // retrieve all public keys from request
            List<PublicKey> publicKeys = publicKeysList.stream().map(PublicKey::new).collect(Collectors.toList());
            // fetch relevant transaction info from the blockchain API if necessary
            transactions = performTransactionDataFlow(publicKeysList).size();
            // get total number of processed transactions
            publicKeysProcessedSize = StreamSupport.stream(publicKeyRepo.save(publicKeys).spliterator(), false).count();
        }
        // compose and response
        Map status = new HashMap<>();
        status.put("keys_processed", publicKeysProcessedSize);
        status.put("transactions_processed", transactions);
        status.put("status", "OK");
        return new ResponseEntity<>(status, HttpStatus.ACCEPTED);
    }

    /**
     * @return transaction and current balance information for all public keys that are currently being tracked. Response is a json object
     * that consists of two arrays. One array has info about the current balance for every tracked public key. Another array has all transaction details
     * that include at least one of the tracked public keys either as a recipient or as a sender. Every transaction has the following info:
     * transaction hash, timestamp, array of receivers, array of senders. Every sender and receiver carries information about the corresponding public key
     * and it's balance at the point of time when this transaction was processed.
     */
    @Transactional
    @RequestMapping(value = "/get-all-info", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getAllInfo() {
        // get all tracked public keys
        List<String> publicKeys = ((List<PublicKey>) publicKeyRepo.findAll())
                .stream()
                .map(PublicKey::getKey)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        // request transaction details for every public key and compose response
        result.put("txs", performTransactionDataFlow(publicKeys));
        result.put("balances", getBalanceData(publicKeys));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Returns balance and transactions info similar to {@link BEClientApiEndpoints#getAllInfo()} endpoint but
     * also saves the public key to the database if it's not there yet. Retrieves all transaction info if necessary
     * and stores it in the database for future use as well.
     *
     * @param publicKeysString an array of public keys separated by the pipe delimiter.
     * Example: /get-info?pubKeys=8LDw7vt9ixFuWwAnKKdDJ3ggsSTLxMJyoM|8MBM9RJ3TqroKqTSBUqresMfKCGzwo2pus
     * @return the same response as {@link BEClientApiEndpoints#getAllInfo()} but limits the results to
     * supplied public addresses only
     */
    @Transactional
    @RequestMapping(value = "/get-info", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getInfo(@RequestParam(value = "pubKeys") String publicKeysString) {
        // parse supplied public keys from request
        List<String> publicKeys = Arrays.asList(publicKeysString.split("\\|"));

        // check if public address exists in the DB
        publicKeys.forEach(publicKey -> {
            if (!publicKeyRepo.exists(publicKey)) {
                // save it if doesn't exist
                publicKeyRepo.save(new PublicKey(publicKey));
            }
        });

        // compose and return response
        Map<String, Object> result = new HashMap<>();
        // fetch relevant transaction info from the remote API if necessary
        result.put("txs", performTransactionDataFlow(publicKeys));
        result.put("balances", getBalanceData(publicKeys));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Makes a query to the "getbalance" blockchain api for current balances.
     * @param addresses public addresses to query for
     * @return current balance info for supplied public addresses
     */
    private Map<String, BigDecimal> getBalanceData(List<String> addresses) {
        Map<String, BigDecimal> result = new ConcurrentHashMap<>();
        LocalDateTime then = LocalDateTime.now();
        addresses.parallelStream().forEach(address -> result.put(address, solrCoinBlockChainService.getBalance(address)));
        System.out.println("Balance data takes " + Duration.between(then, LocalDateTime.now()).toMillis());
        System.out.println();
        return result;
    }

    /**
     * Returns transaction info with relevant senders and receivers for supplied public addresses.
     * @param publicKeys public keys to process
     * @return a list of transactions that have at least one supplied public key either as a sender or as a receiver.
     */
    private List<Transaction> performTransactionDataFlow(List<String> publicKeys) {
        LocalDateTime then = LocalDateTime.now();
        // query total transaction records number for all public addresses combined
        int totalTxs = solrCoinBlockChainService.getTotalTransactionsCount(publicKeys);
        System.out.println("Tx num takes " + Duration.between(then, LocalDateTime.now()).toMillis());
        then = LocalDateTime.now();
        // get transaction summary for all public addresses from the dws API
        List<Transaction> result = solrCoinBlockChainService.getTransactions(publicKeys, totalTxs)
                // get txs element
                .map(MultiaddrResponse::getTxs)
                // filter out duplicate hashes if they are present
                .map(txes -> {
                    Set<Tx> txSet = new HashSet<>();
                    txes.forEach(txSet::add);
                    return txSet;
                })
                // set transaction info to the response
                .map(txes -> {
                    List<Transaction> transactions = new LinkedList<>();
                    txes.forEach(tx -> {
                        Transaction txDto = new Transaction();
                        txDto.setHash(tx.getHash());
                        txDto.setDate(tx.getTime_utc().getTime());
                        transactions.add(txDto);
                    });
                    return transactions;
                })
                // fetch transaction details either from the database or from remote API
                .map(transactions -> {
                            LocalDateTime then1 = LocalDateTime.now();
                            List<Transaction> result1 = transactions
                                    .parallelStream()
                                    .map(txDto -> {
                                        // if transaction details exist in the DB, return them
                                        Transaction currentTransaction = transactionRepo.findOne(txDto.getHash());
                                        if (currentTransaction != null) {
                                            return currentTransaction;
                                        // otherwise query from remote API and return
                                        } else {
                                            TxDetailResponse txDetail = solrCoinBlockChainService.getTransactionDetail(txDto.getHash());

                                            Set<Sender> senders = txDetail.getVin().parallelStream()
                                                    .map(vin -> new Sender(vin.getAddr(), vin.getAmount(), txDto.getHash()))
                                                    .collect(Collectors.toSet());
                                            txDto.setSenders(senders);

                                            Set<Recipient> recipients = txDetail.getVout().parallelStream()
                                                    .map(vout -> new Recipient(vout.getAddr(), vout.getAmount(), txDto.getHash()))
                                                    .collect(Collectors.toSet());
                                            txDto.setRecipients(recipients);

                                            transactionRepo.save(txDto);
                                            return txDto;
                                        }
                                    })
                                    .collect(Collectors.toList());
                            System.out.println("Tx processing takes " + Duration.between(then1, LocalDateTime.now()).toMillis());
                            return result1;
                        }
                )
                .toBlocking()
                .single();
        System.out.println("Tx details takes " + Duration.between(then, LocalDateTime.now()).toMillis());
        return result;
    }
}
