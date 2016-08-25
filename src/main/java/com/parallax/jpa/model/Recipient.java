package com.parallax.jpa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "recipients")
@Cacheable
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    private String publicKey;
    private double amount;
    @JsonIgnore
    private String transactionHash;

    public Recipient() {
    }

    public Recipient(String publicKey, double amount, String transactionHash) {
        this.publicKey = publicKey;
        this.amount = amount;
        this.transactionHash = transactionHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipient recipient = (Recipient) o;
        return Objects.equals(amount, recipient.amount) &&
                Objects.equals(id, recipient.id) &&
                Objects.equals(publicKey, recipient.publicKey) &&
                Objects.equals(transactionHash, recipient.transactionHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publicKey, amount, transactionHash);
    }
}
