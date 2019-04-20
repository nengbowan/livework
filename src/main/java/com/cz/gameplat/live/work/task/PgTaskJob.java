package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.pg.bean.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.pg.service.*;
import com.cz.gameplat.live.core.service.*;
import org.apache.commons.collections.*;
import com.cz.gameplat.live.core.pg.entity.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import org.slf4j.*;

@Component
public class PgTaskJob
{
    private static final Logger logger;
    @Resource
    private PgConfig pgConfig;
    @Resource
    private PgBetRecordService pgBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    
    public void getBet() {
        if (!this.pgConfig.isOpen()) {
            PgTaskJob.logger.info("---------PG-job-------\u672a\u5f00\u542f-----");
            return;
        }
        PgTaskJob.logger.info("---------PG-job-------Start-----");
        try {
            final List<PgBetRecord> list = (List<PgBetRecord>)this.pgBetRecordService.save();
            if (CollectionUtils.isNotEmpty((Collection)list)) {
                for (final PgBetRecord pg : list) {
                    this.liveUserDayReportService.saveDayReport(pg.getRebateTime(), LiveGame.PG);
                    this.liveUserDayReportService.saveRebateReport(pg.getStatTime(), LiveGame.PG);
                }
            }
        }
        catch (Exception e) {
            PgTaskJob.logger.error("---------PG-job-------\u5f02\u5e38\uff1a", (Throwable)e);
        }
        PgTaskJob.logger.info("---------PG-job-----End--");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)PgTaskJob.class);
    }
}
