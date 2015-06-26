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

package com.balch.android.app.framework.model;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class ModelFactory {
    private static final String TAG = ModelFactory.class.getSimpleName();

    private final Map<ModelSourceProvider, Map<String, Class<?>>> modelMap;
    private final Map<Class<?>, ModelSourceProvider> defaultModelMap;

    private final DefaultSourceProvider defaultSourceProvider = new DefaultSourceProvider();

    public ModelFactory() {
        this.modelMap = new HashMap<ModelSourceProvider, Map<String, Class<?>>>(32);
        this.defaultModelMap = new HashMap<Class<?>, ModelSourceProvider>(32);
    }

    public <T extends BaseModel> T getModel(Class<? extends BaseModel> clazz) {
        return getModel(defaultSourceProvider, clazz);
    }

    public <T extends BaseModel> T getModel(ModelSourceProvider sourceProvider, Class<? extends BaseModel> clazz) {
        T model;

        if (sourceProvider == defaultSourceProvider) {
            sourceProvider = this.defaultModelMap.get(clazz);
        }

        Map<String, Class<?>> parentToModelMap = modelMap.get(sourceProvider);
        if (parentToModelMap == null) {
            String msg = "No Registered classes for Source="+ sourceProvider;
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }

        Class<?> modelClazz = parentToModelMap.get(clazz.getName());
        if (modelClazz == null) {
            String msg = "Class Not Registered Source="+ sourceProvider + "clazz="+clazz.getName();
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }

        try {
            model = (T)modelClazz.getConstructor().newInstance();
        } catch (Exception e) {
            String msg = "Error constructing class="+ sourceProvider + "clazz="+modelClazz.getName();
            Log.e(TAG, msg);
            throw new RuntimeException(msg, e);
        }

        sourceProvider.initialize((ModelInitializer)model);

        return model;
    }

    public void registerModel(Class<?> baseModelClazz, Class<?> modelClazz, ModelSourceProvider sourceProvider, boolean isDefault) {

        if (sourceProvider == defaultSourceProvider) {
            String msg = "Source.Default is invalid for "+modelClazz.getName();
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }

        if (isDefault || !this.defaultModelMap.containsKey(baseModelClazz)) {
            this.defaultModelMap.put(baseModelClazz, sourceProvider);
        }

        // make sure modelClazz implements the source provider interface
        Class<?> parent = null;
        Class<?>  superClazz = modelClazz.getSuperclass();
        while (superClazz != null) {
            if (superClazz.getName().equals(sourceProvider.getBaseInterface().getName())) {
                parent = superClazz;
                break;
            }
            superClazz = modelClazz.getSuperclass();
        }

        if (parent == null) {
            String msg = "Invalid class hierarchy "+modelClazz.getName() + " need to extend an interface the extends "+ sourceProvider.getBaseInterface().getName();
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }

        Map<String, Class<?>> parentToModelMap = modelMap.get(sourceProvider);
        if (parentToModelMap == null) {
            parentToModelMap = new HashMap<String, Class<?>>();
            modelMap.put(sourceProvider, parentToModelMap);
        }

        parentToModelMap.put(baseModelClazz.getName(), modelClazz);
    }
}
