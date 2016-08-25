package com.parallax.jpa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.parallax.serializers.SolyaraDateDeserializer;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Entity
@Cacheable
@Table(name = "processed_tx_updates")
public class LastTx {

    @Id
    private String hash;

    @Transient
    private int confirmations;

    @Transient
    private BigDecimal total;

    @Transient
    private Date time;

    public LastTx() {
    }

    public LastTx(String hash) {
        this.hash = hash;
    }

    public LastTx(String hash, int confirmations, BigDecimal total, Date time) {
        this.hash = hash;
        this.confirmations = confirmations;
        this.total = total;
        this.time = time;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LastTx lastTx = (LastTx) o;
        return Objects.equals(hash, lastTx.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }
}
