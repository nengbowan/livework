package com.cz.gameplat.live.core.api;

import com.cz.gameplat.live.core.dto.ds.*;
import org.apache.commons.collections.CollectionUtils;
import org.json.XML;
import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.utils.*;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.parser.*;
import com.cz.framework.exception.*;

import java.text.SimpleDateFormat;
import java.util.*;
import com.cz.gameplat.live.core.api.ds.bean.*;
import org.slf4j.*;

@Component
public class DsApi implements GameApi
{
    private static final Logger logger;
    @Resource
    private DsConfig dsConfig;

    private String agentusername = "hj789";

    private String agentpwd = "aabb1122";

    private String baseUrl = "http://117.27.251.17:23280";

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Double getBalance(final String account) throws Exception {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("hashCode", this.dsConfig.getSecurityKey());
        map.put("command", "GET_BALANCE");
        final Map<String, String> params = new HashMap<String, String>();
        params.put("username", this.dsConfig.getAccount(account));
        params.put("password", this.dsConfig.getPassword(account));
        map.put("params", params);
        final String reqJson = JSON.toJSONString((Object)map);
        String respJson = null;
        try {
            DsApi.logger.info("[DS]\u7528\u6237\u67e5\u8be2\u4f59\u989d\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a" + reqJson);
            respJson = HttpClientUtils.doPost(this.dsConfig.getHost(), reqJson);
            final TypeReference<DsRespBody<Map<String, String>>> typeRef = new TypeReference<DsRespBody<Map<String, String>>>() {};
            final DsRespBody<Map<String, String>> respBody = (DsRespBody<Map<String, String>>)JSON.parseObject(respJson, (TypeReference)typeRef, new Feature[0]);
            final String errorCode = respBody.getErrorCode();
            if ("0".equals(errorCode)) {
                final Map<String, String> respParams = respBody.getParams();
                return Double.valueOf(respParams.get("balance"));
            }
            if ("6605".equals(errorCode)) {
                this.play(account, null, null, false, null);
                return 0.0;
            }
            throw new BusinessException("DS/" + errorCode, respBody.getErrorMessage(), (Object[])null);
        }
        catch (Exception ex) {
            DsApi.logger.error("[DS]\u7528\u6237\u67e5\u8be2\u4f59\u989d\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson);
            throw ex;
        }
    }
    
    @Override
    public boolean transfer(final String account, final Double amount, final String orderNum) throws Exception {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("hashCode", this.dsConfig.getSecurityKey());
        if (amount > 0.0) {
            map.put("command", "DEPOSIT");
        }
        else {
            map.put("command", "WITHDRAW");
        }
        final Map<String, String> params = new HashMap<String, String>();
        params.put("username", this.dsConfig.getAccount(account));
        params.put("password", this.dsConfig.getPassword(account));
        params.put("ref", orderNum);
        params.put("desc", "");
        params.put("amount", Math.abs(amount) + "");
        map.put("params", params);
        final String reqJson = JSON.toJSONString((Object)map);
        DsApi.logger.info("[DS]\u7528\u6237\u8f6c\u8d26\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a" + reqJson);
        String respJson = null;
        try {
            respJson = HttpClientUtils.doPost(this.dsConfig.getHost(), reqJson);
            final TypeReference<DsRespBody<String>> typeRef = new TypeReference<DsRespBody<String>>() {};
            final DsRespBody<String> respBody = (DsRespBody<String>)JSON.parseObject(respJson, (TypeReference)typeRef, new Feature[0]);
            final String errorCode = respBody.getErrorCode();
            if ("0".equals(errorCode)) {
                return true;
            }
            if ("6605".equals(errorCode)) {
                this.play(account, null, null, false, null);
                return this.transfer(account, amount, orderNum);
            }
            throw new BusinessException("DS/" + errorCode, respBody.getErrorMessage(), (Object[])null);
        }
        catch (Exception e) {
            DsApi.logger.error("[DS]\u7528\u6237\u8f6c\u8d26\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson, (Throwable)e);
            return this.checkTransfer(orderNum);
        }
    }
    
    private boolean checkTransfer(final String orderNum) throws Exception {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("hashCode", this.dsConfig.getSecurityKey());
        map.put("command", "CHECK_REF");
        final Map<String, String> params = new HashMap<String, String>();
        params.put("ref", orderNum);
        map.put("params", params);
        final String reqJson = JSON.toJSONString((Object)map);
        String respJson = null;
        try {
            DsApi.logger.info("[DS]\u7528\u6237\u67e5\u8be2\u6d41\u6c34\u53f7\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a" + reqJson);
            respJson = HttpClientUtils.doPost(this.dsConfig.getHost(), reqJson);
            final TypeReference<DsRespBody<String>> typeRef = new TypeReference<DsRespBody<String>>() {};
            final DsRespBody<String> respBody = (DsRespBody<String>)JSON.parseObject(respJson, (TypeReference)typeRef, new Feature[0]);
            final String errorCode = respBody.getErrorCode();
            if ("6601".equals(errorCode)) {
                return true;
            }
            throw new BusinessException("DS/" + errorCode, respBody.getErrorMessage(), (Object[])null);
        }
        catch (Exception ex) {
            DsApi.logger.error("[DS]\u7528\u6237\u67e5\u8be2\u6d41\u6c34\u53f7\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson);
            throw ex;
        }
    }
    
    @Override
    public String free() throws Exception {
        throw new Exception("[DS]\u4e0d\u53ef\u4ee5\u8bd5\u73a9\uff01");
    }
    
    @Override
    public String play(final String account, final String gameType, final String ip, final Boolean isMobile, final String baseUrl) throws Exception {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("hashCode", this.dsConfig.getSecurityKey());
        map.put("command", "LOGIN");
        final Map<String, String> params = new HashMap<String, String>();
        params.put("username", this.dsConfig.getAccount(account));
        params.put("password", this.dsConfig.getPassword(account));
        params.put("currency", this.dsConfig.getCurrency());
        params.put("nickname", this.dsConfig.getAccount(account));
        params.put("language", this.dsConfig.getLang());
        map.put("params", params);
        final String reqJson = JSON.toJSONString((Object)map);
        String respJson = null;
        try {
            DsApi.logger.info("[DS]\u7528\u6237\u767b\u5f55\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a" + reqJson);
            respJson = HttpClientUtils.doPost(this.dsConfig.getHost(), reqJson);
            final TypeReference<DsRespBody<Map<String, String>>> typeRef = new TypeReference<DsRespBody<Map<String, String>>>() {};
            final DsRespBody<Map<String, String>> respBody = (DsRespBody<Map<String, String>>)JSON.parseObject(respJson, (TypeReference)typeRef, new Feature[0]);
            final String errorCode = respBody.getErrorCode();
            if ("0".equals(errorCode)) {
                final Map<String, String> respParams = respBody.getParams();
                return respParams.get("link");
            }
            throw new BusinessException("DS/" + errorCode, respBody.getErrorMessage(), (Object[])null);
        }
        catch (Exception ex) {
            DsApi.logger.error("[DS]\u7528\u6237\u767b\u5f55\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson);
            throw ex;
        }
    }
    
    @Override
    public void isOpen() throws Exception {
        if (!this.dsConfig.isOpen()) {
            throw new BusinessException("DS/NOT-SUPPORT", "\u6e38\u620f\u672a\u63a5\u5165", (Object[])null);
        }
    }

    public void getAllDsRecord(){
        String formatUrl = "";
    }
    
    public List<DsBetRecordRep> getRecord(final Date beginDate) throws Exception {

        String formatRecordUrl = baseUrl + "/sbapi/getcsbetrecord_xml" +
                "?type=-1" +
                "&page_num=1" +
                "&page_size=%s" +
                "&startdate=%s" + //2019-04-18 0:35:28
                "&enddate=%s" + //2019-04-18 0:38:28
                "&agentusername=%s" +
                "&agentpwd=%s" +
                "&bet_type=%s"+
                "&username=%s";

        String beginDateStr = sdf.format(beginDate);
        String endDateStr = sdf.format(new Date());

        //替换将url编码
        beginDateStr = beginDateStr.replace(" ","%20");
        endDateStr = endDateStr.replace(" ","%20");
        String recordUrl = String.format(formatRecordUrl , Integer.MAX_VALUE ,  beginDateStr , endDateStr , agentusername , agentpwd , "-1" , "" );

        List<DsBetRecordRep> result = new ArrayList<>();

        final String reqJson = recordUrl;
        String respJson = null;
        try {
            //注单查询，请求参数：
            DsApi.logger.info("[DS]\u6ce8\u5355\u67e5\u8be2\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a" + reqJson);
            String respXml = HttpClientUtils.doGet(recordUrl , null);

            SbRecordReportDto report = null;
            try{
                org.json.JSONObject jsonObject  = XML.toJSONObject(respXml);
                com.alibaba.fastjson.JSONObject.DEFFAULT_DATE_FORMAT="yyyy/MM/dd HH:mm:ss";//设置日期格式
                report = com.alibaba.fastjson.JSONObject.parseObject(jsonObject.toString() , SbRecordReportDto.class);
            }catch (JSONException e){
                //解析失败
                DsApi.logger.info("[DS1]\u89e3\u6790\u5931\u8d25 " + respXml);
            }


            if(report != null){
                if(report.getReport().getErrcode() == 0){

                    List<SbRecordReportItem> reportItems = report.getReport().getList().getItem();
                    if(CollectionUtils.isNotEmpty(reportItems)){
                        for(SbRecordReportItem item : reportItems){
                            DsBetRecordRep record = new DsBetRecordRep();
                            //账户
                            record.setUserName(item.getUser_name());
                            //
                            record.setSequenceNo(Long.valueOf(item.getBet_no()));
                            record.setGameType("BACCARAT");
                            record.setBankerResult("notnull");
                            record.setStakeAmount(item.getMoney());
                            record.setValidStake(item.getMoney());
                            record.setWinLoss(item.getWinlose());
                            record.setComm(0.0d);

//                            if(item.getStatus().intValue() == 2 ){
//                                record.setSettle(1);
//                            }else{
//                                record.setSettle(0);
//                            }
                            record.setEndTime(item.getBettime().getTime());
//                            record.setBetTime(item.getBettime());
                            record.setStatTime(null);
                            //美东时间
//                            record.setAmesTime(null);
//                            record.setRebateTime(null);
                            result.add(record);
                        }

                    }
                    return result;
                }

            }

            SbRecordReportDto2 report2 = null;
            if(report == null){
                try{
                    org.json.JSONObject jsonObject  = XML.toJSONObject(respXml);
                    com.alibaba.fastjson.JSONObject.DEFFAULT_DATE_FORMAT="yyyy/MM/dd HH:mm:ss";//设置日期格式
                    report2 = com.alibaba.fastjson.JSONObject.parseObject(jsonObject.toString() , SbRecordReportDto2.class);
                }catch (JSONException e){
                    //解析失败
                    DsApi.logger.info("[DS2]\u89e3\u6790\u5931\u8d25 " + respXml);
                }
            }

            if(report2 != null){
                if(report2.getReport().getErrcode() == 0){
                    SbRecordReportItem item = report2.getReport().getList().getItem();
                    if(item != null){

                        DsBetRecordRep record = new DsBetRecordRep();
                        //账户
                        record.setUserName(item.getUser_name());
                        //
                        record.setSequenceNo(Long.valueOf(item.getBet_no()));
                        record.setGameType("BACCARAT");
                        record.setBankerResult(null);
                        record.setStakeAmount(null);
                        record.setValidStake(null);
                        record.setWinLoss(item.getWinlose());
                        record.setComm(0.0d);
                        if(item.getStatus().intValue() == 2 ){
                            record.setSettle(1);
                        }else{
                            record.setSettle(0);
                        }
                        record.setEndTime(item.getBettime().getTime());
                        record.setBetTime(item.getBettime());
                        record.setStatTime(null);
                        //美东时间
                        record.setAmesTime(null);
                        record.setRebateTime(null);
                        result.add(record);

                    }
                    return result;
                }
            }

            SbRecordReportDto3 report3 = null;
            if(report2 == null){
                try{
                    org.json.JSONObject jsonObject  = XML.toJSONObject(respXml);
//                    com.alibaba.fastjson.JSONObject.DEFFAULT_DATE_FORMAT="yyyy/MM/dd HH:mm:ss";//设置日期格式
                    report3 = com.alibaba.fastjson.JSONObject.parseObject(jsonObject.toString() , SbRecordReportDto3.class);
                    return null;
                }catch (JSONException e){
                    //解析失败
                    DsApi.logger.info("[DS3]\u89e3\u6790\u5931\u8d25 " + respXml);
                }
            }
        }
        catch (Exception ex) {
            DsApi.logger.error("[DS]\u6ce8\u5355\u67e5\u8be2\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson);
            throw ex;
        }
        return result;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)DsApi.class);
    }
}
