package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.framework.*;
import com.cz.framework.exception.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import com.cz.gameplat.live.core.api.ag.bean.*;
import org.slf4j.*;

@Component
public class AgTaskJob
{
    private static final Logger logger;
    @Resource
    private AgBetRecordService agBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private AgConfig agConfig;
    
    private void checkData() {
        try {
            final List<Date> needRefetchTimeList = (List<Date>)this.agBetRecordService.getNeedRefetchRecordTimeList(new Date(System.currentTimeMillis() - 43200000L));
            for (final Date startTime : needRefetchTimeList) {
                final Date endTime = new Date(startTime.getTime() + 3600000L);
                AgTaskJob.logger.info("重新获取AG投注记录:{} - {}", (Object)DateUtil.dateToYMDHMS(startTime), (Object)DateUtil.dateToYMDHMS(endTime));
                this.fetchData(startTime, endTime, 1);
            }
        }
        catch (BusinessException e) {
            e.printStackTrace();
        }
    }
    
    private void fetchData(final Date startTime, final Date endTime, final int status) {
        final long st = System.currentTimeMillis();
        final Set<String> statTimeSet = new HashSet<String>();
        final Set<String> rebateTimeSet = new HashSet<String>();
        try {
            int totalCount = 0;
            int page = 1;
            int totalPage = 1;
            final int rows = this.agConfig.getRows();
            int firstTotalCount = 0;
//            do {
            final List<AgBetRecord> data = this.agBetRecordService.getApiBetRecord(startTime, endTime, (String)null, (String)null, status, page, rows);
            if (data == null || data.size() == 0) {
                return;
            }

//                totalCount = data.getTotalCount();
//                if (page == 1) {
//                    firstTotalCount = totalCount;
//                }
//                if (firstTotalCount != totalCount) {
//                    AgTaskJob.logger.info("当次获取数据总条数不相等，第1页总数据：" + firstTotalCount + ",当前页总数据：" + totalCount + ",当前页：" + page + ",startTime=" + startTime + ",endTime=" + endTime);
//                    break;
//                }
//                totalPage = ((totalCount % rows > 0) ? (totalCount / rows + 1) : (totalCount / rows));
//                ++page;
            List<AgBetRecord> dlist = (List<AgBetRecord>)this.agBetRecordService.batchSave(data);
            if (dlist == null || dlist.isEmpty()) {
                return;
            }
            for (final AgBetRecord ag : dlist) {
                statTimeSet.add(ag.getStatTime());
                rebateTimeSet.add(ag.getRebateTime());
            }
            dlist.clear();
            dlist = null;
//            } while (page <= totalPage);
        }
        catch (Exception ex) {
            AgTaskJob.logger.error("下载AG投注记录异常, key:", (Throwable)ex);
        }
        finally {
            for (final String statTime : rebateTimeSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.AG);
            }
            for (final String statTime : statTimeSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.AG);
            }
            statTimeSet.clear();
            rebateTimeSet.clear();
        }
        AgTaskJob.logger.info("End ag,\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    public void getBet() {
        if (!this.agConfig.isOpen()) {
            AgTaskJob.logger.info("ag 未开启...");
            return;
        }
        AgTaskJob.logger.info("start ag........");
        Date startTime = this.agBetRecordService.getMaxAddTime();
        if (startTime == null) {
            startTime = DateUtil.getDateStart(new Date());
        }
//        this.checkData();
        this.fetchData(startTime, new Date(), 0);
    }
    
    public void saveDayReport() {
        try {
            final String statTime = DateUtil.getTimeZoneYMD("AMG-4");
            this.liveUserDayReportService.saveDayReport(statTime, LiveGame.AG);
        }
        catch (Exception ex) {
            AgTaskJob.logger.error("saveDayReport:", (Throwable)ex);
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)AgTaskJob.class);
    }
}
