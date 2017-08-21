package com.balch.android.app.framework.core;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public abstract class DomainObject implements Parcelable {
    protected Long id;
    protected Date createTime;
    protected Date updateTime;

    protected DomainObject() {
    }

    protected DomainObject(Parcel in) {
        long val = in.readLong();
        id = (val != -1) ? val : null;
        createTime = readDate(in);
        updateTime = readDate(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong((id != null) ? id : -1);
        writeDate(dest, createTime);
        writeDate(dest, updateTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    protected void writeDate(Parcel parcel, Date date) {
        long time = (date != null) ? date.getTime() : -1;
        parcel.writeLong(time);
    }

    protected Date readDate(Parcel parcel) {
        long time = parcel.readLong();
        return (time != -1) ? new Date(time) : null;
    }
}
