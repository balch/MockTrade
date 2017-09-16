/*
 * Author: Balch
 * Created: 9/11/17 9:51 PM
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
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.joda.time.DateTime;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BarchartTypeAdapter implements JsonDeserializer<QuoteResult> {
    @Override
    public QuoteResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject status = json.getAsJsonObject().getAsJsonObject("status");

        List<Quote> quotes = new ArrayList<>();
        boolean isSuccess = (status.getAsJsonPrimitive("code").getAsInt() == 200);
        String errorMessage = status.getAsJsonPrimitive("message").getAsString();
        if (isSuccess) {
            JsonArray results = json.getAsJsonObject().getAsJsonArray("results");
            for (int x = 0; x < results.size(); x++) {
                JsonObject jsonQuote = results.get(x).getAsJsonObject();

                String symbol = jsonQuote.getAsJsonPrimitive("symbol").getAsString();
                String name = jsonQuote.getAsJsonPrimitive("name").getAsString();
                String exchange = jsonQuote.getAsJsonPrimitive("exchange").getAsString();
                Money price = new Money(jsonQuote.getAsJsonPrimitive("lastPrice").getAsDouble());
                Date lastTradeTime = DateTime.parse(jsonQuote.getAsJsonPrimitive("tradeTimestamp").getAsString()).toDate();
                Money previousClose = new Money(jsonQuote.getAsJsonPrimitive("close").getAsDouble());
                Money dividendPerShare = (jsonQuote.has("dividendYieldAnnual") && !jsonQuote.get("dividendYieldAnnual").isJsonNull()) ?
                        new Money(jsonQuote.getAsJsonPrimitive("dividendYieldAnnual").getAsDouble()) :
                        new Money();

                quotes.add(new Quote(symbol, name, exchange, price, lastTradeTime, previousClose, dividendPerShare));
            }
        }

        return new QuoteResult(isSuccess, quotes, errorMessage);
    }
}
