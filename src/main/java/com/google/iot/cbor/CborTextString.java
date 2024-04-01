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

import org.json.JSONObject;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

/** CBOR text string object interface. */
public abstract class CborTextString extends CborObject {
    // Prohibit users from subclassing for now.
    CborTextString() {}

    /**
     * Creates a new {@link CborTextString} instance from the given byte arrays, offsets, lengths, tag,
     * and indefinite length flag.
     * @param arrays the byte arrays
     * @param offsets the offsets
     * @param lengths the lengths
     * @param tag the tag
     * @param isIndefiniteLength the indefinite length flag
     * @return {@link CborTextString}
     */
    public static CborTextString create(byte[][] arrays, int[] offsets, int[] lengths, int tag, boolean isIndefiniteLength) {
        return new CborTextStringImpl(arrays, offsets, lengths, tag, isIndefiniteLength);
    }

    /**
     * Creates a new {@link CborTextString} instance from the given byte array, offset, length, tag, and
     * indefinite length flag.
     * @param array the byte array
     * @param offset the offset
     * @param length the length
     * @param tag the tag
     * @param isIndefiniteLength the indefinite length flag
     * @return {@link CborTextString}
     */
    public static CborTextString create(byte[] array, int offset, int length, int tag, boolean isIndefiniteLength) {
        byte[][] arrays = new byte[1][];
        arrays[0] = array;
        int[] offsets = new int[1];
        offsets[0] = offset;
        int[] lengths = new int[1];
        lengths[0] = length;
        return new CborTextStringImpl(arrays, offsets, lengths, tag, isIndefiniteLength);
    }

    /**
     * Creates a new {@link CborTextString} instance from the given byte array, offset, and length.
     * @param array the byte array
     * @param offset the offset
     * @param length the length
     * @return {@link CborTextString}
     */
    public static CborTextString create(byte[] array, int offset, int length) {
        byte[][] arrays = new byte[1][];
        arrays[0] = array;
        int[] offsets = new int[1];
        offsets[0] = offset;
        int[] lengths = new int[1];
        lengths[0] = length;
        return new CborTextStringImpl(arrays, offsets, lengths, CborTag.UNTAGGED, false);
    }

    /**
     * Creates a new {@link CborTextString} instance from the given byte array.
     * @param array the byte array
     * @return {@link CborTextString}
     */
    public static CborTextString create(byte[] array) {
        return create(array, 0, array.length);
    }

    /**
     * Creates a new {@link CborTextString} instance from the given string and tag.
     * @param string the string
     * @param tag the tag
     * @return {@link CborTextString}
     */
    public static CborTextString create(String string, int tag) {
        return new CborTextStringImpl(string, tag);
    }

    /**
     * Creates a new {@link CborTextString} instance from the given string.
     * @param string the string
     * @return {@link CborTextString}
     */
    public static CborTextString create(String string) {
        return new CborTextStringImpl(string, CborTag.UNTAGGED);
    }

    /**
     * Returns the string representation of this text string.
     * @return the string representation
     */
    public abstract String stringValue();

    /**
     * Returns the byte array representation of this text string. The returned array is a copy of the
     * internal representation, so it can be modified without affecting this object.
     * @return the byte array representation
     */
    public abstract byte[][] byteArrayValue();

    @Override
    public final int getMajorType() {
        return CborMajorType.TEXT_STRING;
    }

    @Override
    public int getAdditionalInformation() {
        return CborInteger.calcAdditionalInformation(BigInteger.valueOf(byteArrayValue().length));
    }

    /**
     * Is this text string of indefinite length?
     * @return {@code true} if this text string is of indefinite length, {@code false} otherwise
     */
    public abstract boolean isIndefiniteLength();

    @Override
    public final boolean isValidJson() {
        // CborTextStrings are always valid JSON.
        return true;
    }

    @Override
    public CborTextString copy() {
        // CborTextString instances are immutable, so we can simply return this.
        return this;
    }

    @Override
    public final String toJsonString() {
        return JSONObject.quote(stringValue())
                /* Remove redundant escaping */
                .replaceAll("\\\\/", "/");
    }

    @Override
    public String toJavaObject() {
        return stringValue();
    }

    @Override
    public <T> T toJavaObject(Class<T> clazz) throws CborConversionException {
        CborConversionException lastException = null;

        switch (getTag()) {
            case CborTag.URI:
                if (clazz.isAssignableFrom(URI.class)) {
                    try {
                        return clazz.cast(new URI(stringValue()));
                    } catch (URISyntaxException e) {
                        lastException = new CborConversionException(e);
                    }
                }
                break;

            default:
                break;
        }

        if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(stringValue());
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new CborConversionException(clazz + " is not assignable from a text string");
    }

    @Override
    public int hashCode() {
        return (getTag() - CborTag.UNTAGGED) * 1337 + stringValue().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof CborTextString)) {
            return false;
        }

        CborTextString rhs = (CborTextString) obj;

        return getTag() == rhs.getTag() && stringValue().equals(rhs.stringValue());
    }

    @Override
    public String toString(int ignore) {
        return toString();
    }

    @Override
    public String toString() {
        String ret = toJsonString();

        int tag = getTag();

        return tag == CborTag.UNTAGGED ? ret : tag + "(" + ret + ")";
    }
}
