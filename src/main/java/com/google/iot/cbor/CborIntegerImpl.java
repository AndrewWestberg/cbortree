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
    private final CborByteString mByteString;

    @Override
    public int getTag() {
        return mTag;
    }

    CborIntegerImpl(BigInteger value, int tag, @Nullable Integer majorType, @Nullable Byte additionalInfo) {
        if (!CborTag.isValid(tag)) {
            throw new IllegalArgumentException("Invalid tag value " + tag);
        }

        if(value.compareTo(CborInteger.BI_MAX_8B) > 0) {
            // positive bignum stored as byte string
            byte[] bytes = value.toByteArray();
            mByteString = CborByteString.create(bytes, 0, bytes.length, CborTag.BIGNUM_POS);
        } else if (value.compareTo(CborInteger.BI_MIN_8B) < 0) {
            // negative bignum stored as byte string
            byte[] bytes = value.negate().subtract(BigInteger.ONE).toByteArray();
            mByteString = CborByteString.create(bytes, 0, bytes.length, CborTag.BIGNUM_NEG);
        } else {
            mByteString = null;
        }

        mTag = tag;
        mValue = value;
        mMajorType = majorType;
        mAdditionalInfo = additionalInfo;
    }

    CborIntegerImpl(CborByteString bigNum) {
        if(bigNum.getTag() != 2 && bigNum.getTag() != 3) {
            throw new IllegalArgumentException("Invalid tag value " + bigNum.getTag());
        }
        mTag = CborTag.UNTAGGED;
        if(bigNum.getTag() == 2) {
            mValue = new BigInteger(bigNum.byteArrayValue()[0]);
        } else {
            mValue = BigInteger.valueOf(-1L).subtract(new BigInteger(bigNum.byteArrayValue()[0]));
        }
        mMajorType = null;
        mAdditionalInfo = null;
        mByteString = bigNum;
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
    public CborByteString byteString() {
        return mByteString;
    }

    @Override
    public int getAdditionalInformation() {
        if(mAdditionalInfo != null) return mAdditionalInfo;
        return super.getAdditionalInformation();
    }
}
