/*
 * Author: Balch
 * Created: 9/15/14 7:01 PM
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

package com.balch.android.app.framework.types;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MoneyFormatTest {

    protected String expectedValue;
    protected Double value;
    protected int    places;


    public MoneyFormatTest(String expectedValue, Double value, int places) {
        this.expectedValue = expectedValue;
        this.value = value;
        this.places = places;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"$0.00", 0.0, 2},
                {"$0", 0.0, 0},
                {"$0.00", 0.0, 4},
                {"$0.12", 0.1234, 2},
                {"$0.1234", 0.1234, 4},
                {"$12.34", 12.34, 2},
                {"$12.34", 12.34, 4},
                {"$1,234.56", 1234.56, 2},
                {"$1,234.00", 1234.0, 2},
                {"$1,234,567.89", 1234567.89, 2},
                {"-$0.12", -0.1234, 2},
                {"-$0.1234", -0.1234, 4},
                {"-$12.34", -12.34, 2},
                {"-$12.34", -12.34, 4},
                {"-$1,234.56", -1234.56, 2},
                {"-$1,234.00", -1234.0, 2},
                {"-$1,234,567.89", -1234567.89, 2},
        });
    }

    @Test
    public void testMoneyFormat() throws Exception {
        Money money = new Money(this.value);

        String v = money.getCurrency(this.places);
        assertEquals(this.expectedValue, v);
    }
}