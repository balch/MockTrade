/*
 * Author: Balch
 * Created: 9/4/14 12:26 AM
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

package com.balch.mocktrade.investment;

import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.util.List;

public class InvestmentSqliteModel extends SqliteModel {

    public InvestmentSqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
    }

    protected List<Investment> getInvestments(Long accountId) {
        try {
            String where = null;
            String [] whereArgs = null;
            if (accountId != null) {
                where = Investment.COLUMN_ACCOUNT_ID + " = ?";
                whereArgs = new String[]{accountId.toString()};
            }
            return this.getSqlConnection().query(Investment.class, where, whereArgs, Investment.COLUMN_SYMBOL + " COLLATE NOCASE");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Investment> getAllInvestments() {
        return this.getInvestments(null);
    }

    public Investment getInvestmentBySymbol(String symbol, Long accountId) {
        try {
            String where = String.format("%s = ? AND %s = ?", Investment.COLUMN_SYMBOL, Investment.COLUMN_ACCOUNT_ID);
            String [] whereArgs = new String[]{symbol, accountId.toString()};
            List<Investment> investments = this.getSqlConnection().query(Investment.class, where, whereArgs, null);

            return (investments.size() == 1) ? investments.get(0) : null;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updateInvestment(Investment investment) {
        try {
            return this.getSqlConnection().update(investment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
