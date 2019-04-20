package com.cz.gameplat.live.core.dto.ds;

public class SbRecordReport2 {

    private Integer errcode;

    private String startdate;

    private String enddate;

    private Integer page_num;

    private Integer page_size;

    private Integer total_row;

    private Integer total_page;

    private SbRecordReportItemList2 list;

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    public Integer getPage_num() {
        return page_num;
    }

    public void setPage_num(Integer page_num) {
        this.page_num = page_num;
    }

    public Integer getPage_size() {
        return page_size;
    }

    public void setPage_size(Integer page_size) {
        this.page_size = page_size;
    }

    public Integer getTotal_row() {
        return total_row;
    }

    public void setTotal_row(Integer total_row) {
        this.total_row = total_row;
    }

    public Integer getTotal_page() {
        return total_page;
    }

    public void setTotal_page(Integer total_page) {
        this.total_page = total_page;
    }

    public SbRecordReportItemList2 getList() {
        return list;
    }

    public void setList(SbRecordReportItemList2 list) {
        this.list = list;
    }
}
