package com.parallax.dto.multiaddr;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.parallax.serializers.SolyaraDateDeserializer;

import java.util.Date;

@JsonDeserialize(using = SolyaraDateDeserializer.class)
public class Tx {
    private String hash;
    private int confirmations;
    private Long change;
    private Date time_utc;

    public Tx(String hash, int confirmations, Long change, Date time_utc) {
        this.hash = hash;
        this.confirmations = confirmations;
        this.change = change;
        this.time_utc = time_utc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tx)) return false;

        Tx tx = (Tx) o;

        return hash.equals(tx.hash);

    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    public Object getChange() {
        return change;
    }

    public void setChange(Long change) {
        this.change = change;
    }

    public Date getTime_utc() {
        return time_utc;
    }

    public void setTime_utc(Date time_utc) {
        this.time_utc = time_utc;
    }
}
