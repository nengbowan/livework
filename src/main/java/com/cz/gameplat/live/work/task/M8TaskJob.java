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
public class M8TaskJob
{
    private static final Logger logger;
    @Resource
    private M8Config m8Config;
    @Resource
    private M8BetRecordService betRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    
    public void getBet() {
        M8TaskJob.logger.info("Start job M8.......");
        try {
            if (!this.m8Config.isOpen()) {
                M8TaskJob.logger.info("m8 \u672a\u5f00\u542f...");
                return;
            }
            final List<M8BetRecord> list = (List<M8BetRecord>)this.betRecordService.save();
            if (list != null && !list.isEmpty()) {
                for (final M8BetRecord m8 : list) {
                    this.liveUserDayReportService.saveDayReport(m8.getRebateTime(), LiveGame.M8);
                    this.liveUserDayReportService.saveRebateReport(m8.getStatTime(), LiveGame.M8);
                }
            }
        }
        catch (Exception e) {
            M8TaskJob.logger.error("m8 job\u5f02\u5e38\uff1a", (Throwable)e);
        }
        M8TaskJob.logger.info("End job M8.......");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)M8TaskJob.class);
    }
}
