package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.framework.task.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.*;
import com.cz.framework.exception.*;
import java.util.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.bbin.bean.*;
import org.slf4j.*;

@Component("bbinTaskJob")
public class BBinTaskJob
{
    private static final Logger logger;
    private static final RequestApiQueue1 queue1;
    @Resource
    private BbinConfig bbinConfig;
    @Resource
    private BBinBetRecordService bbinBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveManager liveManager;
    private Set<String> statTimeSet;
    private Set<String> rebateTimeSet;

    public BBinTaskJob() {
        this.statTimeSet = new HashSet<String>();
        this.rebateTimeSet = new HashSet<String>();
    }

    private void checkData() {
        try {
            final List<Date> needRefetchTimeList = (List<Date>)this.bbinBetRecordService.getNeedRefetchRecordTimeList(new Date(System.currentTimeMillis() - 43200000L));
            for (final Date startTime : needRefetchTimeList) {
                final Date endTime = new Date(startTime.getTime() + 3600000L);
                BBinTaskJob.logger.info("\u91cd\u65b0\u83b7\u53d6BBIN\u6295\u6ce8\u8bb0\u5f55:{} - {}", (Object)DateUtil.dateToYMDHMS(startTime), (Object)DateUtil.dateToYMDHMS(endTime));
                this.fetchData(startTime, endTime, 1);
            }
        }
        catch (BusinessException e) {
            e.printStackTrace();
        }
    }

    private void fetchData(final Date startTime, final Date endTime, final int status) {
        final long st = System.currentTimeMillis();
        final Set<String> statTimeSet = new HashSet<String>();
        final Set<String> rebateTimeSet = new HashSet<String>();
        try {
            int totalCount = 0;
            int page = 1;
            int totalPage = 1;
            final int rows = this.bbinConfig.getPagelimit();
            int firstTotalCount = 0;
            do {
                final BBinDataRep data = this.bbinBetRecordService.getApiBetRecord(startTime, endTime, (String)null, (String)null, status, page, rows);
                if (data == null || data.getData() == null) {
                    break;
                }
                if (data.getData().isEmpty()) {
                    break;
                }
                totalCount = data.getTotalCount();
                if (page == 1) {
                    firstTotalCount = totalCount;
                }
                if (firstTotalCount != totalCount) {
                    BBinTaskJob.logger.info("\u5f53\u6b21\u83b7\u53d6\u6570\u636e\u603b\u6761\u6570\u4e0d\u76f8\u7b49\uff0c\u7b2c1\u9875\u603b\u6570\u636e\uff1a" + firstTotalCount + ",\u5f53\u524d\u9875\u603b\u6570\u636e\uff1a" + totalCount + ",\u5f53\u524d\u9875\uff1a" + page + ",startTime=" + startTime + ",endTime=" + endTime);
                    break;
                }
                totalPage = ((totalCount % rows > 0) ? (totalCount / rows + 1) : (totalCount / rows));
                ++page;
                final List<BBinBetRecord> dlist = (List<BBinBetRecord>)this.bbinBetRecordService.newBatchSave(data.getData());
                if (dlist == null || dlist.isEmpty()) {
                    continue;
                }
                for (final BBinBetRecord ag : dlist) {
                    statTimeSet.add(ag.getStatTime());
                    rebateTimeSet.add(ag.getRebateTime());
                }
                dlist.clear();
            } while (page <= totalPage);
        }
        catch (Exception ex) {
            BBinTaskJob.logger.error("\u4e0b\u8f7dbbin\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38, key:", (Throwable)ex);
        }
        finally {
            for (final String statTime : rebateTimeSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.BBIN);
            }
            for (final String statTime : statTimeSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.BBIN);
            }
            statTimeSet.clear();
            rebateTimeSet.clear();
        }
        BBinTaskJob.logger.info("End bbin,\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }

    public void getBetFromOnline() {
        try {
            if (!this.bbinConfig.isOpen()) {
                BBinTaskJob.logger.info("bbin \u672a\u5f00\u542f...");
                return;
            }
            this.liveManager.maintenance(LiveGame.BBIN.getCode());
            final long st = System.currentTimeMillis();
            BBinTaskJob.logger.info("start bbin........");
            Date startTime = this.bbinBetRecordService.getMaxAddTime();
            if (startTime == null) {
                startTime = DateUtil.getDateStart(new Date());
            }
            final Date endTime = new Date();
            this.checkData();
            this.fetchData(startTime, endTime, 0);
            BBinTaskJob.logger.info("End bbin,\u8017\u65f6:" + (System.currentTimeMillis() - st));
        }
        catch (Exception ex) {
            BBinTaskJob.logger.error("bbin job \u5f02\u5e38\uff1a", (Throwable)ex);
        }
    }

    public void getNotSettleFromOnline() {
        try {
            if (!this.bbinConfig.isOpen()) {
                BBinTaskJob.logger.info("bbin 未开启...");
                return;
            }
            this.liveManager.maintenance(LiveGame.BBIN.getCode());
            final String keys = "getNotSettle";
            Iterator localIterator1;
            Iterator localIterator3;
            String statTime = null;
            BBinTaskJob.queue1.add("getNotSettle", new Runnable() {
                @Override
                public void run() {
                    try
                    {
                        BBinTaskJob.logger.info("Start bbin getNotSettle.......");
                        List<BBinBetRecord> list = BBinTaskJob.this.bbinBetRecordService.getNotSettle();
                        String statTime;
                        if ((list == null) || (list.isEmpty()))
                        {
//
                            return;
                        }
                        Object upList = new ArrayList();
                        for (BBinBetRecord b : list)
                        {
                            BBinParam param = new BBinParam();
                            BBGameKind gk = BBGameKind.get(b.getGameKind());
                            if (gk != null)
                            {
                                param.setGameKind(gk.getValue() + "");
                                if (BBGameKind.ELEC.getKind().equals(b.getGameKind())) {
                                    param.setGameSubkind(b.getSubGameKind());
                                } else if (BBGameKind.LOTTERY.getKind().equals(b.getGameKind())) {
                                    param.setGameType(b.getGameType());
                                }
                                param.setStartTime(DateUtil.dateToStr(b.getBetTime(), "HH:mm:ss"));
                                param.setRoundDate(DateUtil.dateToStr(b.getBetTime(), "yyyy-MM-dd"));
                                param.setEndTime(DateUtil.dateToStr(b.getBetTime(), "HH:mm:ss"));
                                param.setPageNo(1);
                                try
                                {
                                    BBinRespBody<ArrayList<BBinBetRecordRep>> respBody = BBinTaskJob.this.bbinBetRecordService.getApiBetRecord(param);
                                    if (respBody.getData() != null)
                                    {
                                        ArrayList<BBinBetRecordRep> dataList = (ArrayList)respBody.getData();
                                        for (BBinBetRecordRep rep : dataList) {
                                            if ((SettleTypes.YES.getValue().intValue() == rep.getSettle().intValue()) &&
                                                    (b.getBillNo().equals(rep.getWagersId()))) {
                                                ((List)upList).add(rep);
                                            }
                                        }
                                    }
                                }
                                catch (Exception e)
                                {
                                    BBinTaskJob.logger.error("", e);
                                }
                            }
                        }
//                        String statTime;
                        if (((List)upList).isEmpty())
                        {
//                            String statTime;
                            return;
                        }
                        List<BBinBetRecord> statList = BBinTaskJob.this.bbinBetRecordService.batchNotSettle1((List)upList);
                        if ((statList != null) && (!statList.isEmpty())) {
                            for (BBinBetRecord bbin : statList)
                            {
                                BBinTaskJob.this.rebateTimeSet.add(bbin.getRebateTime());
                                BBinTaskJob.this.statTimeSet.add(bbin.getStatTime());
                            }
                        }
//                        String statTime;
//                        String statTime;
//                        String statTime;
//                        String statTime;
                        Iterator localIterator3;
//                        String statTime;
//                        String statTime;
                        return;
                    }
                    catch (Exception ex)
                    {
                        BBinTaskJob.logger.error("", ex);
                    }
                    finally
                    {
                        BBinTaskJob.queue1.remove("getNotSettle");
                        if (BBinTaskJob.queue1.isTaskRuning())
                        {
//                            for (localIterator3 = BBinTaskJob.this.rebateTimeSet.iterator(); localIterator3.hasNext();)
//                            {
//                                statTime = (String)localIterator3.next();
//                                BBinTaskJob.this.liveUserDayReportService.saveDayReport(statTime, LiveGame.BBIN);
//                            }
//                            for (localIterator3 = BBinTaskJob.this.statTimeSet.iterator(); localIterator3.hasNext();)
//                            {
//                                statTime = (String)localIterator3.next();
//                                BBinTaskJob.this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.BBIN);
//                            }
                            BBinTaskJob.this.statTimeSet.clear();
                            BBinTaskJob.this.rebateTimeSet.clear();
                            BBinTaskJob.logger.info("End bbin getNotSettle.......");
                        }
                        try
                        {
                            Thread.sleep(2000L);
                        }
                        catch (Exception ex)
                        {
                            BBinTaskJob.logger.error("sleep:", ex);
                        }
                    }
                }
            });
        }
        catch (Exception ex) {
            BBinTaskJob.logger.error(ex.getMessage());
        }
    }

    static {
        logger = LoggerFactory.getLogger((Class)BBinTaskJob.class);
        queue1 = new RequestApiQueue1();
    }
}
