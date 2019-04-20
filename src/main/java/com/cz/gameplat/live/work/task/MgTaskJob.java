package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.*;
import com.cz.framework.exception.*;
import java.util.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.mg.bean.*;
import org.slf4j.*;

@Component
public class MgTaskJob
{
    private static final Logger logger;
    @Resource
    private MgBetRecordService mgBetRecordService;
    @Resource
    private MgOldConfig mgConfig;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    private Set<String> statTimeSet;
    private Set<String> rebateTimeSet;
    
    public MgTaskJob() {
        this.statTimeSet = new HashSet<String>();
        this.rebateTimeSet = new HashSet<String>();
    }
    
    private void checkData() {
        try {
            final List<Date> needRefetchTimeList = (List<Date>)this.mgBetRecordService.getNeedRefetchRecordTimeList(new Date(System.currentTimeMillis() - 28800000L));
            for (final Date startTime : needRefetchTimeList) {
                final Date endTime = new Date(startTime.getTime() + 3600000L);
                MgTaskJob.logger.info("\u91cd\u65b0\u83b7\u53d6MG\u6295\u6ce8\u8bb0\u5f55:{} - {}", (Object)DateUtil.dateToYMDHMS(startTime), (Object)DateUtil.dateToYMDHMS(endTime));
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
            final int rows = this.mgConfig.getRows();
            int firstTotalCount = 0;
            do {
                final MgDataRep data = this.mgBetRecordService.getApiBetRecord(startTime, endTime, (String)null, (String)null, status, page, rows);
                if (data == null || data.getData() == null) {
                    break;
                }
                if (data.getData().isEmpty()) {
                    break;
                }
                totalCount = data.getTotalCount();
                if (page == 1) {
                    firstTotalCount = totalCount;
                }
                if (firstTotalCount != totalCount) {
                    MgTaskJob.logger.info("\u5f53\u6b21\u83b7\u53d6\u6570\u636e\u603b\u6761\u6570\u4e0d\u76f8\u7b49\uff0c\u7b2c1\u9875\u603b\u6570\u636e\uff1a" + firstTotalCount + ",\u5f53\u524d\u9875\u603b\u6570\u636e\uff1a" + totalCount + ",\u5f53\u524d\u9875\uff1a" + page + ",startTime=" + startTime + ",endTime=" + endTime);
                    break;
                }
                totalPage = ((totalCount % rows > 0) ? (totalCount / rows + 1) : (totalCount / rows));
                ++page;
                List<MgBetRecord> mglist = (List<MgBetRecord>)this.mgBetRecordService.batchSave(data.getData());
                if (mglist == null || mglist.isEmpty()) {
                    continue;
                }
                for (final MgBetRecord mg : mglist) {
                    this.statTimeSet.add(mg.getStatTime());
                    this.rebateTimeSet.add(mg.getRebateTime());
                }
                mglist.clear();
                mglist = null;
            } while (page <= totalPage);
            for (final String statTime : this.rebateTimeSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.MG);
            }
            for (final String statTime : this.statTimeSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.MG);
            }
        }
        catch (Exception ex) {
            MgTaskJob.logger.error("\u4e0b\u8f7dMG\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)ex);
        }
        finally {
            this.statTimeSet.clear();
            this.rebateTimeSet.clear();
        }
    }
    
    public void getBet() {
        MgTaskJob.logger.info("Start mg bet info.");
        final long st = System.currentTimeMillis();
        if (!this.mgConfig.isOpen()) {
            MgTaskJob.logger.info("mg \u672a\u5f00\u542f...");
            return;
        }
        Date startTime = this.mgBetRecordService.getUpdateTime();
        if (startTime == null) {
            startTime = DateUtil.getDateStart(new Date());
        }
        final Date endTime = new Date();
        this.checkData();
        this.fetchData(startTime, endTime, 0);
        MgTaskJob.logger.info("End mg bet info.\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)MgTaskJob.class);
    }
}
