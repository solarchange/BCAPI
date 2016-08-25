package com.parallax;

import com.parallax.dto.txdetail.TxDetailResponse;
import com.parallax.jpa.model.*;
import com.parallax.jpa.repos.LastTxRepo;
import com.parallax.jpa.repos.PublicKeyRepo;
import com.parallax.jpa.repos.TransactionRepo;
import com.parallax.service.SolrCoinBlockChainService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main application class. Entry point for the app and also has transaction update logic.
 */
@SpringBootApplication
@EnableWebMvc
@EnableScheduling
@EnableCaching
@EnableRetry
@EnableTransactionManagement
@PropertySources(value = {@PropertySource("classpath:application.properties")})
public class BcapiApplication extends SpringBootServletInitializer {

    private final static Log LOG = LogFactory.getLog(BcapiApplication.class);

    private static final int ONE_MINUTE = 60 * 1000;

    @Autowired
    private SolrCoinBlockChainService solrCoinBlockChainService;

    @Autowired
    private PublicKeyRepo publicKeyRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private LastTxRepo lastTxRepo;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BcapiApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(BcapiApplication.class, args);
    }

    /**
     * Checks for transaction updates every minute with an initial delay of one minute. Using "lasttxs" endpoint
     * of the blockchain API to check for updates. Filters out only relevant transactions (that are related to tracked public keys)
     * and sends them for update to backend. If there are any failed updates in the database they will be included as well.
     * @throws Exception if something goes wrong
     */
    @Scheduled(fixedRate = ONE_MINUTE, initialDelay = ONE_MINUTE)
    public void checkForUpdates() throws Exception {
        // get all registered public keys from the DB
        List<PublicKey> registeredKeys = (List<PublicKey>) publicKeyRepo.findAll();
        LOG.info("Checking for new txUpdates...");
        // requesting new transactions info
        List<LastTx> lastTransactions = solrCoinBlockChainService.getLastTransactions();
        List<Transaction> txUpdates = lastTransactions
                .stream()
                // filter out only those that do not exist in our processed updates table
                .filter(lastTx -> !lastTxRepo.exists(lastTx.getHash()))
                // get details for those that are not processed
                .map(txCandidateForUpdate -> {
                    TxDetailResponse transactionDetail = solrCoinBlockChainService.getTransactionDetail(txCandidateForUpdate.getHash());
                    transactionDetail.setDate(txCandidateForUpdate.getTime());
                    return transactionDetail;
                })
                // remove items that are not relevant to registered public addresses
                .filter(txDetailsCandidateForUpdate -> {
                    if (transactionBelongsToRegisteredPublicAddress(registeredKeys, txDetailsCandidateForUpdate)) {
                        LOG.info("Transaction with hash " + txDetailsCandidateForUpdate.getHash() + " includes a known address.");
                        return true;
                    }
                    LOG.info("Transaction with hash " + txDetailsCandidateForUpdate.getHash() + " does not include a known address.");
                    return false;
                })
                // convert remaining to dtos that we send for updates to backend
                .map(txDetailsForUpdate -> {
                    Transaction txDto = new Transaction();
                    txDto.setHash(txDetailsForUpdate.getHash());
                    txDto.setDate(txDetailsForUpdate.getDate().getTime());
                    Set<Sender> senders = txDetailsForUpdate.getVin().parallelStream()
                            .map(vin -> new Sender(vin.getAddr(), vin.getAmount(), txDto.getHash()))
                            .collect(Collectors.toSet());
                    txDto.setSenders(senders);
                    Set<Recipient> recipients = txDetailsForUpdate.getVout().parallelStream()
                            .map(vout -> new Recipient(vout.getAddr(), vout.getAmount(), txDto.getHash()))
                            .collect(Collectors.toSet());
                    txDto.setRecipients(recipients);
                    return txDto;
                })
                .collect(Collectors.toList());
        // retrieving all failed updates from db to send them for update as well
        List<Transaction> failedTransactionUpdates = transactionRepo.findByProcessed(false);
        if (!failedTransactionUpdates.isEmpty()) {
            LOG.info("Including previously failed updates, size: " + failedTransactionUpdates.size());
            txUpdates.addAll(failedTransactionUpdates);
        }
        // if we have something to send for update, calling the backend
        if (!txUpdates.isEmpty()) {
            solrCoinBlockChainService.sendTxUpdates(txUpdates);
        }
        // persist processed relevant transactions to the database
        lastTxRepo.save(lastTransactions);
        LOG.info("Saved latest transaction hashes to tx update journal, size: " + lastTransactions.size());
    }

    /**
     * Checks if any of transaction senders or receivers contain at least one tracked public address
     * @param registeredKeys registered public keys
     * @param txDetail transaction detail
     * @return true if the transaction contains at least one relevant public keys, false otherwise
     */
    private boolean transactionBelongsToRegisteredPublicAddress(List<PublicKey> registeredKeys, TxDetailResponse txDetail) {
        List<String> keys = registeredKeys.stream().map(PublicKey::getKey).collect(Collectors.toList());
        return txDetail.getVin().stream().anyMatch(vin -> keys.contains(vin.getAddr()))
                || txDetail.getVout().stream().anyMatch(vout -> keys.contains(vout.getAddr()));
    }
}
