package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.hb.bean.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.hb.service.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.utils.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import com.cz.gameplat.live.core.hb.entity.*;
import java.util.function.*;
import java.util.stream.*;
import com.cz.framework.*;
import org.slf4j.*;

@Component
public class HbTaskJob
{
    private static final Logger logger;
    @Resource
    private HbConfig hbConfig;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    @Resource
    private HbBetRecordService hbBetRecordService;
    @Resource
    private LiveDmlService liveDmlService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    
    public void getBet() {
        HbTaskJob.logger.info("-------HB----Job--start-----");
        try {
            if (!this.hbConfig.isOpen()) {
                HbTaskJob.logger.info("-------HB----Job---\u672a\u5f00\u542f------");
                return;
            }
            final Date startDate = this.getStartDate();
            final Date endDate = new Date();
            final Date utcStartTime = DateUtils.aquireBJToUTC(startDate);
            final Date utcEndTime = DateUtils.aquireBJToUTC(endDate);
            final List<HbBetRecord> list = (List<HbBetRecord>)this.hbBetRecordService.getApiBet(utcStartTime, utcEndTime);
            if (CollectionUtils.isNotEmpty((Collection)list)) {
                this.hbBetRecordService.deleteTemp();
                this.hbBetRecordService.batchSaveTemp((List)list);
                this.liveDmlService.liveDml(LiveGame.HB);
                final List<HbBetRecord> statList = (List<HbBetRecord>)this.hbBetRecordService.batchSave();
                if (CollectionUtils.isNotEmpty((Collection)statList)) {
                    statList.stream().map(HbBetRecord::getStatTime).collect(Collectors.toSet()).forEach(statTime -> this.liveUserDayReportService.saveDayReport(statTime, LiveGame.HB));
                    statList.stream().map(HbBetRecord::getRebateTime).collect(Collectors.toSet()).forEach(rebateTime -> this.liveUserDayReportService.saveRebateReport(rebateTime, LiveGame.HB));
                }
            }
            this.liveWorkInfoService.update(LiveGame.HB.getCode(), endDate);
        }
        catch (Exception ex) {
            HbTaskJob.logger.error("-------HB----Job---\u5f02\u5e38\uff1a{}", (Object)ex.toString());
        }
        HbTaskJob.logger.info("-------HB----Job--end-----");
    }
    
    private Date getStartDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.HB.getCode());
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
        logger = LoggerFactory.getLogger((Class)HbTaskJob.class);
    }
}
