package com.parallax.jpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "public_keys")
public class PublicKey {

    @Id
    @Column(name = "key_value")
    private String key;

    public PublicKey() {
    }

    public PublicKey(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

}
