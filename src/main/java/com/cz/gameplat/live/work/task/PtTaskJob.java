package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.live.core.api.pt.bean.*;
import java.util.*;
import org.slf4j.*;

@Component
public class PtTaskJob
{
    private static final Logger logger;
    @Resource
    private PtBetRecordService ptBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    @Resource
    private PtConfig ptConfig;
    
    public void getBet() {
        PtTaskJob.logger.info("Start job PT.......");
        try {
            if (!this.ptConfig.isOpen()) {
                PtTaskJob.logger.info("pt \u672a\u5f00\u542f...");
                return;
            }
            final Date startDate = this.getStartDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, (int)this.ptConfig.getTaskPeriod());
            final int min = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            if (min > 2) {
                endDate = DateUtil.getSecond(endDate, -1);
                final long startTime = System.currentTimeMillis();
                final String keys = "pt_" + DateUtil.dateToYMDHMS(startDate) + "_" + DateUtil.dateToYMDHMS(endDate);
                PtTaskJob.logger.info("Start PT task. keys = " + keys + ",startTime=" + startTime);
                Integer page = 1;
                final Integer rows = this.ptConfig.getPageLimit();
                this.ptBetRecordService.deleteTemp();
                Integer currentPage;
                Integer totalPage;
                do {
                    final PtReqBody ptReqBody = this.ptBetRecordService.getApiBet(startDate, endDate, page, rows);
                    final List<PtBetRecordRep> ptBetRecordRepList = (List<PtBetRecordRep>)ptReqBody.getPtBetRecordRepList();
                    if (ptReqBody == null) {
                        break;
                    }
                    if (ptBetRecordRepList.isEmpty()) {
                        break;
                    }
                    PtTaskJob.logger.info("Start PT task. TotalCount = " + ptReqBody.getTotalCount() + ",page=" + page + ",TotalPage=" + ptReqBody.getTotalPage());
                    ++page;
                    this.ptBetRecordService.batchSaveTemp((List)ptBetRecordRepList);
                    currentPage = ptReqBody.getCurrentPage();
                    totalPage = ptReqBody.getTotalPage();
                } while (currentPage < totalPage);
                final List<PtBetRecord> statList = (List<PtBetRecord>)this.ptBetRecordService.batchSave(endDate);
                if (statList != null) {
                    for (final PtBetRecord pt : statList) {
                        statSet.add(pt.getStatTime());
                        rebateSet.add(pt.getRebateTime());
                    }
                }
                for (final String statTime : rebateSet) {
                    this.liveUserDayReportService.saveDayReport(statTime, LiveGame.PT);
                }
                for (final String statTime : statSet) {
                    this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.PT);
                }
                PtTaskJob.logger.info("end PT task. key=" + keys + "\u8017\u65f6:" + (System.currentTimeMillis() - startTime));
            }
        }
        catch (Exception ex) {
            PtTaskJob.logger.error("PT \u5f02\u5e38\uff1a", (Throwable)ex);
        }
        PtTaskJob.logger.info("End job PT.......");
    }
    
    private Date getStartDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.PT.getCode());
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
        logger = LoggerFactory.getLogger((Class)PtTaskJob.class);
    }
}
