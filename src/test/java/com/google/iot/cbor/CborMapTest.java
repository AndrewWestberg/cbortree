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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class CborMapTest extends CborTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(CborMapTest.class.getCanonicalName());

    @Test
    void testCreateFromCborByteArray() throws Exception {
        byte[] array = decode("bf00a0ff");
        CborObject obj;

        obj = CborMap.createFromCborByteArray(array);
        assertEquals("{0:{}}", obj.toString());

        obj = CborMap.createFromCborByteArray(array, 2, array.length - 3);
        assertEquals("{}", obj.toString());

        assertThrows(
                CborParseException.class,
                () -> CborMap.createFromCborByteArray(array, 2, array.length - 2));
        assertThrows(
                CborParseException.class,
                () -> CborMap.createFromCborByteArray(array, 1, array.length - 1));
        assertThrows(
                CborParseException.class,
                () -> CborMap.createFromCborByteArray(array, 3, array.length - 3));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> CborMap.createFromCborByteArray(array, 40, 1));
    }

    @Test
    void testCorruptedMap() {
        assertThrows(
                CborParseException.class,
                () -> CborObject.createFromCborByteArray(decode("bf6346756ef563416d742100c0ff")));
    }

    @Test
    void testMapWithDuplicateKeys() {
        byte[] array = decode("a2581cf923673e7bc027eba929a9086b7b04a126a18ef524b41db6fb014a26a14a414c414c415f5445535401581cf923673e7bc027eba929a9086b7b04a126a18ef524b41db6fb014a26a14c4741474147415f544553543201");

        String output = "{h'f923673e7bc027eba929a9086b7b04a126a18ef524b41db6fb014a26':{h'414c414c415f54455354':1},h'f923673e7bc027eba929a9086b7b04a126a18ef524b41db6fb014a26':{h'4741474147415f5445535432':1}}";
        CborMap obj = (CborMap) assertParseToString(output, array);

        byte[] encoded = obj.toCborByteArray();

        assertArrayEquals(array, encoded);

        assertEquals(2, obj.size());
        assertFalse(obj.isEmpty());
        assertFalse(obj.isValidJson());
    }

    @Test
    void testParser2() {
        byte[] array = decode("a26161016162820203");

        String output = "{\"a\":1,\"b\":[2,3]}";

        CborMap obj = (CborMap) assertParseToString(output, array);

        byte[] encoded = obj.toCborByteArray();

        assertArrayEquals(array, encoded);

        assertEquals(2, obj.size());
        assertFalse(obj.isEmpty());
        assertTrue(obj.isValidJson());
    }

    @Test
    void testTaggedMap() {
        byte[] array = decode("d9d9f7a0");

        String output = "55799({})";

        CborMap obj = (CborMap) assertParseToString(output, array);

        byte[] encoded = obj.toCborByteArray();

        assertArrayEquals(array, encoded);

        assertEquals(0, obj.size());
        assertEquals(CborTag.SELF_DESCRIBE_CBOR, obj.getTag());
        assertTrue(obj.isEmpty());
        assertTrue(obj.isValidJson());
    }

    @Test
    void testParser3() {
        byte[] array = decode("a0");

        String output = "{}";

        CborMap obj = (CborMap) assertParseToString(output, array);

        byte[] encoded = obj.toCborByteArray();

        assertArrayEquals(array, encoded);

        assertEquals(0, obj.size());
        assertTrue(obj.isEmpty());
        assertTrue(obj.isValidJson());
    }

    @Test
    void testParser4() {
        byte[] array = decode("a201020304");

        String output = "{1:2,3:4}";

        CborMap obj = (CborMap) assertParseToString(output, array);

        byte[] encoded = obj.toCborByteArray();

        assertArrayEquals(array, encoded);
    }

    @Test
    void testParser8() throws Exception {
        byte[] array = decode("bf6346756ef463416d7421ff");

        String output = "{\"Fun\":false,\"Amt\":-2}";

        CborMap obj = (CborMap) assertParseToString(output, array);

        assertTrue(obj.isValidJson());

        byte[] encoded = obj.toCborByteArray();

        assertEquals(obj, obj.copy());

        assertEquals(obj, CborObject.createFromJavaObject(obj.toJavaObject(Map.class)));
        assertThrows(
                CborConversionException.class,
                () -> CborObject.createFromJavaObject(obj.toJavaObject(String[].class)));
    }

    @Test
    void testParser9() throws Exception {
        byte[] array = decode("bf6346756ef463416d7421a0f7ff");

        String output = "{\"Fun\":false,\"Amt\":-2,{}:undefined}";

        CborMap obj = (CborMap) assertParseToString(output, array);

        assertEquals(3, obj.size());
        assertFalse(obj.isEmpty());
        assertFalse(obj.isValidJson());

        byte[] encoded = obj.toCborByteArray();

        assertEquals(obj, obj.copy());

        assertEquals("{\"Fun\":false,\"Amt\":-2,\"{}\":\"undefined\"}", obj.toJsonString());

        // This is not equals because CborSimple.UNDEFINED gets converted to null...
        assertNotEquals(obj, CborObject.createFromJavaObject(obj.toJavaObject(Map.class)));

        assertThrows(
                CborConversionException.class,
                () -> CborObject.createFromJavaObject(obj.toJavaObject(String[].class)));
    }

    @Test
    void testParser10() throws Exception {
        byte[] array = decode("bf6346756ef463416d7421a001ff");

        String output = "{\"Fun\":false,\"Amt\":-2,{}:1}";

        CborMap obj = (CborMap) assertParseToString(output, array);

        assertEquals(3, obj.size());
        assertFalse(obj.isEmpty());
        assertFalse(obj.isValidJson());

        byte[] encoded = obj.toCborByteArray();

        assertEquals(obj, obj.copy());

        assertEquals("{\"Fun\":false,\"Amt\":-2,\"{}\":1}", obj.toJsonString());

        assertEquals(obj, CborObject.createFromJavaObject(obj.toJavaObject(Map.class)));
        assertThrows(
                CborConversionException.class,
                () -> CborObject.createFromJavaObject(obj.toJavaObject(String[].class)));
    }

    @Test
    void testMapEditing() throws Exception {
        byte[] array = decode("a0");
        CborMap obj = (CborMap) assertParseToString("{}", array);
        assertEquals(0, obj.size());

        byte[] witness = decode("a10081825820e98bc049f662c60c3e1066df75f01e02688c3432765b0ec870463d1f93272523584027bb51002786ec504beefd61fb87e1da0aad1ac3ed2b6772eec5148bd1db8145bdb4235157476133b9b8bc57c12da742ba17f4f4494ac2f649b52ab9e24b1c0d");
        CborMap witnessMap = (CborMap) CborObject.createFromCborByteArray(witness);
        CborInteger witnessIndex = CborInteger.create(0);
        CborObject witnessValue = witnessMap.get(witnessIndex);
        assert witnessValue != null;
        obj.put(witnessIndex, witnessValue);
        byte[] encoded = obj.toCborByteArray();
        assertArrayEquals(witness, encoded);
    }
}
