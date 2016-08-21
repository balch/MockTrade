package com.balch.android.app.framework.domain;

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
        id = (Long)in.readSerializable();
        createTime = (Date)in.readSerializable();
        updateTime = (Date)in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(id);
        dest.writeSerializable(createTime);
        dest.writeSerializable(updateTime);
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

}
