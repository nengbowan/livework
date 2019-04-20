package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.config.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.live.core.api.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import java.util.*;
import org.slf4j.*;

@Component
public class Pt2TaskJob
{
    private static final Logger logger;
    @Resource
    private Pt2Config pt2Config;
    @Resource
    private Pt2BetRecordService pt2BetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    @Resource
    private Pt2Api pt2Api;
    private Set<String> statTimeSet;
    private Set<String> rebateTimeSet;
    
    public Pt2TaskJob() {
        this.statTimeSet = new HashSet<String>();
        this.rebateTimeSet = new HashSet<String>();
    }
    
    public void getBet() {
        Pt2TaskJob.logger.info("Start pt2 bet info.");
        final long st = System.currentTimeMillis();
        if (!this.pt2Config.isOpen()) {
            Pt2TaskJob.logger.info("pt2 \u672a\u5f00\u542f...");
            return;
        }
        try {
            final Date startDate = this.getStartDate();
            final Date newDate = new Date();
            Date endDate = DateUtil.getMinute(startDate, (int)this.pt2Config.getTaskPeriod());
            final int mins = DateUtil.betweenMinute(endDate, newDate);
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            final String token = this.pt2Api.getToken();
            if (mins > 2) {
                endDate = DateUtil.getSecond(endDate, -1);
                final Long startTaskTime = System.currentTimeMillis();
                Pt2TaskJob.logger.info("Start pt2 task. startDate=" + startDate);
                int offset = 0;
                this.pt2BetRecordService.deleteTemp();
                while (true) {
                    final List<Pt2BetRecord> list = (List<Pt2BetRecord>)this.pt2BetRecordService.getApiBetRecord(token, (String)null, offset, startDate, endDate, false);
                    final List<Pt2BetRecord> updateList = (List<Pt2BetRecord>)this.pt2BetRecordService.getApiBetRecord(token, (String)null, offset, startDate, endDate, true);
                    if (list != null) {
                        if (updateList != null) {
                            for (int i = 0; i < updateList.size(); ++i) {
                                if (list.contains(updateList.get(i))) {
                                    list.set(list.indexOf(updateList.get(i)), updateList.get(i));
                                }
                                else {
                                    list.add(updateList.get(i));
                                }
                            }
                        }
                        this.pt2BetRecordService.batchSaveTemp((List)list);
                    }
                    else {
                        if (list != null || updateList == null) {
                            break;
                        }
                        this.pt2BetRecordService.batchSaveTemp((List)updateList);
                    }
                    offset += this.pt2Config.getLimit();
                    Thread.sleep(1000L);
                }
                final List<Pt2BetRecord> statList = (List<Pt2BetRecord>)this.pt2BetRecordService.batchSave(endDate, true);
                if (statList != null) {
                    for (final Pt2BetRecord pt2BetRecord : statList) {
                        statSet.add(pt2BetRecord.getStatTime());
                        rebateSet.add(pt2BetRecord.getRebateTime());
                    }
                }
                Pt2TaskJob.logger.info("end pt2 task. \u8017\u65f6:" + (System.currentTimeMillis() - startTaskTime));
            }
            for (final String statTime : statSet) {
                this.liveUserDayReportService.saveDayReport(statTime, LiveGame.PT2);
            }
            for (final String statTime : rebateSet) {
                this.liveUserDayReportService.saveRebateReport(statTime, LiveGame.PT2);
            }
        }
        catch (Exception ex) {
            Pt2TaskJob.logger.error("\u4e0b\u8f7dpt2\u6295\u6ce8\u8bb0\u5f55\u5f02\u5e38:", (Throwable)ex);
        }
        finally {
            this.statTimeSet.clear();
            this.rebateTimeSet.clear();
        }
        Pt2TaskJob.logger.info("End pt2 bet info.\u8017\u65f6:" + (System.currentTimeMillis() - st));
    }
    
    private Date getStartDate() {
        Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.PT2.getCode());
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
        logger = LoggerFactory.getLogger((Class)Pt2TaskJob.class);
    }
}
