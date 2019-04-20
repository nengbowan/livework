package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import org.slf4j.*;

@Component
public class LiveUserDayReportTaskJob
{
    private static final Logger logger;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveDmlService liveDmlService;
    
    public void saveDayReport() {
        final Date day = DateUtil.addDate(new Date(), -1);
        final String statTime = DateUtil.dateToYMD(day);
        for (final LiveGame game : LiveGame.values()) {
            try {
                if (!game.getCode().equals(LiveGame.WZRY.getCode())) {
                    this.liveUserDayReportService.saveDayReport(statTime, game);
                }
            }
            catch (Exception ex) {
                LiveUserDayReportTaskJob.logger.error("saveDayReport:game=" + game + ",statTime=" + statTime, (Throwable)ex);
            }
        }
    }
    
    public void saveRebateData() {
        final Date day = DateUtil.addDate(new Date(), -1);
        final String statTime = DateUtil.dateToYMD(day);
        for (final LiveGame game : LiveGame.values()) {
            try {
                if (!game.getCode().equals(LiveGame.WZRY.getCode())) {
                    this.liveUserDayReportService.saveRebateReport(statTime, game);
                }
            }
            catch (Exception ex) {
                LiveUserDayReportTaskJob.logger.error("saveRebateData:game=" + game + ",statTime=" + statTime, (Throwable)ex);
            }
        }
    }
    
    public void clearLiveData() {
        LiveUserDayReportTaskJob.logger.info("Start clearLiveData......");
        final long time = System.currentTimeMillis();
        try {
            final Calendar calendar = Calendar.getInstance();
            calendar.set(5, 1);
            final Date date = calendar.getTime();
            final String statTime = DateUtil.dateToYMD(date);
            for (final LiveGame game : LiveGame.values()) {
                try {
                    if (!game.getCode().equals(LiveGame.WZRY.getCode())) {
                        final Set<String> statTimeList = (Set<String>)this.liveDmlService.getStatTimeList(game, statTime);
                        for (final String st : statTimeList) {
                            this.liveDmlService.clearLiveData(game, st);
                        }
                    }
                }
                catch (Exception ex) {
                    LiveUserDayReportTaskJob.logger.error("\u5220\u9664\u771f\u4eba\u6570\u636e:game=" + game + ",statTime=" + statTime, (Throwable)ex);
                }
            }
        }
        catch (Exception ex2) {
            LiveUserDayReportTaskJob.logger.error("job \u5f02\u5e38\uff1a", (Throwable)ex2);
        }
        LiveUserDayReportTaskJob.logger.info("End clearLiveData......\u8017\u65f6:" + (System.currentTimeMillis() - time));
    }
    
    public void clearLiveReportData() {
        LiveUserDayReportTaskJob.logger.info("Start clearLiveReportData......");
        final long time = System.currentTimeMillis();
        try {
            final Calendar calendar = Calendar.getInstance();
            calendar.add(2, -6);
            calendar.set(5, 1);
            final Date date = calendar.getTime();
            final String statTime = DateUtil.dateToYMD(date);
            this.liveUserDayReportService.deleteLiveReportData(statTime);
        }
        catch (Exception ex) {
            LiveUserDayReportTaskJob.logger.error("\u5220\u9664\u771f\u4eba\u62a5\u8868job\u5f02\u5e38\uff1a", (Throwable)ex);
        }
        LiveUserDayReportTaskJob.logger.info("End clearLiveReportData......\u8017\u65f6:" + (System.currentTimeMillis() - time));
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)LiveUserDayReportTaskJob.class);
    }
}
