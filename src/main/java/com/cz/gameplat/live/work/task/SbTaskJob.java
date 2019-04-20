package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.exception.*;
import java.util.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.sb.bean.*;
import org.slf4j.*;

@Component
public class SbTaskJob
{
    private static final Logger logger;
    @Resource
    private SbConfig sbConfig;
    @Resource
    private SbBetRecordService sbBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    private Set<String> statTimeSet;
    private Set<String> rebateTimeSet;
    
    public SbTaskJob() {
        this.statTimeSet = new HashSet<String>();
        this.rebateTimeSet = new HashSet<String>();
    }
    
    public void getBet() {
        SbTaskJob.logger.info("Start sb bet info.");
        final long st = System.currentTimeMillis();
        if (!this.sbConfig.isOpen()) {
            SbTaskJob.logger.info("sb \u672a\u5f00\u542f...");
            return;
        }
        this.getBetByCollect();
        SbTaskJob.logger.info("End sb bet info.\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    private void checkData(final Date date) {
        try {
            final List<Date> needRefetchTimeList = (List<Date>)this.sbBetRecordService.getNeedRefetchRecordTimeList(new Date(System.currentTimeMillis() - 43200000L));
            for (final Date startTime : needRefetchTimeList) {
                final Date endTime = new Date(startTime.getTime() + 3600000L);
                SbTaskJob.logger.info("\u91cd\u65b0\u83b7\u53d6SB\u6295\u6ce8\u8bb0\u5f55:{} - {}", (Object)DateUtil.dateToYMDHMS(startTime), (Object)DateUtil.dateToYMDHMS(endTime));
                this.fetchData(startTime, endTime, 1);
            }
        }
        catch (BusinessException e) {
            e.printStackTrace();
        }
    }
    
    private void fetchData(final Date startTime, final Date endTime, final int status) {
        try {
            int totalCount = 0;
            int totalPage = 1;
            int page = 1;
            final int rows = this.sbConfig.getRows();
            do {
                final SbDataRep data = this.sbBetRecordService.getApiBetRecord(startTime, endTime, (String)null, (String)null, status, page, rows);
                if (data == null || data.getData() == null) {
                    break;
                }
                if (data.getData().isEmpty()) {
                    break;
                }
                totalCount = data.getTotalCount();
                LogUtil.info("\u65f6\u95f4[" + startTime + "--" + endTime + "],\u91c7\u96c6\u6570\u636e[" + totalCount + "]\u6761");
                totalPage = ((totalCount % rows > 0) ? (totalCount / rows + 1) : (totalCount / rows));
                ++page;
                List<SbBetRecord> sbList = (List<SbBetRecord>)this.sbBetRecordService.batchSave(data.getData());
                if (sbList == null || sbList.isEmpty()) {
                    continue;
                }
                for (final SbBetRecord sb : sbList) {
                    this.statTimeSet.add(sb.getStatTime());
                    this.rebateTimeSet.add(sb.getRebateTime());
                }
                sbList.clear();
                sbList = null;
            } while (page <= totalPage);
            for (final String statTime : this.rebateTimeSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.SB);
            }
            for (final String statTime : this.statTimeSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.SB);
            }
        }
        catch (Exception ex) {
            SbTaskJob.logger.error("\u4e0b\u8f7dsb\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)ex);
        }
        finally {
            this.statTimeSet.clear();
            this.rebateTimeSet.clear();
        }
    }
    
    private void getBetByCollect() {
        Date startTime = this.sbBetRecordService.getUpdateTime();
        if (startTime == null) {
            startTime = DateUtil.getDateStart(new Date());
        }
        final Date endTime = new Date();
        this.checkData(startTime);
        this.fetchData(startTime, endTime, 0);
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)SbTaskJob.class);
    }
}
