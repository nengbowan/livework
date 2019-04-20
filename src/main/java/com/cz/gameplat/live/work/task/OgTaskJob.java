package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.exception.*;
import java.util.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.og.bean.*;
import org.slf4j.*;

@Component
public class OgTaskJob
{
    private static final Logger logger;
    @Resource
    private OgConfig ogConfig;
    @Resource
    private OgBetRecordService ogBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    private Set<String> statTimeSet;
    private Set<String> rebateTimeSet;
    
    public OgTaskJob() {
        this.statTimeSet = new HashSet<String>();
        this.rebateTimeSet = new HashSet<String>();
    }
    
    public void getBet() {
        OgTaskJob.logger.info("Start og bet info.");
        final long st = System.currentTimeMillis();
        if (!this.ogConfig.isOpen()) {
            OgTaskJob.logger.info("og \u672a\u5f00\u542f...");
            return;
        }
        if (this.ogConfig.isCollect()) {
            this.getBetByCollect();
        }
        else {
            this.getBetByT();
        }
        OgTaskJob.logger.info("End og bet info.\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    private void checkData() {
        try {
            final List<Date> needRefetchTimeList = (List<Date>)this.ogBetRecordService.getNeedRefetchRecordTimeList(new Date());
            for (final Date startTime : needRefetchTimeList) {
                final Date endTime = new Date(startTime.getTime() + 3600000L);
                OgTaskJob.logger.info("\u91cd\u65b0\u83b7\u53d6OG\u6295\u6ce8\u8bb0\u5f55:{} - {}", (Object)DateUtil.dateToYMDHMS(startTime), (Object)DateUtil.dateToYMDHMS(endTime));
                this.fetchData(startTime, endTime, 1);
            }
        }
        catch (BusinessException e) {
            e.printStackTrace();
        }
    }
    
    private void fetchData(final Date startTime, final Date endTime, final int status) {
        try {
            int totalCount = 0;
            int totalPage = 1;
            int page = 1;
            final int rows = this.ogConfig.getRows();
            int firstTotalCount = 0;
            do {
                final OgDataRep data = this.ogBetRecordService.getApiBetRecord(startTime, endTime, (String)null, (String)null, status, page, rows);
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
                    OgTaskJob.logger.info("\u5f53\u6b21\u83b7\u53d6\u6570\u636e\u603b\u6761\u6570\u4e0d\u76f8\u7b49\uff0c\u7b2c1\u9875\u603b\u6570\u636e\uff1a" + firstTotalCount + ",\u5f53\u524d\u9875\u603b\u6570\u636e\uff1a" + totalCount + ",\u5f53\u524d\u9875\uff1a" + page + ",startTime=" + startTime + ",endTime=" + endTime);
                    break;
                }
                LogUtil.info("\u65f6\u95f4[" + startTime + "--" + endTime + "],\u91c7\u96c6\u6570\u636e[" + totalCount + "]\u6761");
                totalPage = ((totalCount % rows > 0) ? (totalCount / rows + 1) : (totalCount / rows));
                ++page;
                List<OgBetRecord> ogList = (List<OgBetRecord>)this.ogBetRecordService.batchSave(data.getData());
                if (ogList == null || ogList.isEmpty()) {
                    continue;
                }
                for (final OgBetRecord og : ogList) {
                    this.statTimeSet.add(og.getStatTime());
                    this.rebateTimeSet.add(og.getRebateTime());
                }
                ogList.clear();
                ogList = null;
            } while (page <= totalPage);
            for (final String statTime : this.rebateTimeSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.OG);
            }
            for (final String statTime : this.statTimeSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.OG);
            }
        }
        catch (Exception ex) {
            OgTaskJob.logger.error("\u4e0b\u8f7dog\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)ex);
        }
        finally {
            this.statTimeSet.clear();
            this.rebateTimeSet.clear();
        }
    }
    
    private void getBetByT() {
        try {
            final List<OgBetRecordRep> list = (List<OgBetRecordRep>)this.ogBetRecordService.getApiBet();
            final List<OgBetRecord> statList = (List<OgBetRecord>)this.ogBetRecordService.batchSaveT((List)list);
            if (statList != null && !statList.isEmpty()) {
                for (final OgBetRecord og : statList) {
                    this.liveUserDayReportService.saveDayReport(og.getRebateTime(), LiveGame.OG);
                    this.liveUserDayReportService.saveRebateReport(og.getStatTime(), LiveGame.OG);
                }
            }
        }
        catch (Exception ex) {
            OgTaskJob.logger.error("OG \u5f02\u5e38\uff1a", (Throwable)ex);
        }
    }
    
    private void getBetByCollect() {
        Date startTime = this.ogBetRecordService.getUpdateTime();
        if (startTime == null) {
            startTime = DateUtil.getDateStart(new Date());
        }
        final Date endTime = new Date();
        this.checkData();
        this.fetchData(startTime, endTime, 0);
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)OgTaskJob.class);
    }
}
