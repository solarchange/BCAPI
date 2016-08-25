package com.parallax.jpa.repos;

import com.parallax.jpa.model.LastTx;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * An interface to interact with the "processed_tx_updates" DB table.
 */
@Repository
public interface LastTxRepo extends CrudRepository<LastTx, String> {

}
