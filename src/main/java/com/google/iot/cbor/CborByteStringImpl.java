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

import it.unimi.dsi.fastutil.BigArrays;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CborByteStringImpl extends CborByteString {
    private final byte[][] mByteValue;
    private final int mTag;
    private final boolean mIsIndefiniteLength;
    @Nullable
    private final Integer mAdditionalInfo;


    @Override
    public int getTag() {
        return mTag;
    }

    CborByteStringImpl(byte[][] array, long offset, long length, int tag, @Nullable Integer additionalInfo) {
        mTag = tag;
        mByteValue = BigArrays.copy(array, offset, length);
        mIsIndefiniteLength = false;
        mAdditionalInfo = additionalInfo;
    }

    CborByteStringImpl(byte[][] array, int tag, boolean isIndefiniteLength, @Nullable Integer additionalInfo) {
        // simple wrap
        mTag = tag;
        mByteValue = array;
        mIsIndefiniteLength = isIndefiniteLength;
        mAdditionalInfo = additionalInfo;
    }

    @Override
    public byte[][] byteArrayValue() {
        return mByteValue;
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
