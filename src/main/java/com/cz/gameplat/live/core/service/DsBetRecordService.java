package com.cz.gameplat.live.core.service;

import freemarker.template.SimpleDate;
import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.api.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.gameplat.live.core.dao.*;
import com.cz.gameplat.live.core.api.ds.bean.*;
import com.cz.framework.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;

@Service
public class DsBetRecordService
{
    @Resource
    private DsApi dsApi;
    @Resource
    private DsConfig dsConfig;
    @Resource
    private DsBetRecordDao dsBetRecordDao;
    @Resource
    private LiveDmlService liveDmlService;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    private static Date defaultRecordTime = null;

    static{
        try {
            defaultRecordTime = sdf.parse("2019-04-20 10:35:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    public List<DsBetRecordRep> getList() throws Exception {

        final Date beginTime = this.dsBetRecordDao.queryMaxUpdateTime();
        final List<DsBetRecordRep> list = this.dsApi.getRecord((beginTime == null) ? defaultRecordTime : beginTime);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (final DsBetRecordRep rep : list) {
            rep.setBetTime(new Date(rep.getEndTime()));
            rep.setGameKind("DSIN");
            final Date amesTime = DateUtil.getUSToAMES(rep.getBetTime());
            rep.setAmesTime(DateUtil.dateToYMDHMS(amesTime));
            rep.setRebateTime(DateUtil.dateToYMD(amesTime));
            rep.setStatTime(this.getStatTime(rep.getBetTime()));
            rep.setWinLoss(rep.getWinLoss() * -1.0);
            int settle = 1;
            if ("-1".equals(rep.getBankerResult())) {
                settle = -1;
            }
            rep.setSettle(settle);
        }
        return list;
    }
    
    private String getStatTime(final Date betTime) {
        final String statTime = DateUtil.dateToYMD(betTime);
        final Date startTime = DateUtil.strToDate(statTime + " 07:59:59");
        if (DateUtil.dateCompareByYmdhms(betTime, startTime)) {
            return DateUtil.dateToYMD(DateUtil.addDate(betTime, -1));
        }
        return statTime;
    }
    
    public List<DsBetRecord> batchSave(final List<DsBetRecordRep> list) throws Exception {
        if (list == null || list.isEmpty()) {
            return null;
        }
        this.dsBetRecordDao.deleteTemp();
        this.dsBetRecordDao.batchSaveTemp(list);
        this.liveDmlService.liveDml(LiveGame.DS);
        this.dsBetRecordDao.batchSaveByTemp();
        return this.dsBetRecordDao.getTempStatTime();
    }
}
