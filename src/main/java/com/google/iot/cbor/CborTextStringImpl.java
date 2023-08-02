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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class CborTextStringImpl extends CborTextString {
    private final String mValue;
    private final byte[][] mByteValue;
    private final int mTag;
    private final boolean mIsIndefiniteLength;

    @Override
    public int getTag() {
        return mTag;
    }

    CborTextStringImpl(String value, int tag) {
        if (!CborTag.isValid(tag)) {
            throw new IllegalArgumentException("Invalid tag value " + tag);
        }

        mTag = tag;
        mValue = value;
        mByteValue = new byte[1][];
        mByteValue[0] = mValue.getBytes(StandardCharsets.UTF_8);
        mIsIndefiniteLength = false;
    }

    CborTextStringImpl(byte[][] array, int[] offset, int[] length, int tag, boolean isIndefiniteLength) {
        if (!CborTag.isValid(tag)) {
            throw new IllegalArgumentException("Invalid tag value " + tag);
        }

        mTag = tag;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mByteValue = new byte[array.length][];
        for (int i = 0; i < array.length; i++) {
            mByteValue[i] = Arrays.copyOfRange(array[i], offset[i], offset[i] + length[i]);
            try {
                baos.write(mByteValue[i]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        mValue = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        mIsIndefiniteLength = isIndefiniteLength;
    }

    @Override
    public String stringValue() {
        return mValue;
    }

    @Override
    public byte[][] byteArrayValue() {
        return mByteValue;
    }

    @Override
    public boolean isIndefiniteLength() {
        return mIsIndefiniteLength;
    }
}
