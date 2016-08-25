package com.parallax.dto.multiaddr;

public class Address {

    private String address;
    private long total_sent;
    private long total_received;
    private long final_balance;
    private int n_tx;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getTotal_sent() {
        return total_sent;
    }

    public void setTotal_sent(long total_sent) {
        this.total_sent = total_sent;
    }

    public long getTotal_received() {
        return total_received;
    }

    public void setTotal_received(long total_received) {
        this.total_received = total_received;
    }

    public long getFinal_balance() {
        return final_balance;
    }

    public void setFinal_balance(long final_balance) {
        this.final_balance = final_balance;
    }

    public int getN_tx() {
        return n_tx;
    }

    public void setN_tx(int n_tx) {
        this.n_tx = n_tx;
    }
}
