package com.cz.gameplat.live.core.service;

import com.alibaba.fastjson.JSONException;
import com.cz.gameplat.live.core.api.ds.bean.DsBetRecordRep;
import com.cz.gameplat.live.core.dto.ds.SbRecordReportDto;
import com.cz.gameplat.live.core.dto.ds.SbRecordReportDto2;
import com.cz.gameplat.live.core.dto.ds.SbRecordReportDto3;
import com.cz.gameplat.live.core.dto.ds.SbRecordReportItem;
import com.cz.gameplat.live.core.utils.HttpClientUtils;
import org.apache.commons.collections.CollectionUtils;
import org.json.XML;
import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.api.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.gameplat.live.core.dao.*;
import org.apache.commons.lang.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.api.ag.bean.*;
import com.cz.framework.exception.*;
import com.cz.rest.config.*;
import com.cz.framework.http.*;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.parser.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.bean.*;
import org.slf4j.*;
import  com.alibaba.fastjson.JSONException;

@Service
public class AgBetRecordService extends LiveCheckService
{
    private static final Logger logger;
    @Resource
    AgApi agApi;
    @Resource
    AgConfig agConfig;
    @Resource
    private AgBetRecordDao agbetRecordDao;
    @Resource
    private LiveDmlService liveDmlService;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String baseUrl = "http://115.231.174.31:22280";

    private String agentusername = "hj789";

    private String agentpwd = "aabb1122";

    private static Date defaultRecordTime = null;
    static{
        try {
            defaultRecordTime = sdf.parse("2019-04-20 10:35:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Date getMaxAddTime() {
        final String sdate = this.agbetRecordDao.getMaxAddTime();
        if (StringUtils.isNotEmpty(sdate)) {
            return DateUtil.strToDate(sdate, "yyyy-MM-dd HH:mm:ss");
        }
        return null;
    }

    public List<AgBetRecord> getApiBetRecord(final Date startTime, final Date endTime, final String gameKind, final String account, final Integer status, final int page, final int rows) throws Exception {
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


        String beginDateStr = sdf.format(startTime);
        String endDateStr = sdf.format(new Date());

        //替换将url编码
        beginDateStr = beginDateStr.replace(" ","%20");
        endDateStr = endDateStr.replace(" ","%20");
        String recordUrl = String.format(formatRecordUrl , Integer.MAX_VALUE ,  beginDateStr , endDateStr , agentusername , agentpwd , "-1" , "" );

        List<AgBetRecord> result = new ArrayList<>();

        final String reqJson = recordUrl;
        String respJson = null;
        try {
            //注单查询，请求参数：
            logger.info("[AgBetRecordService]\u6ce8\u5355\u67e5\u8be2\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a" + reqJson);
            String respXml = HttpClientUtils.doGet(recordUrl , null);

            SbRecordReportDto report = null;
            try{
                org.json.JSONObject jsonObject  = XML.toJSONObject(respXml);
                com.alibaba.fastjson.JSONObject.DEFFAULT_DATE_FORMAT="yyyy/MM/dd HH:mm:ss";//设置日期格式
                report = com.alibaba.fastjson.JSONObject.parseObject(jsonObject.toString() , SbRecordReportDto.class);
            }catch (JSONException e){
                //解析失败
                logger.info("[AgBetRecordService1]\u89e3\u6790\u5931\u8d25 " + respXml);
            }


            if(report != null){
                if(report.getReport().getErrcode() == 0){

                    List<SbRecordReportItem> reportItems = report.getReport().getList().getItem();
                    if(CollectionUtils.isNotEmpty(reportItems)){
                        for(SbRecordReportItem item : reportItems){
                            AgBetRecord agBetRecord = new AgBetRecord();
                            agBetRecord.setBillNo(item.getBet_no());
                            agBetRecord.setDataType(null);
                            agBetRecord.setAccount(item.getUser_name());
                            agBetRecord.setWinAmount(item.getWinlose());
                            agBetRecord.setBetTime(item.getBettime());
                            agBetRecord.setGameType("BACCARAT");
                            agBetRecord.setBetAmount(item.getMoney());
                            agBetRecord.setValidAmount(item.getMoney());
                            agBetRecord.setFlag(1);
                            agBetRecord.setGameKind("AGIN");
                            agBetRecord.setAgentName(agentusername);
                            agBetRecord.setAddTime(new Date());

//                            agBetRecord.setRebateTime();
//                            agBetRecord.setGmtBetTime();
//                            agBetRecord.setAmesTime(new Date());
//                            agBetRecord.setStatTime();

                            agBetRecord.setBetTime(item.getBettime());
                            final Date amesTime = DateUtil.getUSToAMES(item.getBettime());
                            agBetRecord.setAmesTime(sdf.parse(DateUtil.dateToYMDHMS(amesTime)));
                            agBetRecord.setRebateTime(DateUtil.dateToYMD(amesTime));
                            agBetRecord.setStatTime(this.getStatTime(item.getBettime()));
                            agBetRecord.setGmtBetTime(new Date());
                            agBetRecord.setBetContent(item.getBet_items_name());
                            if(item.getStatus().intValue() == 2 ){
                                agBetRecord.setSettle(1);
                            }else{
                                agBetRecord.setSettle(0);
                            }
                            agBetRecord.setUpdateTime(new Date());

//                            DsBetRecordRep record = new DsBetRecordRep();
//                            //账户
//                            record.setUserName(item.getUser_name());
//                            //
//                            record.setSequenceNo(Long.valueOf(item.getBet_no()));
//                            record.setGameType("BACCARAT");
//                            record.setBankerResult("notnull");
//                            record.setStakeAmount(item.getMoney());
//                            record.setValidStake(item.getMoney());
//                            record.setWinLoss(item.getWinlose());
//                            record.setComm(0.0d);
//

//                            record.setEndTime(item.getBettime().getTime());
////                            record.setBetTime(item.getBettime());
//                            record.setStatTime(null);
                            //美东时间
//                            record.setAmesTime(null);
//                            record.setRebateTime(null);
                            result.add(agBetRecord);
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
                    logger.info("[AgBetRecordService2]\u89e3\u6790\u5931\u8d25 " + respXml);
                }
            }

            if(report2 != null){
                if(report2.getReport().getErrcode() == 0){
                    SbRecordReportItem item = report2.getReport().getList().getItem();
                    if(item != null){

                        AgBetRecord agBetRecord = new AgBetRecord();
                        agBetRecord.setBillNo(item.getBet_no());
                        agBetRecord.setDataType(null);
                        agBetRecord.setAccount(item.getUser_name());
                        agBetRecord.setWinAmount(item.getWinlose());
                        agBetRecord.setBetTime(item.getBettime());
                        agBetRecord.setGameType("BACCARAT");
                        agBetRecord.setBetAmount(item.getMoney());
                        agBetRecord.setValidAmount(item.getMoney());
                        agBetRecord.setFlag(1);
                        agBetRecord.setGameKind("AGIN");
                        agBetRecord.setAgentName(agentusername);
                        agBetRecord.setAddTime(new Date());

                        agBetRecord.setBetTime(item.getBettime());
                        final Date amesTime = DateUtil.getUSToAMES(item.getBettime());
                        agBetRecord.setAmesTime(sdf.parse(DateUtil.dateToYMDHMS(amesTime)));
                        agBetRecord.setRebateTime(DateUtil.dateToYMD(amesTime));
                        agBetRecord.setStatTime(this.getStatTime(item.getBettime()));
                        agBetRecord.setGmtBetTime(new Date());
                        agBetRecord.setBetContent(item.getBet_items_name());
                        if(item.getStatus().intValue() == 2 ){
                            agBetRecord.setSettle(1);
                        }else{
                            agBetRecord.setSettle(0);
                        }
                        agBetRecord.setUpdateTime(new Date());
                        result.add(agBetRecord);

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
                    logger.info("[AgBetRecordService3]\u89e3\u6790\u5931\u8d25 " + respXml);
                }
            }
        }
        catch (Exception ex) {
            logger.error("[DS]\u6ce8\u5355\u67e5\u8be2\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson);
            throw ex;
        }
        return result;
//        if (bean.getStatus() != 200) {
//            AgBetRecordService.logger.error("\u83b7\u53d6AG\u6570\u636e\u5f02\u5e38:url=" + client.getReqUrl() + " ," + bean);
//            throw new BusinessException("HTTP/error", bean.getStatus() + "", (Object[])null);
//        }
//        AgBetRecordService.logger.info("\u8bf7\u6c42AG\u6570\u636e\uff0cURL=" + client.getReqUrl());
//        final AgDataRep data = (AgDataRep)WafJsonMapper.parse(bean.getRespBody(), (Class)AgDataRep.class);
//        return data;
    }

    public List<AgBetRecord> batchSave(final List<AgBetRecord> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        this.agbetRecordDao.deleteTemp();
        this.agbetRecordDao.batchSaveTemp(list);
        this.liveDmlService.liveDml(LiveGame.AG);
        this.agbetRecordDao.batchSaveByTemp();
        return this.agbetRecordDao.getTempStatTime();
    }

    @Override
    protected String getCheckApi() {
        return this.agConfig.getCheckApi();
    }

    @Override
    protected String getAgentName() {
        return this.agConfig.getAgentName();
    }

    @Override
    protected CheckInfo queryCheckInfo(final Date startTime, final Date endTime) {
        return this.agbetRecordDao.queryCheckInfo(startTime, endTime);
    }

    static {
        logger = LoggerFactory.getLogger((Class)AgBetRecordService.class);
    }

    private String getStatTime(final Date betTime) {
        final String statTime = DateUtil.dateToYMD(betTime);
        final Date startTime = DateUtil.strToDate(statTime + " 07:59:59");
        if (DateUtil.dateCompareByYmdhms(betTime, startTime)) {
            return DateUtil.dateToYMD(DateUtil.addDate(betTime, -1));
        }
        return statTime;
    }
}
