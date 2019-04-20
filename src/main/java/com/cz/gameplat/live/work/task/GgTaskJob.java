package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.gg.*;
import java.util.*;
import org.slf4j.*;

@Component
public class GgTaskJob
{
    private static final Logger logger;
    @Resource
    private GgConfig ggConfig;
    @Resource
    private GgBetRecordService ggBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    private static final Integer MIN;
    
    public void getBet() {
        GgTaskJob.logger.info("Start gg bet info.");
        final long st = System.currentTimeMillis();
        if (!this.ggConfig.isOpen()) {
            GgTaskJob.logger.info("gg \u672a\u5f00\u542f...");
            return;
        }
        this.getBetByCollect();
        GgTaskJob.logger.info("End gg bet info.\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    private void getBetByCollect() {
        try {
            final Date startDate = this.getStartDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, (int)this.ggConfig.getTaskPeriod());
            final Integer min = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            if (min > GgTaskJob.MIN) {
                final long startTime = System.currentTimeMillis();
                endDate = ((endDate.compareTo(DateUtil.getMinute(newDate, -10)) > 0) ? DateUtil.getMinute(newDate, -10) : endDate);
                final String keys = "GG_" + DateUtil.dateToYMDHMS(startDate) + "_" + DateUtil.dateToYMDHMS(endDate);
                GgTaskJob.logger.info("Start GG task. keys = " + keys + ",startTime=" + startTime);
                final List<GgBetRecordRep> list = (List<GgBetRecordRep>)this.ggBetRecordService.getApiBetRecord(startDate, endDate, DateUtil.daysBetween(endDate, newDate) < 3);
                final List<GgBetRecord> statList = (List<GgBetRecord>)this.ggBetRecordService.batchSave((List)list, DateUtil.getSecond(endDate, -1), true);
                if (statList != null) {
                    for (final GgBetRecord gg : statList) {
                        statSet.add(gg.getStatTime());
                        rebateSet.add(gg.getRebateTime());
                    }
                }
                GgTaskJob.logger.info("end GG task. key=" + keys + "\u8017\u65f6:" + (System.currentTimeMillis() - startTime));
            }
            for (final String statTime : rebateSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.GG);
            }
            for (final String statTime : statSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.GG);
            }
        }
        catch (Exception ex) {
            GgTaskJob.logger.error("\u4e0b\u8f7dGG\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)ex);
        }
    }
    
    private Date getStartDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.GG.getCode());
        if (startDate == null) {
            final String date = DateUtil.getNowTime();
            final Date newDate = DateUtil.strToDate(date);
            return DateUtil.getDateStart(newDate);
        }
        startDate = DateUtil.getMinute(startDate, -1);
        startDate = DateUtil.getSecond(startDate, 1);
        return startDate;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)GgTaskJob.class);
        MIN = 2;
    }
}
