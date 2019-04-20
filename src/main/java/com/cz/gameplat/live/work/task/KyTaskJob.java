package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import org.slf4j.*;

@Component
public class KyTaskJob
{
    private static final Logger logger;
    @Resource
    private KyConfig kyConfig;
    @Resource
    private KyBetRecordService kyBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    private Set<String> statTimeSet;
    private Set<String> rebateTimeSet;
    
    public KyTaskJob() {
        this.statTimeSet = new HashSet<String>();
        this.rebateTimeSet = new HashSet<String>();
    }
    
    public void getBet() {
        KyTaskJob.logger.info("Start Ky bet info.");
        final long st = System.currentTimeMillis();
        if (!this.kyConfig.isOpen()) {
            KyTaskJob.logger.info("ky \u672a\u5f00\u542f...");
            return;
        }
        this.getBetByCollect();
        KyTaskJob.logger.info("End ky bet info.\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    private void getBetByCollect() {
        try {
            Date startDate = this.getStartDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, (int)this.kyConfig.getTaskPeriod());
            int mins = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            while (mins > 2) {
                endDate = DateUtil.getSecond(endDate, -1);
                final long startTime = System.currentTimeMillis();
                final String keys = "ky_" + DateUtil.dateToYMDHMS(startDate) + "_" + DateUtil.dateToYMDHMS(endDate);
                KyTaskJob.logger.info("Start ky task. keys = " + keys + ",startTime=" + startTime);
                final List<KyBetRecord> list = (List<KyBetRecord>)this.kyBetRecordService.getApiBetRecord(startDate, endDate);
                final List<KyBetRecord> statList = (List<KyBetRecord>)this.kyBetRecordService.batchSave((List)list, endDate);
                if (statList != null) {
                    for (final KyBetRecord ky : statList) {
                        statSet.add(ky.getStatTime());
                        rebateSet.add(ky.getRebateTime());
                    }
                }
                KyTaskJob.logger.info("end ky task. key=" + keys + "\u8017\u65f6:" + (System.currentTimeMillis() - startTime));
                startDate = DateUtil.getSecond(endDate, 1);
                endDate = DateUtil.getMinute(startDate, (int)this.kyConfig.getTaskPeriod());
                mins = DateUtil.betweenMinute(endDate, newDate);
                Thread.sleep(11000L);
            }
            for (final String statTime : rebateSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.KY);
            }
            for (final String statTime : statSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.KY);
            }
        }
        catch (Exception ex) {
            KyTaskJob.logger.error("\u4e0b\u8f7dky\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)ex);
        }
        finally {
            this.statTimeSet.clear();
            this.rebateTimeSet.clear();
        }
    }
    
    private Date getStartDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.KY.getCode());
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
        logger = LoggerFactory.getLogger((Class)KyTaskJob.class);
    }
}
