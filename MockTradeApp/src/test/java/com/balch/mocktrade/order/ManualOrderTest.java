/*
 * Author: Balch
 * Created: 9/6/14 1:11 PM
 *
 * This file is part of MockTrade.
 *
 * MockTrade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MockTrade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MockTrade.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2014
 */

package com.balch.mocktrade.order;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.finance.QuoteGeneric;
import com.balch.mocktrade.settings.Settings;

import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class ManualOrderTest {

    protected Quote quote;
    protected Order order;


    public ManualOrderTest(Order order, Quote quote) {
        this.quote = quote;
        this.order = order;
    }

    protected static Order createOrder(Order.OrderAction action, double limitPrice) {
        Order order = new Order();
        order.setStrategy(Order.OrderStrategy.MANUAL);
        order.setAction(action);
        order.setLimitPrice(new Money(limitPrice));
        return order;
    }

    protected static Quote createQuote(double price) {
        Quote quote = new QuoteGeneric();
        quote.setPrice(new Money(price));
        return quote;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {createOrder(Order.OrderAction.BUY, 25.00), createQuote(10.60)},
                {createOrder(Order.OrderAction.SELL, 35.00), createQuote(12.60)},
        });
    }

    @Test
    public void testOrder() throws Exception {
        Context context = mock(Context.class);
        FinanceModel financeModel = mock(FinanceModel.class);
        Settings settings = mock(Settings.class);
        OrderManager.OrderManagerListener listener = mock(OrderManager.OrderManagerListener.class);

        OrderManager orderManager = spy(new OrderManager(context, financeModel, settings, listener));
        doReturn(true).when(orderManager).isQuoteValid(any(Quote.class));

        orderManager.attemptExecuteOrder(order, quote);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<Quote> quoteCaptor = ArgumentCaptor.forClass(Quote.class);
        ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);

        verify(listener).executeOrder(orderCaptor.capture(), quoteCaptor.capture(), moneyCaptor.capture());

        assertEquals(this.order, orderCaptor.getValue());
        assertEquals(this.quote, quoteCaptor.getValue());
        assertEquals(0, order.getLimitPrice().compareTo(moneyCaptor.getValue()));
    }
}