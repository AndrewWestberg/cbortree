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
import it.unimi.dsi.fastutil.BigArrays;

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
        writeCborFullInteger(majorType, val, CborInteger.calcAdditionalInformation(val));
    }

    private void writeCborFullInteger(int majorType, BigInteger val, int ai) throws IOException {
        if (val.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("val cannot be negative");
        }

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

        return switch (obj) {
            case CborArray cborObjects -> writeDataItem(cborObjects);
            case CborFloat cborFloat -> writeDataItem(cborFloat);
            case CborInteger cborInteger -> writeDataItem(cborInteger);
            case CborMap cborMap -> writeDataItem(cborMap);
            case CborTextString cborTextString -> writeDataItem(cborTextString);
            case CborByteString cborByteString -> writeDataItem(cborByteString);
            case CborSimple cborSimple -> writeDataItem(cborSimple);
            default -> throw new CborRuntimeException(
                    "Can't encode \"" + obj + "\" of type " + obj.getClass());
        };
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborArray array) throws IOException {
        if (array.isIndefiniteLength()) {
            writeCborHeader(array.getMajorType(), ADDITIONAL_INFO_EXTRA_INDEF);
        } else {
            writeCborFullInteger(array.getMajorType(), BigInteger.valueOf(array.size()), array.getAdditionalInformation());
        }
        for (CborObject obj : array) {
            writeDataItem(obj);
        }
        if (array.isIndefiniteLength()) {
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
        CborByteString byteString = obj.byteString();
        if(byteString != null) {
            // write the big integer as-is since it's bigger than 64 bits
            writeDataItem((CborObject) byteString);
        } else {
            BigInteger val = obj.bigIntegerValue();

            if(val.compareTo(CborInteger.BI_MAX_8B) > 0) {
                // write as a bignum positive byte string
                byte[] bytes = val.toByteArray();
                writeDataItem(CborByteString.create(bytes, 0, bytes.length, CborTag.BIGNUM_POS));
            } else if (val.compareTo(CborInteger.BI_MIN_8B) < 0){
                // write as a bignum negative byte string
                val = val.negate().subtract(BigInteger.ONE);
                byte[] bytes = val.toByteArray();
                writeDataItem(CborByteString.create(bytes, 0, bytes.length, CborTag.BIGNUM_NEG));
            } else {
                // write as a full integer
                if (val.compareTo(BigInteger.ZERO) < 0) {
                    val = val.negate().subtract(BigInteger.ONE);
                }
                writeCborFullInteger(obj.getMajorType(), val, obj.getAdditionalInformation());
            }
        }
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborMap map) throws IOException {
        if (map.isIndefiniteLength()) {
            writeCborHeader(map.getMajorType(), ADDITIONAL_INFO_EXTRA_INDEF);
        } else {
            writeCborFullInteger(map.getMajorType(), BigInteger.valueOf(map.mapValue().size()), map.getAdditionalInformation());
        }
        for (Map.Entry<CborObject, CborObject> entry : map.mapValue()) {
            writeDataItem(entry.getKey());
            writeDataItem(entry.getValue());
        }
        if (map.isIndefiniteLength()) {
            mEncoderStream.put(BREAK);
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
        if(obj.isIndefiniteLength()) {
            writeCborHeader(obj.getMajorType(), ADDITIONAL_INFO_EXTRA_INDEF);
            byte[][] chunks = obj.byteArrayValue();
            for (byte[] chunk : chunks) {
                writeCborFullInteger(CborMajorType.BYTE_STRING, BigInteger.valueOf(chunk.length));
                mEncoderStream.put(chunk);
            }
            mEncoderStream.put(BREAK);
        } else {
            writeCborFullInteger(obj.getMajorType(), BigInteger.valueOf(BigArrays.length(obj.byteArrayValue())), obj.getAdditionalInformation());
            mEncoderStream.put(obj.byteArrayValue());
        }
        return this;
    }

    @CanIgnoreReturnValue
    private CborWriterImpl writeDataItem(CborTextString obj) throws IOException {
        if(obj.isIndefiniteLength()) {
            writeCborHeader(obj.getMajorType(), ADDITIONAL_INFO_EXTRA_INDEF);
            byte[][] chunks = obj.byteArrayValue();
            for (byte[] chunk : chunks) {
                writeCborFullInteger(CborMajorType.TEXT_STRING, BigInteger.valueOf(chunk.length));
                mEncoderStream.put(chunk);
            }
            mEncoderStream.put(BREAK);
        } else {
            writeCborFullInteger(obj.getMajorType(), BigInteger.valueOf(obj.byteArrayValue()[0].length), obj.getAdditionalInformation());
            mEncoderStream.put(obj.byteArrayValue());
        }
        return this;
    }
}
