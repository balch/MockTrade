/*
 * Author: Balch
 * Created: 9/6/14 1:37 PM
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

import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.finance.FinanceModel;
import com.balch.mocktrade.finance.Quote;
import com.balch.mocktrade.finance.QuoteGeneric;
import com.balch.mocktrade.settings.Settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


@RunWith(Parameterized.class)
public class TrailingStopAmountOrderTest {
    private enum Outcome {
        SUCCESS,
        NO_ORDER,
        UNSUPPORTED,
        HIGHEST_PRICE_CHANGE
    }

    private Outcome outcome;
    private Quote quote;
    private Order order;

    public TrailingStopAmountOrderTest(Outcome outcome, Order order, Quote quote) {
        this.outcome = outcome;
        this.quote = quote;
        this.order = order;
    }

    private static Order createOrder(Order.OrderAction action, double stopPrice, double highestPrice) {
        Order order = new Order();
        order.setStrategy(Order.OrderStrategy.TRAILING_STOP_AMOUNT_CHANGE);
        order.setAction(action);
        order.setStopPrice(new Money(stopPrice));
        order.setHighestPrice(new Money(highestPrice));
        return order;
    }

    private static Quote createQuote(double price) {
        Quote quote = new QuoteGeneric();
        quote.setPrice(new Money(price));
        return quote;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {Outcome.UNSUPPORTED, createOrder(Order.OrderAction.BUY, 2.00, 10.0), createQuote(10.0)},
            {Outcome.SUCCESS, createOrder(Order.OrderAction.SELL, 2.0, 10.0), createQuote(8.0)},
            {Outcome.SUCCESS, createOrder(Order.OrderAction.SELL, 2.0, 10.0), createQuote(7.99)},
            {Outcome.NO_ORDER, createOrder(Order.OrderAction.SELL, 2.0, 10.0), createQuote(8.01)},
            {Outcome.NO_ORDER, createOrder(Order.OrderAction.SELL, 2.0, 10.0), createQuote(10.00)},
            {Outcome.HIGHEST_PRICE_CHANGE, createOrder(Order.OrderAction.SELL, 2.0, 10.0), createQuote(12.00)},
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
        doReturn(true).when(listener).updateOrder(any(Order.class));

        try {
            orderManager.attemptExecuteOrder(order, quote);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            ArgumentCaptor<Quote> quoteCaptor = ArgumentCaptor.forClass(Quote.class);
            ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);

            if (outcome == Outcome.SUCCESS) {
                verify(listener).executeOrder(orderCaptor.capture(), quoteCaptor.capture(), moneyCaptor.capture());

                assertEquals(this.order, orderCaptor.getValue());
                assertEquals(this.quote, quoteCaptor.getValue());
                assertEquals(0, quote.getPrice().compareTo(moneyCaptor.getValue()));
            } else if (outcome == Outcome.NO_ORDER) {
                verify(listener, never()).executeOrder(any(Order.class), any(Quote.class), any(Money.class));
            } else if (outcome == Outcome.HIGHEST_PRICE_CHANGE) {
                verify(listener, never()).executeOrder(any(Order.class), any(Quote.class), any(Money.class));
                verify(listener).updateOrder(orderCaptor.capture());
                assertEquals(this.order, orderCaptor.getValue());
                assertEquals(0, quote.getPrice().compareTo(order.getHighestPrice()));
            } else if (outcome == Outcome.UNSUPPORTED) {
                fail("Should have received and Unsupported Exceptions");
            }
        } catch (UnsupportedOperationException ex) {
            if (outcome != Outcome.UNSUPPORTED) {
                fail("Should NOT have received and Unsupported Exceptions");
            }
        }
    }
}