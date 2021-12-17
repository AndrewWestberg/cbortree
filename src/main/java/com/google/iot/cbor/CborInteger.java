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

/**
 * CBOR integer object interface.
 */
public abstract class CborInteger extends CborObject implements CborNumber {
    // Prohibit users from subclassing for now.
    CborInteger() {
    }

    /**
     * Additional info value for when the subsequent value/size encoding is one byte long.
     */
    static final BigInteger BI_ADDITIONAL_INFO_EXTRA_1B = BigInteger.valueOf(24L);

    static final BigInteger BI_MAX_1B = BigInteger.valueOf(0xFFL);
    static final BigInteger BI_MAX_2B = BigInteger.valueOf(0xFFFFL);
    static final BigInteger BI_MAX_4B = BigInteger.valueOf(0xFFFFFFFFL);
    static final BigInteger BI_MAX_8B = new BigInteger("18446744073709551615");
    static final BigInteger BI_MIN_8B = new BigInteger("-18446744073709551616");



    public static CborInteger create(Number value) {
        return create(value, CborTag.UNTAGGED);
    }

    public static CborInteger create(Number value, int tag) {
        return create(value, tag, null);
    }

    public static CborInteger create(Number value, int tag, @Nullable Integer majorType) {
        if (value.getClass().isAssignableFrom(BigInteger.class)) {
            return new CborIntegerImpl((BigInteger) value, tag, majorType);
        }
        return new CborIntegerImpl(BigInteger.valueOf(value.longValue()), tag, majorType);
    }

    static int calcAdditionalInformation(BigInteger val) {
        if (val.compareTo(BigInteger.ZERO) < 0) {
            val = val.negate().subtract(BigInteger.ONE);
        }

        if (val.compareTo(BI_ADDITIONAL_INFO_EXTRA_1B) < 0) {
            return val.byteValue();
        }

        if (val.compareTo(BI_MAX_1B) <= 0) {
            return ADDITIONAL_INFO_EXTRA_1B;
        }

        if (val.compareTo(BI_MAX_2B) <= 0) {
            return ADDITIONAL_INFO_EXTRA_2B;
        }

        if (val.compareTo(BI_MAX_4B) <= 0) {
            return ADDITIONAL_INFO_EXTRA_4B;
        }

        return ADDITIONAL_INFO_EXTRA_8B;
    }

    @Override
    public final int getAdditionalInformation() {
        return calcAdditionalInformation(bigIntegerValue());
    }

    @Override
    public int getMajorType() {
        return (bigIntegerValue().compareTo(BigInteger.ZERO) < 0) ? CborMajorType.NEG_INTEGER : CborMajorType.POS_INTEGER;
    }

    /**
     * Returns the value of the integer as a {@link long}.
     *
     * @return The {@link long} value of this object.
     */
    @Override
    public abstract long longValue();

    @Override
    public final float floatValue() {
        return (float) longValue();
    }

    @Override
    public final double doubleValue() {
        return (double) longValue();
    }

    @Override
    public final CborInteger copy() {
        // CborInteger objects are immutable, thus we can copy by returning this.
        return this;
    }

    @Override
    public final boolean isValidJson() {
        // CborIntegers are always valid in JSON.
        return true;
    }

    @Override
    public final String toJsonString() {
        return Long.toString(longValue());
    }

    @Override
    public Number toJavaObject() {
        BigInteger bigIntVal = bigIntegerValue();
        if (bigIntVal.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 || bigIntVal.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
            return bigIntVal;
        }
        long lval = longValue();
        if ((lval > Integer.MAX_VALUE) || (lval < Integer.MIN_VALUE)) {
            return lval;
        }
        return (int) lval;
    }

    @Override
    public <T> T toJavaObject(Class<T> clazz) throws CborConversionException {
        if (clazz.isAssignableFrom(Number.class) || Object.class.equals(clazz)) {
            return clazz.cast(toJavaObject());
        }

        if (clazz.isAssignableFrom(Float.class)) {
            return clazz.cast(floatValue());
        }

        if (clazz.isAssignableFrom(Double.class)) {
            return clazz.cast(doubleValue());
        }

        if (clazz.isAssignableFrom(BigInteger.class)) {
            return clazz.cast(bigIntegerValue());
        }

        if (clazz.isAssignableFrom(Long.class)) {
            return clazz.cast(longValue());
        }

        try {
            if (clazz.isAssignableFrom(Integer.class)) {
                return clazz.cast(intValueExact());
            }

            if (clazz.isAssignableFrom(Short.class)) {
                return clazz.cast(shortValueExact());
            }

        } catch (ArithmeticException x) {
            throw new CborConversionException(x);
        }

        throw new CborConversionException(
                String.format("%s is not assignable from %s", clazz, Long.class));
    }

    @Override
    public final int hashCode() {
        // Mixes hashes of both the double value and the long value in
        // order to preserve the semantic equivalence of numbers as
        // described in Section 3.6 of RFC7049.
        return (getTag() - CborTag.UNTAGGED) * 1337 + Double.hashCode(doubleValue())
                ^ Long.hashCode(longValue());
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof CborObject)) {
            return false;
        }

        if (getTag() != ((CborObject) obj).getTag()) {
            return false;
        }

        if (obj instanceof CborInteger) {
            CborInteger rhs = (CborInteger) obj;
            return bigIntegerValue().compareTo(rhs.bigIntegerValue()) == 0;
        }

        if (!(obj instanceof CborNumber)) {
            return false;
        }

        CborNumber rhs = (CborNumber) obj;

        // Compares both long value and double value in
        // order to preserve the semantic equivalence of numbers as
        // described in Section 3.6 of RFC7049.
        return longValue() == rhs.longValue()
                && Double.doubleToRawLongBits(doubleValue())
                == Double.doubleToRawLongBits(rhs.doubleValue());
    }

    @Override
    public String toString(int ignore) {
        return toString();
    }

    @Override
    public String toString() {
        String ret = bigIntegerValue().toString();

        int tag = getTag();

        return tag == CborTag.UNTAGGED ? ret : tag + "(" + ret + ")";
    }
}
