package com.cz.gameplat.live.core.dto.ds;

public class SBBalanceDto{
    private SBBalanceRootDto root;

    public SBBalanceRootDto getRoot() {
        return root;
    }

    public void setRoot(SBBalanceRootDto root) {
        this.root = root;
    }

    @Override
    public String toString(){
        return com.alibaba.fastjson.JSONObject.toJSONString(this);
    }
}
