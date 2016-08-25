package com.parallax.dto.multiaddr;

import java.util.List;

public class MultiaddrResponse {

    private List<Address> addresses;
    private List<Tx> txs;

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<Tx> getTxs() {
        return txs;
    }

    public void setTxs(List<Tx> txs) {
        this.txs = txs;
    }
}
