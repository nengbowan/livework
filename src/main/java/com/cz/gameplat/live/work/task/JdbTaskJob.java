package com.cz.gameplat.live.work.task;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.framework.task.*;
import com.alibaba.fastjson.*;
import org.apache.http.*;
import com.cz.gameplat.live.core.api.jdb.bean.*;
import com.cz.gameplat.live.core.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.utils.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.util.zip.*;
import org.slf4j.*;

@Component
public class JdbTaskJob
{
    private static final Logger logger;
    @Resource
    private JdbBetRecordService jdbBetRecordService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private JdbConfig jdbConfig;
    @Resource
    private LiveWorkInfoService liveWorkInfoService;
    private static final RequestApiQueue1 queue;
    
    public void getBet() {
        JdbTaskJob.logger.info("Start job JDB.......");
        String respJson = null;
        JdbBetRecordRep betRep = null;
        try {
            if (!this.jdbConfig.isOpen()) {
                JdbTaskJob.logger.info("JDB \u672a\u5f00\u542f...");
                return;
            }
            final Map map = this.getEasternTime();
            final Set<String> statSet = new HashSet<String>();
            final Set<String> rebateSet = new HashSet<String>();
            final String apiUrl = new String(this.jdbConfig.getApiUrl() + "?dc=" + this.jdbConfig.getDc() + "&x=");
            final HashMap<String, Object> getBetDetailParams = new HashMap<String, Object>();
            getBetDetailParams.put("action", 29);
            getBetDetailParams.put("ts", new Date().getTime());
            getBetDetailParams.put("parent", this.jdbConfig.getAgentName());
            getBetDetailParams.put("starttime", map.get("startTime"));
            getBetDetailParams.put("endtime", map.get("endTime"));
            final String encryptParams = EncryptUtils.encrypt(JSON.toJSON((Object)getBetDetailParams).toString(), this.jdbConfig.getKey(), this.jdbConfig.getIv());
            JdbTaskJob.logger.info("[JDB]\u83b7\u53d6\u4e0b\u6ce8\u8be6\u7ec6\u8bb0\u5f55\uff0c\u8bf7\u6c42URL\uff1a" + apiUrl + encryptParams);
            respJson = HttpClientUtils.doGet(apiUrl + encryptParams, (Header[])null);
            betRep = (JdbBetRecordRep)JSON.parseObject(respJson, (Class)JdbBetRecordRep.class);
            if (betRep.getStatus().equals("0000")) {
                if (null != betRep.getData() && betRep.getData().size() > 0) {
                    final List<JdbBetRecord> dataList = (List<JdbBetRecord>)this.jdbBetRecordService.batchSave(betRep.getData());
                    for (final JdbBetRecord jdb : dataList) {
                        statSet.add(jdb.getStatTime());
                        rebateSet.add(jdb.getRebateTime());
                    }
                    for (final String statTime : statSet) {
                        this.liveUserDayReportService.saveDayReport(statTime, LiveGame.JDB);
                    }
                    for (final String rebatTime : rebateSet) {
                        this.liveUserDayReportService.saveRebateReport(rebatTime, LiveGame.JDB);
                    }
                    JdbTaskJob.logger.info("[JDB]\u83b7\u53d6\u8be5\u65f6\u95f4\u6bb5\u4e0b\u6ce8\u8be6\u7ec6\u8bb0\u5f55\u6210\u529f\uff1a" + map.get("startTime") + " - " + map.get("endTime"));
                }
                else {
                    JdbTaskJob.logger.info("[JDB]\u672a\u83b7\u53d6\u5230\u8be5\u65f6\u95f4\u6bb5\u4e0b\u6ce8\u8be6\u7ec6\u8bb0\u5f55\uff1a" + map.get("startTime") + " - " + map.get("endTime"));
                }
            }
            else {
                JdbTaskJob.logger.error("[JDB]\u83b7\u53d6\u4e0b\u6ce8\u8be6\u7ec6\u8bb0\u5f55\u5931\u8d25\uff0c\u54cd\u5e94\u6570\u636e\uff1a" + respJson);
            }
        }
        catch (Exception ex) {
            JdbTaskJob.logger.error("JDB \u5f02\u5e38\uff1a" + ex);
        }
        JdbTaskJob.logger.info("End job JDB.......");
    }
    
    private Map<String, String> getEasternTime() {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        final Calendar calendar = Calendar.getInstance();
        calendar.add(11, -12);
        calendar.add(12, -3);
        calendar.set(13, 0);
        final String endTime = sdf.format(calendar.getTime());
        calendar.add(12, -5);
        calendar.set(13, 0);
        final String startTime = sdf.format(calendar.getTime());
        final String start = DateUtil.dateToStr(DateUtil.strToDate(startTime, "dd-MM-yyyy HH:mm:ss"), "yyyyMMddHHmm");
        final String end = DateUtil.dateToStr(DateUtil.strToDate(endTime, "dd-MM-yyyy HH:mm:ss"), "yyyyMMddHHmm");
        final Map map = new HashMap();
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("fileName", start + "_" + end + ".zip");
        return (Map<String, String>)map;
    }
    
    public void getFtpBetData() {
        final List<String> fileList = this.download();
        for (final String file : fileList) {
//            JdbTaskJob.queue.add(file, (Runnable)new JdbTaskJob.JdbTaskJob$1(this, file));
        }
        final Calendar calendar = Calendar.getInstance();
        calendar.add(11, -12);
        this.liveWorkInfoService.update(LiveGame.JDB.getCode(), calendar.getTime());
    }
    
    public List<String> download() {
        final String startDate = this.getStratDate();
        JdbTaskJob.logger.info("Start download JDB betData\uff0cfolderName = " + startDate);
        final long startTime = System.currentTimeMillis();
        final List<String> allList = new ArrayList<String>();
        final JdbFtpUtil ftp = new JdbFtpUtil();
        try {
            ftp.createConnect(this.jdbConfig.getFtpHost(), this.jdbConfig.getFtpPort(), this.jdbConfig.getAgentName() + this.jdbConfig.getFtpAccount() + this.jdbConfig.getDc(), this.jdbConfig.getFtpPassword());
            final List<String> jjList = (List<String>)ftp.download("/Bar Game/" + startDate, this.jdbConfig.getLocalDir());
            if (jjList != null && jjList.size() > 0) {
                allList.addAll(jjList);
            }
            final List<String> qpList = (List<String>)ftp.download("/Card/" + startDate, this.jdbConfig.getLocalDir());
            if (qpList != null && qpList.size() > 0) {
                allList.addAll(qpList);
            }
            final List<String> byList = (List<String>)ftp.download("/Fishing/" + startDate, this.jdbConfig.getLocalDir());
            if (byList != null && byList.size() > 0) {
                allList.addAll(byList);
            }
            final List<String> cpList = (List<String>)ftp.download("/Lottery/" + startDate, this.jdbConfig.getLocalDir());
            if (cpList != null && cpList.size() > 0) {
                allList.addAll(cpList);
            }
            final List<String> lhjList = (List<String>)ftp.download("/Slot/" + startDate, this.jdbConfig.getLocalDir());
            if (lhjList != null && lhjList.size() > 0) {
                allList.addAll(lhjList);
            }
        }
        catch (Exception e) {
            JdbTaskJob.logger.error("FTP\u4e0b\u8f7d\u6587\u4ef6\u5f02\u5e38:", (Throwable)e);
        }
        finally {
            ftp.closeConnect();
        }
        JdbTaskJob.logger.info("End download JDB BetData, folderName: " + startDate + ", dataSiz\uff1a" + allList.size() + "\uff0c\u8017\u65f6:" + (System.currentTimeMillis() - startTime));
        return allList;
    }
    
    private boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (int i = 0; i < children.length; ++i) {
                final boolean success = this.deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    public void deleteJdbDir() {
        final long startTime = System.currentTimeMillis();
        JdbTaskJob.logger.info("\u5220\u9664\u7f8e\u4e1c\u65f6\u95f43\u5929\u524dJDB\u4e0b\u8f7d\u76ee\u5f55\u5f00\u59cb........ ");
        try {
            final Calendar calendar = Calendar.getInstance();
            calendar.add(11, -12);
            calendar.add(5, -3);
            final int dateymd = Integer.parseInt(DateUtil.dateToStr(calendar.getTime(), "yyyyMMdd"));
            final File jjFile = new File(this.jdbConfig.getLocalDir() + "/Bar Game/");
            if (jjFile != null && jjFile.listFiles() != null) {
                for (final File file : jjFile.listFiles()) {
                    final int fv = new Integer(file.getName());
                    if (fv < dateymd) {
                        final boolean flag = this.deleteDir(file);
                        JdbTaskJob.logger.info("\u5220\u9664jdb\u8857\u673aBar Game\u76ee\u5f55\uff1a" + fv + (flag ? "\u6210\u529f" : "\u5931\u8d25"));
                    }
                }
            }
            final File qpFile = new File(this.jdbConfig.getLocalDir() + "/Card/");
            if (qpFile != null && qpFile.listFiles() != null) {
                for (final File file2 : qpFile.listFiles()) {
                    final int fv2 = new Integer(file2.getName());
                    if (fv2 < dateymd) {
                        final boolean flag2 = this.deleteDir(file2);
                        JdbTaskJob.logger.info("\u5220\u9664jdb\u68cb\u724cCard\u76ee\u5f55\uff1a" + fv2 + (flag2 ? "\u6210\u529f" : "\u5931\u8d25"));
                    }
                }
            }
            final File byFile = new File(this.jdbConfig.getLocalDir() + "/Fishing/");
            if (byFile != null && byFile.listFiles() != null) {
                for (final File file3 : byFile.listFiles()) {
                    final int fv3 = new Integer(file3.getName());
                    if (fv3 < dateymd) {
                        final boolean flag3 = this.deleteDir(file3);
                        JdbTaskJob.logger.info("\u5220\u9664jdb\u6355\u9c7cFishing\u76ee\u5f55\uff1a" + fv3 + (flag3 ? "\u6210\u529f" : "\u5931\u8d25"));
                    }
                }
            }
            final File cpFile = new File(this.jdbConfig.getLocalDir() + "/Lottery/");
            if (cpFile != null && cpFile.listFiles() != null) {
                for (final File file4 : cpFile.listFiles()) {
                    final int fv4 = new Integer(file4.getName());
                    if (fv4 < dateymd) {
                        final boolean flag4 = this.deleteDir(file4);
                        JdbTaskJob.logger.info("\u5220\u9664jdb\u5f69\u7968Lottery\u76ee\u5f55\uff1a" + fv4 + (flag4 ? "\u6210\u529f" : "\u5931\u8d25"));
                    }
                }
            }
            final File lhjFile = new File(this.jdbConfig.getLocalDir() + "/Slot/");
            if (lhjFile != null && lhjFile.listFiles() != null) {
                for (final File file5 : lhjFile.listFiles()) {
                    final int fv5 = new Integer(file5.getName());
                    if (fv5 < dateymd) {
                        final boolean flag5 = this.deleteDir(file5);
                        JdbTaskJob.logger.info("\u5220\u9664jdb\u8001\u864e\u673aSlot\u76ee\u5f55\uff1a" + fv5 + (flag5 ? "\u6210\u529f" : "\u5931\u8d25"));
                    }
                }
            }
        }
        catch (Exception ex) {
            JdbTaskJob.logger.error("deleteJdbDir:", (Throwable)ex);
        }
        JdbTaskJob.logger.info("\u5220\u9664\u7f8e\u4e1c\u65f6\u95f43\u5929\u524dJDB\u4e0b\u8f7d\u76ee\u5f55\u7ed3\u675f\uff0c\u8017\u65f6:" + (System.currentTimeMillis() - startTime));
    }
    
    private String getStratDate() {
        final Date startDate = this.liveWorkInfoService.getLastTime(LiveGame.JDB.getCode());
        final Calendar calendar = Calendar.getInstance();
        if (startDate == null) {
            calendar.add(11, -12);
            calendar.add(12, -20);
        }
        else {
            calendar.setTime(startDate);
            calendar.add(12, -20);
        }
        return DateUtil.dateToStr(calendar.getTime(), "yyyyMMdd");
    }
    
    public static List<String> getFileNameList() {
        final List<String> list = new ArrayList<String>();
        final DecimalFormat df = new DecimalFormat("00");
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final Calendar calendar = Calendar.getInstance();
        calendar.add(11, -12);
        final String startDate = sdf.format(calendar.getTime());
        String endDate = sdf.format(calendar.getTime());
        int startHour = 0;
        int startMinute = 0;
        int endHour = 0;
        int endMinute = 5;
        for (int i = 0; i < 24; ++i) {
            if (i <= calendar.getTime().getHours()) {
                for (int j = 0; j < 12; ++j) {
                    if (startMinute == 60) {
                        ++startHour;
                        startMinute = 0;
                    }
                    if (endMinute == 60) {
                        ++endHour;
                        endMinute = 0;
                    }
                    if (endHour == 24) {
                        calendar.add(5, 1);
                        endDate = sdf.format(calendar.getTime());
                        endHour = 0;
                    }
                    list.add(startDate + df.format(startHour) + df.format(startMinute) + "_" + endDate + df.format(endHour) + df.format(endMinute) + ".zip");
                    startMinute += 5;
                    endMinute += 5;
                }
            }
        }
        return list;
    }
    
    public static void main(final String[] args) {
        try {
            final JdbFtpUtil ftp = new JdbFtpUtil();
            ftp.createConnect("203.73.176.196", 2121, "tgeag@TGE", "OdBC8ttgeag");
            final List<String> list = (List<String>)ftp.download("/Fishing/20180903", "C:\\Users\\THINKPAD\\Desktop\\\u771f\u4eba\\JDB\\Fishing");
            for (final String filePath : list) {
                final ZipFile zf = new ZipFile(filePath);
                final InputStream in = new BufferedInputStream(new FileInputStream(filePath));
                final ZipInputStream zin = new ZipInputStream(in);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        final BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze), "utf-8"));
                        String line;
                        while ((line = br.readLine()) != null) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("{\"status\":\"0000\",").append("\"data\":").append(line).append("}");
                            final JdbBetRecordRep betRep = (JdbBetRecordRep)JSON.parseObject(sb.toString(), (Class)JdbBetRecordRep.class);
                            System.out.println(betRep.toString());
                        }
                        br.close();
                    }
                    zin.closeEntry();
                }
                zf.close();
                in.close();
                zin.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)JdbTaskJob.class);
        queue = new RequestApiQueue1();
    }
}
