package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.lmg.bean.*;
import java.util.*;
import org.slf4j.*;

@Component
public class LmgTaskJob
{
    private static final Logger logger;
    @Resource
    private LmgConfig lmgConfig;
    @Resource
    private LmgBetRecordService lmgBetDetailService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    
    public void getBet() {
        LmgTaskJob.logger.info("start lmg job..");
        try {
            if (!this.lmgConfig.isOpen()) {
                LmgTaskJob.logger.info("lmg \u672a\u5f00\u542f...");
                return;
            }
            final List<LmgBetRecordRep> list = (List<LmgBetRecordRep>)this.lmgBetDetailService.getApiBet();
            final List<LmgBetRecord> statTimeList = (List<LmgBetRecord>)this.lmgBetDetailService.batchSave((List)list);
            if (statTimeList != null && !statTimeList.isEmpty()) {
                for (final LmgBetRecord lmg : statTimeList) {
                    this.liveUserDayReportService.saveDayReport(lmg.getRebateTime(), LiveGame.LMG);
                    this.liveUserDayReportService.saveRebateReport(lmg.getStatTime(), LiveGame.LMG);
                }
            }
        }
        catch (Exception e) {
            LmgTaskJob.logger.error("\u4e0b\u8f7dLMG\u4e0b\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)e);
        }
        LmgTaskJob.logger.info("end lmg job..");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)LmgTaskJob.class);
    }
}
