package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import org.slf4j.*;

@Component
public class DgTaskJob
{
    private static final Logger logger;
    @Resource
    private DgConfig dgConfig;
    @Resource
    private DgBetRecordService dgBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    
    public void getBet() {
        DgTaskJob.logger.info("Start job DG.......");
        try {
            if (!this.dgConfig.isOpen()) {
                DgTaskJob.logger.info("dg \u672a\u5f00\u542f...");
                return;
            }
            final List<DgBetRecord> list = (List<DgBetRecord>)this.dgBetRecordService.save();
            if (list != null && !list.isEmpty()) {
                for (final DgBetRecord dg : list) {
                    this.liveUserDayReportService.saveDayReport(dg.getRebateTime(), LiveGame.DG);
                    this.liveUserDayReportService.saveRebateReport(dg.getStatTime(), LiveGame.DG);
                }
            }
        }
        catch (Exception e) {
            DgTaskJob.logger.error("DG job \u5f02\u5e38\uff1a", (Throwable)e);
        }
        DgTaskJob.logger.info("End job DG.......");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)DgTaskJob.class);
    }
}
