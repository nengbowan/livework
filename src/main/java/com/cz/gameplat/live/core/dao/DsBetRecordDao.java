package com.cz.gameplat.live.core.dao;

import com.cz.framework.dao.*;
import com.cz.gameplat.live.core.entity.*;
import org.springframework.stereotype.*;
import com.cz.gameplat.live.core.api.ds.bean.*;
import java.text.*;
import com.cz.framework.*;
import java.util.*;
import com.cz.framework.bean.*;
import com.cz.gameplat.live.core.bean.*;
import com.cz.gameplat.live.core.constants.*;
import org.apache.commons.lang.*;

@Repository
public class DsBetRecordDao extends MysqlBaseDaoImpl<DsBetRecord, Long> implements BetRecordDao
{
    public String queryMaxSequenceNo() throws Exception {
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL("SELECT bill_no FROM live_ds_bet_record  ORDER BY bill_no DESC ");
        String result = null;
        final DsBetRecord po = (DsBetRecord)sqlUtil.queryOne();
        if (po != null) {
            result = po.getBillNo();
        }
        return result;
    }

    public Date queryMaxUpdateTime() throws Exception {
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL("SELECT update_time FROM live_ds_bet_record  ORDER BY update_time DESC ");
        Date result = null;
        final DsBetRecord po = (DsBetRecord)sqlUtil.queryOne();
        if (po != null) {
            result = po.getUpdateTime();
        }
        return result;
    }


    
    public void deleteTemp() {
        final String sql1 = "DELETE FROM live_ds_bet_record_temp";
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL(sql1);
        sqlUtil.execSQL();
    }
    
    public List<DsBetRecord> getTempStatTime() {
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL("SELECT distinct stat_time,rebate_time FROM live_ds_bet_record_temp");
        final List<DsBetRecord> list = (List<DsBetRecord>)sqlUtil.queryList();
        return list;
    }
    
    public void batchSaveTemp(final List<DsBetRecordRep> list) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder formSb = new StringBuilder();
        sb.append("INSERT INTO `live_ds_bet_record_temp` ").append(" (`bill_no`, `account`, `game_type`, `bet_amount`,`win_amount` ").append(", `valid_amount`, `bet_time`, `settle`,`game_kind`, `gmt_bet_time` ").append(", `stat_time`, `add_time`, `update_time`, `comm`,ames_time, `rebate_time`");
        sb.append(" ) VALUES ");
        formSb.append("(''{0}'',''{1}'',''{2}'',''{3}'',{4},''{5}'',''{6}''").append(",''{7}'', ''{8}'',''{9}'',''{10}'',''{11}'',''{12}'',''{13}'',''{14}'',''{15}''),");
        final MessageFormat form = new MessageFormat(formSb.toString());
        final String date = DateUtil.getNowTime();
        for (final DsBetRecordRep ds : list) {
            final String bet = DateUtil.dateToYMDHMS(ds.getBetTime());
            final String[] args = { ds.getSequenceNo() + "", ds.getUserName(), ds.getGameType() + "", ds.getStakeAmount() + "", ds.getWinLoss() + "", ds.getValidStake() + "", bet, ds.getSettle() + "", ds.getGameKind(), bet, ds.getRebateTime(), date, date, ds.getComm() + "", ds.getAmesTime(), ds.getRebateTime() };
            sb.append(form.format(args));
        }
        String sql3 = sb.toString();
        sql3 = sql3.substring(0, sql3.length() - 1);
        this.batchUpdate(new String[] { sql3 });
    }
    
    public void batchSaveByTemp() {
        final String sql4 = "update live_ds_bet_record, live_ds_bet_record_temp set live_ds_bet_record.update_time=live_ds_bet_record_temp.update_time,live_ds_bet_record.bet_amount=live_ds_bet_record_temp.bet_amount,live_ds_bet_record.win_amount=live_ds_bet_record_temp.win_amount,live_ds_bet_record.valid_amount=live_ds_bet_record_temp.valid_amount,live_ds_bet_record.comm=live_ds_bet_record_temp.comm,live_ds_bet_record.ames_time=live_ds_bet_record_temp.ames_time,live_ds_bet_record.rebate_time=live_ds_bet_record_temp.rebate_time,live_ds_bet_record.settle=live_ds_bet_record_temp.settle  where live_ds_bet_record.bill_no = live_ds_bet_record_temp.bill_no ";
        final StringBuffer sql5 = new StringBuffer();
        sql5.append("INSERT INTO `live_ds_bet_record`").append(" (`bill_no`, `account`, `game_type`, `bet_amount`,`win_amount`").append(", `valid_amount`, `bet_time`, `settle`,`game_kind`, `gmt_bet_time`").append(", `stat_time`, `add_time`, `update_time`, `comm`,ames_time, `rebate_time` ");
        sql5.append(" ) ");
        sql5.append("SELECT  t.`bill_no`, t.`account`, t.`game_type`, t.`bet_amount`,t.`win_amount`").append(", t.`valid_amount`, t.`bet_time`, t.`settle`,t.`game_kind`, t.`gmt_bet_time`").append(", t.`stat_time`, t.`add_time`, t.`update_time`, t.`comm`,t.ames_time, t.`rebate_time` ").append(" FROM live_ds_bet_record_temp t LEFT JOIN live_ds_bet_record m ON t.bill_no=m.bill_no WHERE m.bill_no IS NULL");
        this.batchUpdate(new String[] { sql4, sql5.toString() });
    }
    
    public PageData<GameBetRecord> queryPageBetRecord(final QueryBetRecordParam param, final PageBean pageBean) {
        final String sql = "SELECT * FROM live_ds_bet_record t inner join user_info u on t.account=u.account WHERE 1=1 ";
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL(sql);
        final TimeType tt = TimeType.getValue(param.getTimeType());
        sqlUtil.addSqlAndArgs(" AND t.`account`=? ", (Object)param.getAccount()).addSqlLikeRight(" AND u.super_path LIKE ? ", param.getUserPaths()).addSqlAndArgs(" AND t.`stat_time`=? ", (Object)param.getStatTime()).addSqlAndArgs(" AND t.`game_kind`=? ", (Object)param.getGameKind()).addSqlAndArgs(" AND t.`" + tt.getField() + "` BETWEEN  ? AND ? ", new Object[] { param.getBetStartDate(), param.getBetEndDate() });
        if (StringUtils.isNotBlank(param.getOrderByField())) {
            sqlUtil.addSql(" ORDER BY t.`" + param.getOrderByField() + "` " + param.getOrderBySort());
        }
        else {
            sqlUtil.addSql(" ORDER BY t.`" + tt.getOrderField() + "` DESC");
        }
        return (PageData<GameBetRecord>)sqlUtil.queryPage(pageBean, (Class)GameBetRecord.class);
    }
    
    public GameBetRecord queryBetRecordTotal(final QueryBetRecordParam param) {
        final String sql = "SELECT SUM(win_amount) AS win_amount,SUM(bet_amount) AS bet_amount,SUM(valid_amount) AS valid_amount FROM live_ds_bet_record t inner join user_info u on t.account=u.account WHERE t.`settle`=1 ";
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL(sql);
        final TimeType tt = TimeType.getValue(param.getTimeType());
        sqlUtil.addSqlAndArgs(" AND t.`account`=? ", (Object)param.getAccount()).addSqlLikeRight(" AND u.super_path LIKE ? ", param.getUserPaths()).addSqlAndArgs(" AND t.`stat_time`=? ", (Object)param.getStatTime()).addSqlAndArgs(" AND t.`game_kind`=? ", (Object)param.getGameKind()).addSqlAndArgs(" AND t.`" + tt.getField() + "` BETWEEN  ? AND ? ", new Object[] { param.getBetStartDate(), param.getBetEndDate() });
        final List<GameBetRecord> list = (List<GameBetRecord>)sqlUtil.queryList((Class)GameBetRecord.class);
        return (list != null && list.size() > 0) ? list.get(0) : null;
    }
}
