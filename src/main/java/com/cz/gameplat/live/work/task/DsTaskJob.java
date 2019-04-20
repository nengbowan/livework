package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.ds.bean.*;
import java.util.*;
import org.slf4j.*;

@Component
public class DsTaskJob
{
    private static final Logger logger;
    @Resource
    private DsConfig dsConfig;
    @Resource
    private DsBetRecordService dsBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    
    public void getBet() {
        DsTaskJob.logger.info("Start job DS.......");
        try {
            if (!this.dsConfig.isOpen()) {
                DsTaskJob.logger.info("ds \u672a\u5f00\u542f...");
                return;
            }
            final List<DsBetRecordRep> list = (List<DsBetRecordRep>)this.dsBetRecordService.getList();
            final List<DsBetRecord> statTimeList = (List<DsBetRecord>)this.dsBetRecordService.batchSave((List)list);
            if (statTimeList != null && !statTimeList.isEmpty()) {
                for (final DsBetRecord ds : statTimeList) {
                    this.liveUserDayReportService.saveDayReport(ds.getRebateTime(), LiveGame.DS);
                    this.liveUserDayReportService.saveRebateReport(ds.getStatTime(), LiveGame.DS);
                }
            }
        }
        catch (Exception e) {
            DsTaskJob.logger.error("ds job \u5f02\u5e38", (Throwable)e);
        }
        DsTaskJob.logger.info("End job DS.......");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)DsTaskJob.class);
    }
}
