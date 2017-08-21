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
import android.os.Parcel;
import android.os.Parcelable;

import com.balch.android.app.framework.core.ColumnDescriptor;
import com.balch.android.app.framework.core.ExternalController;
import com.balch.android.app.framework.core.ValidatorException;
import com.balch.android.app.framework.core.widget.ControlMap;
import com.balch.android.app.framework.core.widget.EditLayout;

public class AccountEditController implements ExternalController<Account>, Parcelable {
    public AccountEditController() {
    }

    @Override
    public void onChanged(Context context, ColumnDescriptor descriptor, Object value, ControlMap controlMap) throws ValidatorException {
        if (descriptor.getField().getName().equals(Account.FLD_STRATEGY)) {
            Account.Strategy strategy = (Account.Strategy)value;
            if (strategy != Account.Strategy.NONE) {
                String [] displayVals = context.getResources().getStringArray(strategy.getListResId());
                EditLayout control = controlMap.get(Account.FLD_NAME);
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

    private AccountEditController(Parcel in) {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AccountEditController> CREATOR = new Creator<AccountEditController>() {
        @Override
        public AccountEditController createFromParcel(Parcel in) {
            return new AccountEditController(in);
        }

        @Override
        public AccountEditController[] newArray(int size) {
            return new AccountEditController[size];
        }
    };

}
