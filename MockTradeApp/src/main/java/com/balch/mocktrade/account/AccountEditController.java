/*
 * Author: Balch
 * Created: 9/4/14 7:00 AM
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

package com.balch.mocktrade.account;

import android.content.Context;

import com.balch.android.app.framework.domain.ColumnDescriptor;
import com.balch.android.app.framework.domain.ExternalController;
import com.balch.android.app.framework.domain.ValidatorException;
import com.balch.android.app.framework.domain.controls.ControlMap;
import com.balch.android.app.framework.domain.controls.EditControl;

public class AccountEditController implements ExternalController<Account> {
    @Override
    public void onChanged(Context context, ColumnDescriptor descriptor, Object value, ControlMap controlMap) throws ValidatorException {
        if (descriptor.getField().getName().equals(Account.FLD_STRATEGY)) {
            Account.Strategy strategy = (Account.Strategy)value;
            if (strategy != Account.Strategy.NONE) {
                String [] displayVals = context.getResources().getStringArray(strategy.getListResId());
                EditControl control = controlMap.get(Account.FLD_NAME);
                control.setValue(displayVals[strategy.ordinal()]);
            }
        }
    }

    @Override
    public void validate(Context context, Account account, ControlMap controlMap) throws ValidatorException {
    }

    @Override
    public void initialize(Context context, Account account, ControlMap controlMap) {
    }

}
