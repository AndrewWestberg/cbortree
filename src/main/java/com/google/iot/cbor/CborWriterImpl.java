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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.google.iot.cbor.CborObject.ADDITIONAL_INFO_EXTRA_INDEF;

class CborWriterImpl implements CborWriter {
    private static final byte BREAK = (byte) 0xFF;

    private final EncoderStream mEncoderStream;

    CborWriterImpl(OutputStream outputStream) {
        mEncoderStream = EncoderStream.create(outputStream);
    }

    CborWriterImpl(ByteBuffer byteBuffer) {
        mEncoderStream = EncoderStream.create(byteBuffer);
    }

    private CborWriterImpl() {
        mEncoderStream = EncoderStream.create();
    }

    static int length(CborObject obj) {
        int ret = 0;

        try {
            ret = new CborWriterImpl().writeDataItem(obj).mEncoderStream.length();
        } catch (IOException ignored) {
            // This will never get thrown
        }

        return ret;
    }

    private void writeCborHeader(int majorType, int val) throws IOException {
        mEncoderStream.put((byte) ((majorType << 5) + (val & 0x1F)));
    }

    private void writeCborFullInteger(int majorType, BigInteger val) throws IOException {
        if (val.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("val cannot be negative");
        }

        final int ai = CborInteger.calcAdditionalInformation(val);

        writeCborHeader(majorType, ai);

        switch (ai) {
            case CborObject.ADDITIONAL_INFO_EXTRA_1B:
                mEncoderStream.put(val.byteValue());
                break;

            case CborObject.ADDITIONAL_INFO_EXTRA_2B:
                mEncoderStream.putShort(val.shortValue());
                break;

            case CborObject.ADDITIONAL_INFO_EXTRA_4B:
                mEncoderStream.putInt(val.intValue());
                break;

            case CborObject.ADDITIONAL_INFO_EXTRA_8B:
                mEncoderStream.putLong(val.longValue());
                break;
        }
    }

    @Override
    @CanIgnoreReturnValue
    public CborWriter writeTag(int tag) throws IOException {
        if (tag != CborTag.UNTAGGED) {
            writeCborFullInteger(CborMajorType.TAG, BigInteger.valueOf(tag));
        }
        return this;
    }

    @Override
    @CanIgnoreReturnValue
    public CborWriterImpl writeDataItem(CborObject obj) throws IOException {
        writeTag(obj.getTag());

        if (obj instanceof CborArray) {
            return writeDataItem((CborArray) obj);
        } else if (obj instanceof CborFloat) {
            return writeDataItem((CborFloat) obj);
        } else if (obj instanceof CborInteger) {
            return writeDataItem((CborInteger) obj);
        } else if (obj instanceof CborMap) {
            return writeDataItem((CborMap) obj);
        } else if (obj instanceof CborTextString) {
            return writeDataItem((CborTextString) obj);
        } else if (obj instanceof CborByteString) {
            return writeDataItem((CborByteString) obj);
        } else if (obj instanceof CborSimple) {
            return writeDataItem((CborSimple) obj);
        } else {
            throw new CborRuntimeException(
                    "Can't encode \"" + obj + "\" of type " + obj.getClass());
        }
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborArray array) throws IOException {
        if(array.isIndefiniteLength()) {
            writeCborHeader(array.getMajorType(), ADDITIONAL_INFO_EXTRA_INDEF);
        } else {
            writeCborFullInteger(array.getMajorType(), BigInteger.valueOf(array.size()));
        }
        for (CborObject obj : array) {
            writeDataItem(obj);
        }
        if(array.isIndefiniteLength()) {
            mEncoderStream.put(BREAK);
        }
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborFloat obj) throws IOException {
        writeCborHeader(obj.getMajorType(), obj.getAdditionalInformation());

        switch (obj.getAdditionalInformation()) {
            case CborFloat.TYPE_HALF:
                mEncoderStream.putHalf(obj.floatValue());
                break;
            case CborFloat.TYPE_FLOAT:
                mEncoderStream.putFloat(obj.floatValue());
                break;
            case CborFloat.TYPE_DOUBLE:
                mEncoderStream.putDouble(obj.floatValue());
                break;
        }

        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborInteger obj) throws IOException {
        BigInteger val = obj.bigIntegerValue();

        if (val.compareTo(BigInteger.ZERO) < 0) {
            val = val.negate().subtract(BigInteger.ONE);
        }

        writeCborFullInteger(obj.getMajorType(), val);
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborMap map) throws IOException {
        writeCborFullInteger(map.getMajorType(), BigInteger.valueOf(map.mapValue().size()));
        for (Map.Entry<CborObject, CborObject> entry : map.mapValue().entrySet()) {
            writeDataItem(entry.getKey());
            writeDataItem(entry.getValue());
        }
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborSimple obj) throws IOException {
        writeCborFullInteger(obj.getMajorType(), BigInteger.valueOf(obj.getValue()));
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborByteString obj) throws IOException {
        writeCborFullInteger(obj.getMajorType(), BigInteger.valueOf(obj.byteArrayValue().length));
        mEncoderStream.put(obj.byteArrayValue());
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborTextString obj) throws IOException {
        writeCborFullInteger(obj.getMajorType(), BigInteger.valueOf(obj.byteArrayValue().length));
        mEncoderStream.put(obj.byteArrayValue());
        return this;
    }
}
