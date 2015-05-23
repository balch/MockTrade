package com.balch.android.app.framework.domain;

import com.balch.android.app.framework.types.ISO8601DateTime;

import java.io.Serializable;

public abstract class DomainObject implements Serializable {
    protected Long id;
    protected ISO8601DateTime createTime;
    protected ISO8601DateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ISO8601DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(ISO8601DateTime createTime) {
        this.createTime = createTime;
    }

    public ISO8601DateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(ISO8601DateTime updateTime) {
        this.updateTime = updateTime;
    }

}
