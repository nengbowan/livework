package com.cz.gameplat.live.core.dto.ds;

import java.io.Serializable;

public class SBBalanceRootDto  implements Serializable {
    private String errcode;

    private Double amount;

    public String getErrcode() {
        return errcode;
    }

    public void setErrcode(String errcode) {
        this.errcode = errcode;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
