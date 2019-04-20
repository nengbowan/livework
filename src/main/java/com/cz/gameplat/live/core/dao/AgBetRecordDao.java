package com.cz.gameplat.live.core.dao;

import com.cz.framework.dao.*;
import com.cz.gameplat.live.core.entity.*;
import org.springframework.stereotype.*;
import com.cz.framework.*;
import java.text.*;
import com.cz.framework.bean.*;
import com.cz.gameplat.live.core.constants.*;
import org.apache.commons.lang.*;
import java.util.*;
import com.cz.gameplat.live.core.bean.*;
import org.springframework.jdbc.core.*;

@Repository
public class AgBetRecordDao extends MysqlBaseDaoImpl<AgBetRecord, String> implements BetRecordDao
{
    public Date queryMaxUpdateTime() throws Exception {
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL("SELECT update_time FROM live_ds_bet_record  ORDER BY update_time DESC ");
        Date result = null;
        final DsBetRecord po = (DsBetRecord)sqlUtil.queryOne();
        if (po != null) {
            result = po.getUpdateTime();
        }
        return result;
    }

    public String getMaxAddTime() {
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL("SELECT update_time as add_time FROM live_ag_bet_record ORDER BY update_time DESC ");
        final AgBetRecord po = (AgBetRecord)sqlUtil.queryOne();
        String result = null;
        if (po != null) {
            result = DateUtil.dateToYMDHMS(po.getAddTime());
        }
        return result;
    }
    
    public void deleteTemp() {
        final String sql1 = "DELETE FROM live_ag_bet_record_temp";
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL(sql1);
        sqlUtil.execSQL();
    }
    
    public List<AgBetRecord> getTempStatTime() {
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL("SELECT distinct stat_time,rebate_time FROM live_ag_bet_record_temp");
        final List<AgBetRecord> list = (List<AgBetRecord>)sqlUtil.queryList();
        return list;
    }
    
    public void batchSaveTemp(final List<AgBetRecord> list) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder formSb = new StringBuilder();
        sb.append("INSERT INTO `live_ag_bet_record_temp`").append(" (`bill_no`, `account`, `data_type`, `win_amount`, `bet_time`, `game_type` ").append(", `bet_amount`, `valid_amount`, `flag`").append(", `game_kind`, `settle`, `agent_name`, `add_time`, `gmt_bet_time`").append(",  `stat_time`,`ames_time`,`update_time`,`rebate_time`,`bet_content`");
        sb.append(" ) VALUES ");
        formSb.append("(''{0}'',''{1}'',''{2}'',''{3}'',''{4}'',''{5}'',''{6}''").append(",''{7}'', ''{8}'',''{9}'',''{10}'',''{11}'',''{12}'',''{13}''").append(",''{14}'',''{15}'',''{16}'',''{17}'',''{18}''),");
        final MessageFormat form = new MessageFormat(formSb.toString());
        for (final AgBetRecord ag : list) {
            final String[] args = { ag.getBillNo() + "", ag.getAccount(), ag.getDataType(), ag.getWinAmount() + "", DateUtil.dateToYMDHMS(ag.getBetTime()), ag.getGameType(), ag.getBetAmount() + "", ag.getValidAmount() + "", ag.getFlag() + "", ag.getGameKind(), ag.getSettle() + "", ag.getAgentName(), DateUtil.dateToYMDHMS(ag.getAddTime()), DateUtil.dateToYMDHMS(ag.getGmtBetTime()), ag.getRebateTime(), DateUtil.dateToYMDHMS(ag.getAmesTime()), DateUtil.dateToYMDHMS(ag.getUpdateTime()), ag.getRebateTime(), ag.getBetContent() };
            sb.append(form.format(args));
        }
        String sql3 = sb.toString();
        sql3 = sql3.substring(0, sql3.length() - 1);
        this.batchUpdate(new String[] { sql3 });
    }
    
    public void batchSaveByTemp() {
        final String sql4 = "update live_ag_bet_record, live_ag_bet_record_temp set live_ag_bet_record.win_amount=live_ag_bet_record_temp.win_amount,live_ag_bet_record.bet_amount=live_ag_bet_record_temp.bet_amount,live_ag_bet_record.valid_amount=live_ag_bet_record_temp.valid_amount,live_ag_bet_record.settle=live_ag_bet_record_temp.settle,live_ag_bet_record.ames_time=live_ag_bet_record_temp.ames_time,live_ag_bet_record.rebate_time=live_ag_bet_record_temp.rebate_time,live_ag_bet_record.update_time=live_ag_bet_record_temp.update_time,live_ag_bet_record.flag=live_ag_bet_record_temp.flag where live_ag_bet_record.bill_no = live_ag_bet_record_temp.bill_no";
        final StringBuffer sql5 = new StringBuffer();
        sql5.append("INSERT INTO `live_ag_bet_record`").append(" (`bill_no`, `account`, `data_type`, `win_amount`, `bet_time`, `game_type` ").append(", `bet_amount`, `valid_amount`, `flag`").append(", `game_kind`, `settle`, `agent_name`, `add_time`, `gmt_bet_time` ").append(", `stat_time`,`ames_time`,`update_time`,`rebate_time`,`bet_content` ");
        sql5.append(" ) ");
        sql5.append(" SELECT t.`bill_no`, t.`account`, t.`data_type`, t.`win_amount`, t.`bet_time`, t.`game_type` ").append(", t.`bet_amount`, t.`valid_amount`, t.`flag`").append(", t.`game_kind`, t.`settle`, t.`agent_name`, t.`add_time`, t.`gmt_bet_time`").append(", t.`stat_time`,t.`ames_time`,t.`update_time`,t.`rebate_time`,t.`bet_content`").append(" FROM live_ag_bet_record_temp t LEFT JOIN live_ag_bet_record m ON t.bill_no=m.bill_no WHERE m.bill_no IS NULL");
        this.batchUpdate(new String[] { sql4, sql5.toString() });
    }
    
    public PageData<GameBetRecord> queryPageBetRecord(final QueryBetRecordParam param, final PageBean pageBean) {
        final String sql = "SELECT * FROM live_ag_bet_record t inner join user_info u on t.account=u.account WHERE 1=1 ";
        final TimeType tt = TimeType.getValue(param.getTimeType());
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL(sql);
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
        final String sql = "SELECT SUM(win_amount) AS win_amount,SUM(bet_amount) AS bet_amount,SUM(valid_amount) AS valid_amount FROM live_ag_bet_record t inner join user_info u on t.account=u.account WHERE t.`settle`=1 ";
        final TimeType tt = TimeType.getValue(param.getTimeType());
        final MysqlBaseDaoImpl.SQLUtil sqlUtil = this.buildSQL(sql);
        sqlUtil.addSqlAndArgs(" AND t.`account`=? ", (Object)param.getAccount()).addSqlLikeRight(" AND u.super_path LIKE ? ", param.getUserPaths()).addSqlAndArgs(" AND t.`stat_time`=? ", (Object)param.getStatTime()).addSqlAndArgs(" AND t.`game_kind`=? ", (Object)param.getGameKind()).addSqlAndArgs(" AND t.`" + tt.getField() + "` BETWEEN  ? AND ? ", new Object[] { param.getBetStartDate(), param.getBetEndDate() });
        final List<GameBetRecord> list = (List<GameBetRecord>)sqlUtil.queryList((Class)GameBetRecord.class);
        return (list != null && list.size() > 0) ? list.get(0) : null;
    }
    
    public CheckInfo queryCheckInfo(final Date startTime, final Date endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return (CheckInfo)this.getJdbcTemplate().queryForObject("SELECT SUM(bet_amount) AS totalBetMoney, SUM(win_amount) AS totalWinMoney FROM live_ag_bet_record WHERE bet_time BETWEEN ? AND ?", (RowMapper)new BeanPropertyRowMapper((Class)CheckInfo.class), new Object[] { startTime, endTime });
    }
}
