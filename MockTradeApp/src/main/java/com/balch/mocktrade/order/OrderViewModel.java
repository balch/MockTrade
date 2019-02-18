/*
 * Author: Balch
 * Created: 5/20/17 8:37 PM
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

package com.balch.mocktrade.order;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

class OrderViewModel extends ViewModel {

    private OrderModel orderModel;

    private final MutableLiveData<List<Order>> liveOrders = new MutableLiveData<>();

    private Disposable disposableLoadOrders = null;

    boolean isInitialized() {
        return (orderModel != null);
    }

    @Override
    protected void onCleared() {
        disposeOrders();
    }

    LiveData<List<Order>> getOrders() {
        return liveOrders;
    }

    void loadOrders(final long accountId) {
        disposeOrders();
        disposableLoadOrders = Observable.just(true)
                .subscribeOn(Schedulers.io())
                .map(aBoolean -> orderModel.getOpenOrders(accountId))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(liveOrders::setValue);
    }
    void setOrderModel(OrderModel orderModel) {
        this.orderModel = orderModel;
    }

    OrderModel getOrderModel() {
        return orderModel;
    }

    private void disposeOrders() {
        if (disposableLoadOrders != null) {
            disposableLoadOrders.dispose();
            disposableLoadOrders = null;
        }
    }
}


