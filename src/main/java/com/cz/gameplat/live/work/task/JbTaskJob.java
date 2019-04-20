package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.utils.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.jb.bean.*;
import java.util.*;
import org.slf4j.*;

@Component
public class JbTaskJob
{
    private static final Logger logger;
    @Resource
    private JbBetRecordService jbBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    @Resource
    private JbConfig jbConfig;
    
    public void getBet() {
        JbTaskJob.logger.info("Start job JB.......");
        try {
            if (!this.jbConfig.isOpen()) {
                JbTaskJob.logger.info("JB \u672a\u5f00\u542f...");
                return;
            }
            final Date startDate = this.getStratDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, 10);
            final int mins = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            if (mins > 2) {
                endDate = DateUtil.getSecond(endDate, -1);
                final Long startTaskTime = System.currentTimeMillis();
                final String startTime = DateUtils.format(startDate, "yyyyMMddHHmmss");
                final String endTime = DateUtils.format(endDate, "yyyyMMddHHmmss");
                JbTaskJob.logger.info("Start JB task. startTime=" + startTime);
                final int offset = 0;
                int count = 0;
                this.jbBetRecordService.deleteTemp();
                while (true) {
                    final List<JbBetRecordRep> list = (List<JbBetRecordRep>)this.jbBetRecordService.getApiBet(startTime, endTime, String.valueOf(count * 500));
                    if (list == null) {
                        break;
                    }
                    this.jbBetRecordService.batchSaveTemp((List)list);
                    if (list.size() < 500) {}
                    ++count;
                    Thread.sleep(1000L);
                }
                final List<JbBetRecord> statList = (List<JbBetRecord>)this.jbBetRecordService.batchSave(endDate);
                if (statList != null) {
                    for (final JbBetRecord jb : statList) {
                        statSet.add(jb.getStatTime());
                        rebateSet.add(jb.getRebateTime());
                    }
                }
                JbTaskJob.logger.info("end jb task. \u8017\u65f6:" + (System.currentTimeMillis() - startTaskTime));
            }
            for (final String statTime : rebateSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.JB);
            }
            for (final String statTime : statSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.JB);
            }
        }
        catch (Exception ex) {
            JbTaskJob.logger.error("JB \u5f02\u5e38\uff1a", (Throwable)ex);
        }
        JbTaskJob.logger.info("End job JB.......");
    }
    
    private Date getStratDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.JB.getCode());
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
        logger = LoggerFactory.getLogger((Class)JbTaskJob.class);
    }
}
