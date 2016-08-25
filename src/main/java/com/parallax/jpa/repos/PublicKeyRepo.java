package com.parallax.jpa.repos;

import com.parallax.jpa.model.PublicKey;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * An interface to interact with the "public_keys" DB table.
 */
@Repository
public interface PublicKeyRepo extends CrudRepository<PublicKey, String> {

}
