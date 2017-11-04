/*
 * Author: Balch
 * Created: 11/4/17 10:47 AM
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
 * Copyright (C) 2017
 *
 */

package com.balch.mocktrade.finance;

import com.balch.android.app.framework.types.Money;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IEXQuoteTypeAdapter implements JsonDeserializer<QuoteResult> {
    @Override
    public QuoteResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        List<Quote> quotes = new ArrayList<>();

        Set<Map.Entry<String, JsonElement>> jsonQuotes = json.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry: jsonQuotes) {
            String symbol = entry.getKey();
            JsonObject jsonQuote = entry.getValue().getAsJsonObject().getAsJsonObject("quote");

            String name = jsonQuote.getAsJsonPrimitive("companyName").getAsString();
            String exchange = jsonQuote.getAsJsonPrimitive("primaryExchange").getAsString();
            Money price = new Money(jsonQuote.getAsJsonPrimitive("latestPrice").getAsDouble());
            Date lastTradeTime = new Date(jsonQuote.getAsJsonPrimitive("latestUpdate").getAsLong());
            Money previousClose = new Money(jsonQuote.getAsJsonPrimitive("previousClose").getAsDouble());

            JsonObject jsonStats = entry.getValue().getAsJsonObject().getAsJsonObject("stats");
            Money dividendPerShare = (jsonStats.has("dividendYield") && !jsonStats.get("dividendYield").isJsonNull()) ?
                    new Money(jsonStats.getAsJsonPrimitive("dividendYield").getAsDouble()) :
                    new Money();

            quotes.add(new Quote(symbol, name, exchange, price, lastTradeTime, previousClose, dividendPerShare));

        }
        return new QuoteResult(true, quotes);
    }
}
