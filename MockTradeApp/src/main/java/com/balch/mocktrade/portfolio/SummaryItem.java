package com.balch.mocktrade.portfolio;

import com.balch.android.app.framework.domain.DomainObject;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.account.Account;

import java.util.Date;

public class SummaryItem extends DomainObject {
    private static final String TAG = SummaryItem.class.getSimpleName();

    protected Account account;
    protected String symbol;
    protected Money costBasis;
    protected Money price;
    protected Date tradeTime;
    protected Long quantity;

    public SummaryItem() {
    }

    public SummaryItem(Account account, String symbol, Money costBasis, Money price, Date tradeTime, Long quantity) {
        this.account = account;
        this.symbol = symbol;
        this.costBasis = costBasis;
        this.price = price;
        this.tradeTime = tradeTime;
        this.quantity = quantity;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(Money costBasis) {
        this.costBasis = costBasis;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public Date getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
