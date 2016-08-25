package com.parallax.jpa.repos;

import com.parallax.jpa.model.Transaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * An interface to interact with the "transactions" DB table.
 */
@Repository
public interface TransactionRepo extends CrudRepository<Transaction, String> {

    /**
     * Returns either processed or failed transactions
     * @param processed true for processed, false for failed
     * @return list of relevant transactions
     */
    List<Transaction> findByProcessed(boolean processed);

}
