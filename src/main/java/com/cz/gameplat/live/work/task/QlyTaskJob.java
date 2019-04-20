package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.qly.bean.*;
import java.util.*;
import org.slf4j.*;

@Component
public class QlyTaskJob
{
    private static final Logger logger;
    @Resource
    private QlyBetRecordService qlyBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    @Resource
    private QlyConfig qlyConfig;
    
    public void getBet() {
        QlyTaskJob.logger.info("Start job QLY.......");
        try {
            if (!this.qlyConfig.isOpen()) {
                QlyTaskJob.logger.info("qly \u672a\u5f00\u542f...");
                return;
            }
            final Date startDate = this.getStartDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, (int)this.qlyConfig.getTaskPeriod());
            final int min = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            if (min > 2) {
                endDate = DateUtil.getSecond(endDate, -1);
                final long startTime = System.currentTimeMillis();
                final String keys = "qly_" + DateUtil.dateToYMDHMS(startDate) + "_" + DateUtil.dateToYMDHMS(endDate);
                QlyTaskJob.logger.info("Start QLY task. keys = " + keys + ",startTime=" + startTime);
                Integer page = 1;
                final Integer rows = this.qlyConfig.getRow();
                final Map map = this.qlyBetRecordService.loadKind();
                final String hour = DateUtil.getNowMonth();
                this.qlyBetRecordService.deleteTemp();
                Integer currentPage;
                Integer totalPage;
                do {
                    final QlyReqBody qlyReqBody = this.qlyBetRecordService.getApiBet(startDate, endDate, page, rows, map, hour);
                    final List<QlyBetRecordRep> qlyBetRecordRepList = (List<QlyBetRecordRep>)qlyReqBody.getData();
                    if (qlyReqBody == null) {
                        break;
                    }
                    if (qlyBetRecordRepList.isEmpty()) {
                        break;
                    }
                    QlyTaskJob.logger.info("Start QLY task. TotalCount = " + qlyReqBody.getTotal() + ",page=" + page + ",TotalPage=" + qlyReqBody.getCurpage());
                    ++page;
                    this.qlyBetRecordService.batchSaveTemp((List)qlyBetRecordRepList);
                    currentPage = qlyReqBody.getCurpage();
                    totalPage = qlyReqBody.getTotal() / rows;
                } while (currentPage < totalPage);
                final List<QlyBetRecord> statList = (List<QlyBetRecord>)this.qlyBetRecordService.batchSave(endDate, true);
                if (statList != null) {
                    for (final QlyBetRecord qly : statList) {
                        statSet.add(qly.getStatTime());
                        rebateSet.add(qly.getRebateTime());
                    }
                }
                for (final String statTime : rebateSet) {
                    this.liveUserDayReportService.saveDayReport(statTime, LiveGame.QLY);
                }
                for (final String statTime : statSet) {
                    this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.QLY);
                }
                QlyTaskJob.logger.info("end QLY task. key=" + keys + "\u8017\u65f6:" + (System.currentTimeMillis() - startTime));
            }
        }
        catch (Exception ex) {
            QlyTaskJob.logger.error("QLY \u5f02\u5e38\uff1a", (Throwable)ex);
        }
        QlyTaskJob.logger.info("End job QLY.......");
    }
    
    private Date getStartDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.QLY.getCode());
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
        logger = LoggerFactory.getLogger((Class)QlyTaskJob.class);
    }
}
