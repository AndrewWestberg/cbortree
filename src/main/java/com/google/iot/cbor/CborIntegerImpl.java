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

import javax.annotation.Nullable;
import java.math.BigInteger;

final class CborIntegerImpl extends CborInteger {
    private final BigInteger mValue;
    private final int mTag;
    private final Integer mMajorType;
    private final Byte mAdditionalInfo;

    @Override
    public int getTag() {
        return mTag;
    }

    CborIntegerImpl(BigInteger value, int tag, @Nullable Integer majorType, @Nullable Byte additionalInfo) {
        if (!CborTag.isValid(tag)) {
            throw new IllegalArgumentException("Invalid tag value " + tag);
        }

        mTag = tag;
        mValue = value;
        mMajorType = majorType;
        mAdditionalInfo = additionalInfo;
    }

    @Override
    public int getMajorType() {
        if(mMajorType != null) return mMajorType;
        return super.getMajorType();
    }

    @Override
    public long longValue() {
        return mValue.longValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return mValue;
    }

    @Override
    public int getAdditionalInformation() {
        if(mAdditionalInfo != null) return mAdditionalInfo;
        return super.getAdditionalInformation();
    }
}
