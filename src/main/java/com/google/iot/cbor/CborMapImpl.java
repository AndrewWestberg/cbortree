/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.iot.cbor;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

final class CborMapImpl extends CborMap {
    // we use a list of map entries to preserve the order of the keys and to allow for duplicate keys in
    // non-canonical cbor.
    private final List<Map.Entry<CborObject, CborObject>> mMap;
    private int mTag;
    private boolean mIsIndefiniteLength;
    @Nullable
    private Integer mAdditionalInfo;


    CborMapImpl(int tag) {
        if (!CborTag.isValid(tag)) {
            throw new IllegalArgumentException("Invalid tag value " + tag);
        }

        mMap = new LinkedList<>();
        mTag = tag;
        mIsIndefiniteLength = false;
        mAdditionalInfo = null;
    }

    CborMapImpl(Map<CborObject, CborObject> map, int tag) {
        this(tag);
        mMap.addAll(map.entrySet());
        mIsIndefiniteLength = false;
        mAdditionalInfo = null;
    }

    CborMapImpl(@Nullable Map<CborObject, CborObject> map, int tag, boolean isIndefiniteLength, @Nullable Integer additionalInfo) {
        this(tag);
        if(map != null) {
            mMap.addAll(map.entrySet());
        }

        mIsIndefiniteLength = isIndefiniteLength;
        mAdditionalInfo = additionalInfo;
    }

    @Override
    public int getTag() {
        return mTag;
    }

    @Override
    public List<Map.Entry<CborObject, CborObject>> mapValue() {
        return mMap;
    }

    @Override
    public boolean isIndefiniteLength() {
        return mIsIndefiniteLength;
    }

    @Override
    public int getAdditionalInformation() {
        if(mAdditionalInfo == null) {
            return super.getAdditionalInformation();
        }
        return mAdditionalInfo;
    }
}
