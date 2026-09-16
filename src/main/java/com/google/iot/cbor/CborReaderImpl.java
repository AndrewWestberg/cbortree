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
import it.unimi.dsi.fastutil.bytes.ByteBigArrays;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

class CborReaderImpl implements CborReader {
    private static final boolean DEBUG = true;
    private static final Logger LOGGER = Logger.getLogger(CborReader.class.getCanonicalName());

    static final int UNSPECIFIED = -1;
    private static final byte BREAK = (byte) 0xFF;

    private final DecoderStream mDecoderStream;
    private int mRemainingObjects;

    CborReaderImpl(InputStream inputStream, int objectCount) {
        mDecoderStream = DecoderStream.create(inputStream);
        mRemainingObjects = objectCount;
    }

    CborReaderImpl(byte[] bytes, int offset, int objectCount) {
        this(new ByteArrayInputStream(bytes, offset, bytes.length - offset), objectCount);
        if (offset >= bytes.length) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public boolean hasRemainingDataItems() {
        try {
            if (mRemainingObjects < 0) {
                return mDecoderStream.hasRemaining() && (mDecoderStream.peek() != BREAK);
            }
            return mRemainingObjects != 0;
        } catch (EOFException x) {
            return false;
        } catch (IOException x) {
            // x.printStackTrace();
            // We say true here so that we will call readDataItem() and get the exception
            return true;
        }
    }

    @Override
    public long bytesParsed() {
        return mDecoderStream.bytesParsed();
    }

    @Override
    public CborObject readDataItem() throws CborParseException, IOException {
        if (!hasRemainingDataItems()) {
            throw new NoSuchElementException();
        }

        ArrayDeque<Frame> frames = new ArrayDeque<>();
        int pendingTag = CborTag.UNTAGGED;
        boolean tagPending = false;

        try {
            while (true) {
                CborObject completed;
                byte firstByte = mDecoderStream.get();

                if (firstByte == BREAK) {
                    if (tagPending || frames.isEmpty()) {
                        throw new CborParseException("CBOR data is truncated or corrupt");
                    }

                    Frame frame = frames.peek();
                    if (!frame.indefinite || frame.pendingKey != null) {
                        throw new CborParseException("CBOR data is truncated or corrupt");
                    }
                    frames.pop();
                    completed = frame.finish();

                } else {
                    int majorType = ((firstByte & 0xFF) >> 5);
                    byte additionalInfo = (byte) (firstByte & 0x1F);
                    BigInteger additionalData;

                    if (additionalInfo < CborObject.ADDITIONAL_INFO_EXTRA_1B) {
                        additionalData = BigInteger.valueOf(additionalInfo);
                    } else if (additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_1B) {
                        additionalData = BigInteger.valueOf(mDecoderStream.get() & 0xFF);
                    } else if (additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_2B) {
                        additionalData = BigInteger.valueOf(mDecoderStream.getShort() & 0xFFFF);
                    } else if (additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_4B) {
                        additionalData = BigInteger.valueOf(mDecoderStream.getInt() & 0xFFFFFFFFL);
                    } else if (additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_8B) {
                        additionalData =
                                new BigInteger(Long.toUnsignedString(mDecoderStream.getLong()));
                    } else if (additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_INDEF) {
                        additionalData = BigInteger.valueOf(UNSPECIFIED);
                    } else {
                        throw new CborParseException(
                                "Undefined additional info value "
                                        + additionalInfo
                                        + " for major type "
                                        + majorType);
                    }

                    if (majorType == CborMajorType.TAG) {
                        tagPending = true;
                        pendingTag = CborTag.UNTAGGED;
                        if (CborTag.isValid(additionalData.longValue())) {
                            pendingTag = (int) additionalData.longValue();
                        } else {
                            LOGGER.warning("Ignoring invalid tag: " + additionalData);
                        }
                        continue;
                    }

                    int tag = pendingTag;
                    pendingTag = CborTag.UNTAGGED;
                    tagPending = false;

                    switch (majorType) {
                        case CborMajorType.POS_INTEGER:
                            completed =
                                    CborInteger.create(
                                            additionalData,
                                            tag,
                                            CborMajorType.POS_INTEGER,
                                            additionalInfo);
                            break;

                        case CborMajorType.NEG_INTEGER:
                            completed =
                                    CborInteger.create(
                                            BigInteger.valueOf(-1L).subtract(additionalData),
                                            tag,
                                            CborMajorType.NEG_INTEGER,
                                            additionalInfo);
                            break;

                        case CborMajorType.BYTE_STRING:
                        case CborMajorType.TEXT_STRING:
                        case CborMajorType.ARRAY:
                        case CborMajorType.MAP:
                            if (additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_INDEF
                                    || majorType == CborMajorType.ARRAY
                                    || majorType == CborMajorType.MAP) {
                                Frame frame =
                                        new Frame(majorType, additionalData, tag, additionalInfo);
                                if (frame.isComplete()) {
                                    completed = frame.finish();
                                } else {
                                    frames.push(frame);
                                    continue;
                                }
                            } else if (majorType == CborMajorType.BYTE_STRING) {
                                CborByteString byteString;
                                if (BigInteger.valueOf(additionalData.intValue())
                                        .equals(additionalData)) {
                                    byte[] bytes = new byte[additionalData.intValue()];
                                    mDecoderStream.get(bytes);
                                    byteString =
                                            CborByteString.wrap(
                                                    BigArrays.wrap(bytes),
                                                    tag,
                                                    false,
                                                    (int) additionalInfo);
                                } else {
                                    byte[][] bytes =
                                            ByteBigArrays.newBigArray(additionalData.longValue());
                                    mDecoderStream.get(bytes);
                                    byteString =
                                            CborByteString.wrap(
                                                    bytes, tag, false, (int) additionalInfo);
                                }
                                completed =
                                        tag == CborTag.BIGNUM_POS || tag == CborTag.BIGNUM_NEG
                                                ? CborInteger.create(byteString)
                                                : byteString;
                            } else {
                                byte[] bytes = new byte[additionalData.intValue()];
                                mDecoderStream.get(bytes);
                                completed =
                                        CborTextString.create(
                                                bytes,
                                                0,
                                                bytes.length,
                                                tag,
                                                false,
                                                (int) additionalInfo);
                            }
                            break;

                        case CborMajorType.OTHER:
                            if (additionalInfo == CborFloat.TYPE_HALF) {
                                completed =
                                        CborFloat.createHalf(
                                                Half.shortBitsToFloat(additionalData.shortValue()),
                                                tag);
                            } else if (additionalInfo == CborFloat.TYPE_FLOAT) {
                                completed =
                                        CborFloat.create(
                                                Float.intBitsToFloat(additionalData.intValue()), tag);
                            } else if (additionalInfo == CborFloat.TYPE_DOUBLE) {
                                completed =
                                        CborFloat.create(
                                                Double.longBitsToDouble(additionalData.longValue()),
                                                tag);
                            } else {
                                completed = CborSimple.create(additionalData.intValue(), tag);
                            }
                            break;

                        default:
                            throw new CborParseException("Invalid major type value " + majorType);
                    }
                }

                while (true) {
                    if (frames.isEmpty()) {
                        if (mRemainingObjects != UNSPECIFIED) {
                            mRemainingObjects--;
                        }
                        return completed;
                    }

                    Frame parent = frames.peek();
                    parent.accept(completed);
                    if (!parent.isComplete()) {
                        break;
                    }
                    frames.pop();
                    completed = parent.finish();
                }
            }
        } catch (EOFException
                | BufferUnderflowException
                | NoSuchElementException
                | IllegalArgumentException x) {
            throw new CborParseException("CBOR data is truncated or corrupt", x);
        }
    }

    private static final class Frame {
        final int majorType;
        final int tag;
        final byte additionalInfo;
        final boolean indefinite;
        long remaining;
        final CborArray array;
        final CborMap map;
        final ArrayList<byte[]> chunks;
        CborObject pendingKey;

        Frame(int majorType, BigInteger additionalData, int tag, byte additionalInfo) {
            this.majorType = majorType;
            this.tag = tag;
            this.additionalInfo = additionalInfo;
            indefinite = additionalInfo == CborObject.ADDITIONAL_INFO_EXTRA_INDEF;
            remaining = additionalData.longValue();
            array =
                    majorType == CborMajorType.ARRAY
                            ? CborArray.create(
                                    null, tag, indefinite, indefinite ? null : (int) additionalInfo)
                            : null;
            map =
                    majorType == CborMajorType.MAP
                            ? CborMap.create(
                                    null, tag, indefinite, indefinite ? null : (int) additionalInfo)
                            : null;
            chunks =
                    majorType == CborMajorType.BYTE_STRING
                                    || majorType == CborMajorType.TEXT_STRING
                            ? new ArrayList<>()
                            : null;
        }

        void accept(CborObject child) throws CborParseException {
            if (majorType == CborMajorType.ARRAY) {
                array.add(child);
                if (!indefinite) {
                    remaining--;
                }
            } else if (majorType == CborMajorType.MAP) {
                if (pendingKey == null) {
                    pendingKey = child;
                } else {
                    map.mapValue().add(new AbstractMap.SimpleEntry<>(pendingKey, child));
                    pendingKey = null;
                    if (!indefinite) {
                        remaining--;
                    }
                }
            } else if (majorType == CborMajorType.BYTE_STRING
                    && child instanceof CborByteString
                    && child.getMajorType() == CborMajorType.BYTE_STRING) {
                chunks.addAll(Arrays.asList(((CborByteString) child).byteArrayValue()));
            } else if (majorType == CborMajorType.TEXT_STRING
                    && child instanceof CborTextString) {
                chunks.addAll(Arrays.asList(((CborTextString) child).byteArrayValue()));
            } else if (majorType == CborMajorType.BYTE_STRING) {
                throw new CborParseException("Unexpected major type in byte string stream");
            } else {
                throw new CborParseException("Unexpected major type in text string stream");
            }
        }

        boolean isComplete() {
            if (indefinite) {
                return false;
            }
            return remaining == 0 && pendingKey == null;
        }

        CborObject finish() {
            if (majorType == CborMajorType.ARRAY) {
                return array;
            }
            if (majorType == CborMajorType.MAP) {
                return map;
            }
            if (majorType == CborMajorType.BYTE_STRING) {
                CborByteString byteString =
                        CborByteString.wrap(chunks.toArray(new byte[0][]), tag, true, null);
                return tag == CborTag.BIGNUM_POS || tag == CborTag.BIGNUM_NEG
                        ? CborInteger.create(byteString)
                        : byteString;
            }

            byte[][] bytes = chunks.toArray(new byte[0][]);
            int[] offsets = new int[bytes.length];
            int[] lengths = new int[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                lengths[i] = bytes[i].length;
            }
            return CborTextString.create(bytes, offsets, lengths, tag, true, null);
        }
    }
}
