package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.utils.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.cq.bean.*;
import java.util.*;
import org.slf4j.*;

@Component
public class Cq9TaskJob
{
    private static final Logger logger;
    @Resource
    private Cq9BetRecordService cq9BetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    @Resource
    private Cq9Config cq9Config;
    
    public void getBet() {
        Cq9TaskJob.logger.info("Start job CQ9.......");
        try {
            if (!this.cq9Config.isOpen()) {
                Cq9TaskJob.logger.info("CQ9 \u672a\u5f00\u542f...");
                return;
            }
            Date startDate = this.getStratDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, 10);
            int mins = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            while (mins > 2) {
                endDate = DateUtil.getSecond(endDate, -1);
                final Long startTaskTime = System.currentTimeMillis();
                this.cq9BetRecordService.deleteTemp();
                final String startTime = DateUtils.format(startDate, "yyyy-MM-dd'T'HH:mm:ssXXX", "GMT-4");
                final String endTime = DateUtils.format(endDate, "yyyy-MM-dd'T'HH:mm:ssXXX", "GMT-4");
                Cq9TaskJob.logger.info("Start CQ9 task. startTime=" + startTime);
                int count = 1;
                int totalPage = 1;
                do {
                    final CqRespBody list = this.cq9BetRecordService.getApiBet(startTime, endTime, String.valueOf(count));
                    if (list != null && list.getTotalSize() > 0) {
                        totalPage = list.getTotalSize();
                        this.cq9BetRecordService.batchSaveTemp(list.getCqBetRecordRepList());
                    }
                    ++count;
                    Thread.sleep(1000L);
                } while (count <= totalPage);
                final List<Cq9BetRecord> statList = (List<Cq9BetRecord>)this.cq9BetRecordService.batchSave(endDate);
                if (statList != null) {
                    for (final Cq9BetRecord cq : statList) {
                        statSet.add(cq.getStatTime());
                        rebateSet.add(cq.getRebateTime());
                    }
                }
                Cq9TaskJob.logger.info("end cq task. \u8017\u65f6:" + (System.currentTimeMillis() - startTaskTime));
                startDate = DateUtil.getSecond(endDate, 1);
                endDate = DateUtil.getMinute(startDate, 10);
                mins = DateUtil.betweenMinute(endDate, newDate);
                Thread.sleep(3000L);
            }
            for (final String statTime : rebateSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.CQ9);
            }
            for (final String statTime : statSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.CQ9);
            }
        }
        catch (Exception ex) {
            Cq9TaskJob.logger.error("CQ9 \u5f02\u5e38\uff1a", (Throwable)ex);
        }
        Cq9TaskJob.logger.info("End job CQ9.......");
    }
    
    private Date getStratDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.CQ9.getCode());
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
        logger = LoggerFactory.getLogger((Class)Cq9TaskJob.class);
    }
}
