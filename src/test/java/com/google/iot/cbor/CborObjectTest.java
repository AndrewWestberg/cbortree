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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class CborObjectTest extends CborTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(CborObjectTest.class.getCanonicalName());

    @Test
    void testCreateFromCborByteArray() throws Exception {
        byte[] array = decode("9f00ff");
        CborObject obj;

        obj = CborObject.createFromCborByteArray(array);
        assertEquals("[0]", obj.toString());

        obj = CborObject.createFromCborByteArray(array, 1, array.length - 2);
        assertEquals("0", obj.toString());

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> CborObject.createFromCborByteArray(array, 30, 1));
        assertThrows(
                CborParseException.class,
                () -> CborObject.createFromCborByteArray(array, 1, array.length - 1));
        assertThrows(
                CborParseException.class,
                () -> CborObject.createFromCborByteArray(array, 2, array.length - 2));
    }

    @Test
    void testJavaToCborConversion() throws Exception {
        assertEquals("\"hello\"", CborObject.createFromJavaObject("hello").toString());
        assertEquals(
                "32(\"http://google.com/\")",
                CborObject.createFromJavaObject(URI.create("http://google.com/")).toString());
        assertEquals("false", CborObject.createFromJavaObject(false).toString());
        assertEquals("null", CborObject.createFromJavaObject(null).toString());
        assertEquals("true", CborObject.createFromJavaObject(true).toString());
        assertEquals("12345", CborObject.createFromJavaObject(12345).toString());
        assertEquals("12345", CborObject.createFromJavaObject((short) 12345).toString());
        assertEquals("-12345", CborObject.createFromJavaObject(-12345L).toString());
        assertEquals("18446744073709551615", CborObject.createFromJavaObject(new BigInteger("18446744073709551615")).toString());
        assertEquals("-18446744073709551616", CborObject.createFromJavaObject(new BigInteger("-18446744073709551616")).toString());
        assertEquals("2(h'05563918244f3fffff')", CborObject.createFromJavaObject(new BigInteger("98446744073709551615")).toString());
        assertEquals("12345.0_2", CborObject.createFromJavaObject(12345.0f).toString());
        assertEquals("12345.0_3", CborObject.createFromJavaObject(12345.0).toString());
        assertEquals("[]", CborObject.createFromJavaObject(new LinkedList<>()).toString());
        assertEquals("{}", CborObject.createFromJavaObject(new HashMap<>()).toString());
        assertEquals(
                "h'01020304'",
                CborObject.createFromJavaObject(new byte[]{0x01, 0x02, 0x03, 0x04}).toString());
        assertEquals(
                "[1,2,3,4]",
                CborObject.createFromJavaObject(new int[]{0x01, 0x02, 0x03, 0x04}).toString());
        assertEquals(
                "[1,2,3,4]",
                CborObject.createFromJavaObject(new long[]{0x01, 0x02, 0x03, 0x04}).toString());
        assertEquals(
                "[1,2,3,4]",
                CborObject.createFromJavaObject(new short[]{0x01, 0x02, 0x03, 0x04}).toString());
        assertEquals(
                "[false,true,false,true]",
                CborObject.createFromJavaObject(new boolean[]{false, true, false, true})
                        .toString());
        assertEquals(
                "[1.0_2,2.0_2,3.0_2,4.0_2]",
                CborObject.createFromJavaObject(new float[]{1.0f, 2.0f, 3.0f, 4.0f}).toString());
        assertEquals(
                "[1.0_3,2.0_3,3.0_3,4.0_3]",
                CborObject.createFromJavaObject(new double[]{1.0f, 2.0f, 3.0f, 4.0f}).toString());
        assertEquals(
                "[1,2,null,4]",
                CborObject.createFromJavaObject(new Object[]{1, 2, null, 4}).toString());
    }

    @Test
    void testCborToJavaConversion() throws Exception {
        assertEquals(12345, CborInteger.create(12345).toJavaObject());
        assertEquals(new BigInteger("18446744073709551615"), CborInteger.create(new BigInteger("18446744073709551615")).toJavaObject());
        assertEquals(new BigInteger("-18446744073709551616"), CborInteger.create(new BigInteger("-18446744073709551616")).toJavaObject());
        assertEquals("hello", CborTextString.create("hello").toJavaObject());
        assertEquals(true, CborObject.createFromJavaObject(true).toJavaObject());
        assertEquals(false, CborObject.createFromJavaObject(false).toJavaObject());
        assertNull(CborObject.createFromJavaObject(null).toJavaObject());
        assertEquals(
                "http://google.com/",
                CborObject.createFromJavaObject(URI.create("http://google.com/")).toJavaObject());
    }

    @Test
    void testJavaToCborConversionFailures() {
        assertThrows(
                CborConversionException.class, () -> CborObject.createFromJavaObject(Object.class));
        assertThrows(
                CborConversionException.class,
                () -> CborObject.createFromJavaObject(new Object[]{1, 2, null, Object.class}));
    }

    @Test
    void testInvalidTags() {
        final int INVALID_TAG = CborTag.UNTAGGED - 1;
        assertThrows(IllegalArgumentException.class, () -> CborSimple.create(0, INVALID_TAG));
        assertThrows(IllegalArgumentException.class, () -> CborInteger.create(0, INVALID_TAG));
        assertThrows(IllegalArgumentException.class, () -> CborFloat.create(0, INVALID_TAG));
        assertThrows(IllegalArgumentException.class, () -> CborArray.create(INVALID_TAG));
        assertThrows(
                IllegalArgumentException.class,
                () -> CborArray.create(new LinkedList<>(), INVALID_TAG));
        assertThrows(IllegalArgumentException.class, () -> CborMap.create(INVALID_TAG));
        assertThrows(
                IllegalArgumentException.class, () -> CborMap.create(new HashMap<>(), INVALID_TAG));
        assertThrows(IllegalArgumentException.class, () -> CborTextString.create("", INVALID_TAG));
        assertThrows(
                IllegalArgumentException.class,
                () -> CborByteString.create(new byte[0], 0, 0, INVALID_TAG));
    }

    @Test
    void testTags() {
        final int TAG = CborTag.SELF_DESCRIBE_CBOR;
        assertEquals(TAG, CborSimple.create(0, TAG).getTag());
        assertEquals(TAG, CborInteger.create(0, TAG).getTag());
        assertEquals(TAG, CborFloat.create(0, TAG).getTag());
        assertEquals(TAG, CborTextString.create("", TAG).getTag());
        assertEquals(TAG, CborByteString.create(new byte[0], 0, 0, TAG).getTag());

        assertEquals(TAG, CborMap.create(TAG).getTag());
        CborMap map = CborMap.create();
        assertEquals(CborTag.UNTAGGED, map.getTag());

        assertEquals(TAG, CborArray.create(TAG).getTag());
        CborArray array = CborArray.create();
        assertEquals(CborTag.UNTAGGED, array.getTag());
    }

    @Test
    void testJSONObjectConversion1() throws Exception {
        String jsonString =
                "{\"modeOfOperation\":\"cbc\",\"encrypted\":[[]],\"f\":0.2,\"segmentSize\":null,\"plaintext\":[[]],\"iv\":[239,223,181,236,61,118,254,53,251,59,113,4,2,58,69,100],\"key\":[58,69,185,228,20,175,122,156,194,103,53,209,61,20,147,253,222,177,125,54,3,84,13,168]}";
        JSONObject jsonObject = new JSONObject(jsonString);

        CborMap cborObject = CborMap.createFromJSONObject(jsonObject);

        if (DEBUG) LOGGER.info("cborObject = " + cborObject);

        assertTrue(cborObject.areAllKeysStrings());

        assertEquals(jsonObject.toString(), cborObject.toJsonString());
        assertEquals(jsonString, cborObject.toJsonString());

        assertEquals(cborObject.toNormalMap(), jsonObject.toMap());
        assertEquals(cborObject, CborMap.createFromJavaObject(jsonObject.toMap()));

        assertEquals(cborObject, cborObject.copy());

        assertEquals(
                cborObject, CborObject.createFromJavaObject(cborObject.toJavaObject(Object.class)));
        assertEquals(
                cborObject, CborObject.createFromJavaObject(cborObject.toJavaObject(Map.class)));
        assertThrows(
                CborConversionException.class,
                () -> CborObject.createFromJavaObject(cborObject.toJavaObject(String[].class)));
    }

    @Test
    void testJSONObjectConversion2() throws Exception {
        String jsonString =
                "{\"glossary\":{\"title\":\"example glossary\",\"GlossDiv\":{\"GlossList\":{\"GlossEntry\":{\"GlossTerm\":\"Standard Generalized Markup Language\",\"GlossSee\":\"markup\",\"SortAs\":\"SGML\",\"GlossDef\":{\"para\":\"A meta-markup language, used to create markup languages such as DocBook.\",\"GlossSeeAlso\":[\"GML\",\"XML\"]},\"ID\":\"SGML\",\"Acronym\":\"SGML\",\"Abbrev\":\"ISO 8879:1986\"}},\"title\":\"S\"}},\"float\":1.5}";
        JSONObject jsonObject = new JSONObject(jsonString);

        CborMap cborObject = CborMap.createFromJSONObject(jsonObject);

        if (DEBUG) LOGGER.info("cborObject = " + cborObject);

        assertTrue(cborObject.areAllKeysStrings());

        assertEquals(jsonObject.toString(), cborObject.toJsonString());
        assertEquals(jsonString, cborObject.toJsonString());

        assertEquals(cborObject.toNormalMap(), jsonObject.toMap());
        assertEquals(cborObject, CborMap.createFromJavaObject(jsonObject.toMap()));

        assertEquals(cborObject, cborObject.copy());

        assertEquals(
                cborObject, CborObject.createFromJavaObject(cborObject.toJavaObject(Object.class)));
        assertEquals(
                cborObject, CborObject.createFromJavaObject(cborObject.toJavaObject(Map.class)));
        assertThrows(
                CborConversionException.class,
                () -> CborObject.createFromJavaObject(cborObject.toJavaObject(String[].class)));
    }

    @Test
    void testIndefiniteLengthArrays() throws Exception {
        String cborIndefiniteArrays = "85828f1a0061049a1a0287161c58209551cd55b9b7d62adb870b7374abda8c9ba0e1e1b2b3ba751f8e78f6fdae385158207a0119cad9e1a38ab7f4d4404f29ce053c0132b1c2bd33e2a019125e730255315820955ba016959d42f50f2e77d4069f5ed1265a6337a7b2105643bbf15fc196be04825840a030e6f1fa1bfed1a01c9fd96cfbe92194ded6767c81e713712d4779e2d2bcfaaec20bdb70ea119de27dc4401a4f587bb0fae88eb0b51c8f8ad64ea981413236585050cebdf8683684386990e4ded2123188765c6c122a81ec6e2c1758b042213d28be6cff3e374ab16caf0765de9ea49bd51e76a4367c582a99213ce32118ae57b5f8c1ee0c51ce73a33bc9aba9f083fa038258400001caa1ffd2e37b046938da88d9e685d7cf8a439b552007e487b212b5e7138db5432d41c11332031d13691240abfeff16fce55c8b87f117a08ea19f4cbc2ed758501429ed338c846396e8a2bb7fd04689564330ff5468fa05d2dbcd41f93acdcd6a4a1be5add655ac381c560d51758c82ce36beb89b8a6d73e75f5d2914dd301be46a31c466b5ef1137ff64671e1aae240c1910f75820e6099e584837666d6ddde68355196445ded22c6d50b9fcde25f15dfe39c1d9f4582082db70495f6104810bd4632e79833ebd19c56308983063b0fe9b61023840bd080219012b5840d1479bb78d334a0f778b12345c4123b3103e1144347898a8ba0934047762ed4e3ad505de44ade0dab62962581ab5c6712495e17078df3b14b4dd105b49e9ca0b06005901c0586e3c6bdac14cb5ba9a772314d623eb5278ac31539fda4e4c827cde5676f8cf4ad17ebb9bed2021f03db09af44c35fb6870cedc61c12dd3639e1bf905e2b60cf9aa4e5673968f59a6a2f9d1aae74e5b19fc048098d64beee449198d5c02dcaff635b160b51bf5f8f474b50753a50bc736bfa70a5b8d6274e558ed4c412ffde88ed0d1fb72aaa41aa6d3f661cac3fd7e2774ad5b176a3281b9f613fdfacfecb62a063d690cadc1e6cfb75c2c6407e0d1481d875656820ca493f879e2ea52f831c40a48ffb7082a2f6bd6b36067e5cd998a9ffd4b7caa9207eb8da95c61845e54d0f9aeb485e086a0bab5d21ad80d8e3ca07303b95c8a86cbcf7f973fba6ab7b9bf1ef60b979fe93387a18b3f7a7cf85ff35b6926a932c8e7a81b866633a7f4b013dc0f56da6cded899cb654c2756fd4b7a115b70d18bd792794c2f8e9616826f6c8eb83bc7c4a0d7ea8d050883782d248f249f8a1671b856f561dd5b8aa234d1432ce70c4d5c1b46325df3be1422f65b714b0f6b7e10eb109f5806adc62c113af5ba1bccd5d18670fb91f1dd82a7d79ce037dbcd4470d5e471812f0161dd4717ebe0126c4aa81526e8c2117b8c752f5326f2fd856510b0689a6cba07413d171c82a4008182582088a29f840e8a8ff7f0c145320705b9bb498423c8b0f28ffbc4300bc3573cbb1500018282581d610b35f9ff0387bc7e3985b42ec0b118004991d2d29769beef1625b6af1a02160ec082583901706e890ef0ca6362fcdc8a2675c31cd57bfd6d1fb5337a95b812128464ad0c7e9684167e241c5ae594f94cc186f256d2f586f9074266bba11a0049bebf021a00028c81031a02873220a3009f8258200e6051e181758445d11631650bda34a02c7824c502fda6c389c0e4d07363570d018258200e6051e181758445d11631650bda34a02c7824c502fda6c389c0e4d07363570d18238258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6158258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6168258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6178258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6182d8258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6182e8258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6182f8258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a618308258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a618318258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182028258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182048258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac1820b8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac1820f8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac18218198258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182181a8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182181b8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac18218208258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac1821825825820c4b075cc362edc4995254c59cb74c6a967b3765fb1bfda27d01e6ef43ce7c7121832825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce12825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce14825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce16825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181a825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181b825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181e825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181f825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce1823ff018282581d61ee9c3318ead2183091af7067887224cdcc39a2bd6f28ed87ffb2c422821a005b8d80a1581cd894897411707efa755a76deb66d26dfd50593f2e70863e1661e98a0a14a7370616365636f696e7319dac082581d6129fe632bb0f66526aa201858b062fa94d6fc09df17dbcb7ee3062e69821a02f52267a2581c4247d5091db82330100904963ab8d0850976c80d3f1b927e052e07bda146546f6b68756e196d60581c5ad8deb64bfec21ad2d96e1270b5873d0c4d0f231b928b4c39eb2435a14661646f7369611b00000041314cf000021a0005ce1982a1008182582028e7bef9759ce838757bcc71f03c787947af96db547e572b11a08ad4d6aa08b55840d66a278874f8755cc1b63626e0e0ac4b75a00cd6c4389a1efa4e1f5a9b4a9d6d77c6ed4f78d34f2a44619493c1c21a9a24fa3db7dc3d9a9694f943f81d4e2305a1009f825820e347f92a5cc637b086d53be17ae86a2d22da43257fe4ceee503753c949e3d3735840c3e704d2021633973be6868e23ce1f2ca0a07ce47e811d3771af6dfa10722a579acd962b6210ee20f34ae2acef762830337ac1a8d38ed46fc03d683d15bc10098258206c563992abd10ec6991bbbd21232953346446c1d74d33bc11309088ad2e092da5840ea3747bc23b3082416bd6f677aae8f821819259bda3363761ac82d9ad87f50b9da1c7c3eac5c7a0e09f78efaf874f9a615a26181cd22555d4c8f1a2fc01ad90682582094f9d8a6c58868a73315523c1186c4fe909cc4ede91de74e7701e13a6fe3d0f65840d9ed4aa0d10c3fb4c9e45757f8409a82b16aa0c6815f94f376381fd4de9920b49dc9954505db147780c412df3574849a3d3513ee2dc2169788ff9deef0089406825820aa202e3db01e2a3055622a0ae3423cdbd1276fe69175d14a19e73dc93d16095f5840fee4ba89cc656ee6bab6fee903695ab234bacd05364b082fa9d92c432c4ea5f2130b59bf97170fd444dab2a0b4fccee4264546a8cbe102964fadfd133f80be0b825820905c537ff56c8f6946d5fa93399b2439a7b850bd602b84d53e67948a67892b955840a9027eab92f082cd0c3e241c4912649caa870c8485636305bb8ca34e50b3e020c363957ceb3a4693e4205b5abd4d470bad814c5690a424d24e20cd499456480c8258205eb08350d659faaa81781d011a47b3a6c043183ac828a3b8e5fbe11e8169857c584030644a8fb444ac69692136bb5691adc4dabf4ad3bc333dfd4f6138bc8bb953e196eb608e03893e8fa12515a467f6f2f22a8e48bd5f984ca5046c2fc8382a9707825820c68d25dea37731d9d0258873ac2079b67ee2f6fd58f349351324807a901ce3fa58403ee4c9204dfd1a8adc82a7f9b6d0564284dc5a096d63d4b19b4e5a50edabbc9cff652c16c0e3558d94f9091ab5ab7f6d91b985cb0217471f3f702e8efc698d088258204de38aa589f1b4381aedcea963579d78a51c823186c6067f2dc24544263a869658401ff5a358febfd0dec467f0a90c87f123d50b13a5558624273b469c56c3b1ba6a43901629a5d06698c893db91c933be1ffc8c25b57046c1e88f8efa5f3718af0e82582064c5b2773e53e1380a5d6e5be992521df0aaff30b255560593bb995d20bf928a584029a561017b2fa187937abca33fca3284ad0dfb699fd78d70b7d240e161c0811aeeb843f1cbc7dc5dd53f9aa4f93adbbe1fba28cd6149d949fa171c536628470b8258201f581b6b9cb7b8bcecdf15bd0fd801e1213433414ad5cbd1c88bba70ff24133258404986455145be08d6e813bb4ca3449f409c6425aa7f6b90288db91c8a1fe1d9004776bf880419cfedbed4aac1f0170456ec48ff31b0a7f9a554c743a88697820282582044bbf14ae126cd4b2c7abf20f311b3f521f58af54caaf7417424dd1952be520b5840a3a0582fbc20a68da81c2f38c454b1960ac25645df248b747bef178a5f15077a6b1a63277dd171fa7221e92f1d0cdeeb42bca909b0eaad2767696d99b6cf410f8258202ef2013b7e10a79a3934083b495b92a0bdbeecc38f833baee7c463c6a7ca95a15840970b027f8417c7e19f73ee6bc8b0c9a0ddb2a0d52b96f4a76ff272ffca936b8b33924e14a134e57767ad2195ab37c35c7b047dc46978074959594d656dd583078258209729c2ea45052d445c734587401da769d2067d1a02a72f28d0c80ce4890d81745840fd815f374d63f105447fd2df603753be498082704d2cc0e741e596f262b57189ba62d10b6f300082e51e45090c04a8aad61a7f4a831e3487d08c4556331adf0e8258201056b042842e1fc8899e801775808d73b130e01959bb4e0e0cbafadacb8760b658403b5b679beef1ef0d96416e9641736bdd6810fb07089259bd017d21b077d7a15fafa3b48d9514849252eb4283c7ea37e340e72c9eac83eec519c48210b87d9d038258201c97fc364cf33d4979fc1ee703fa1d6ead8dd9bc6010614e87100ea61563faef58409251db400caf92a024658ba7534005d1c97e91511878b4fd9ca701437f7f6604ff3c8d35304d416ab3ec1c8efd0633c08bd7b56de79be821cbb680d91b88180882582001f47938bc0f72f1343848c51a961f43e156caaf5bda44f20c13c85fcb1744435840fabdc4f7170fe5e2a66877729b93801440e937f9320195c4408ce463b9e374d92a3396814562e6c0166cfc1a32e67e55905b8d9f6d55ddce7311249172c56f048258204629be8daad9b8014081f4b82829f6388aa9261928e2c17058e0a93b8e8f42035840865c85c8285313c54fd2db93de5b103836a4fa7b602e33f13f9b32363f8d4851ae0c3924d56902b098ba122ccd03a2f957e1cb06a4e76b2a78ff7e83d11b4d0a8258205b2820b589f011c3777a97154309d1725e3d6a4e8fec64ca39185dc1ba05b66c58409a16df51b366e369e119a1a215258752fc11a837311cb830a1dd9652d7f1cdf869be9fee2239bfd43cce45476e3e8340ca95e9fd50ed5d596ede20f0a499780e82582036e16c2ed6f1a467e834936aad7f5d5a3987936d9329e3ef699ae2e416a65f1358405ff16b63963015e1564296b13f14ed5f657d57fef0eafb4ba5d014b79c9d893ba3dd7c510930349a7838a0bcd210c0341575a4933c3a3c6a36988260753c8707825820c8e9bff01f7eaf91f53ff98e5ea4670ba98f73d858ba80107e91ede9911405e358409f4b63ad8de5b01a9eb13255ae075f3a8df95dcf4833f24b82cf3dcaa165a98973de336f84e752bc12768e71a91b0fe3339ea60b38ad909b3ed5ab61822a72018258208359570e262d76a4e57b48b722dc71e9e7b5a866ee0bc9739523c5f1c3b4d176584070e6f4a696affda27ea7acef8836804a7e87de5d899c623ff13844b95b1a2cf423d86c820fa2dca991d39022cf0f7a273cdde4d61e4fe81a8b328947a4acf30e825820acda4ffe2e9038523f8556efe074e3c81e1fa32a1bc6741cb63fbb7ea1826c25584052b0954234d8ebe6311539e280f36ffc2c8aa40bb39aaaa344ed71855628fdd89c5130b816fe789932294c1be779da673d8bbe3989002abcb490d69476a9150f8258202f23960d4302698fe690e18fd0cba07859feebc21d419d85ee0698ef1f42ff8f5840eeda68eb63a82e075c0434d1340e98dd9c2662d5c711c038f709c168e860e0195ac21c06aeb8f109b56919df91769d45ff39a0f80921c92ebd4567d844e6f706825820bb23899b812a62738c28e5e2d86f1751ae097df0c674405d0378981974160e885840590a44566f477460699b880291215c4ad37ad47645f50c6eb4f844a877e76eaecda4d874c3c507edd8ff209e2850f8bd49c450a617e88ac7fd037738681fd409825820d9f10541f4ec9b7fa98be9547144298dff3cf22e4076a953564c7b69489fa1fe58402f14e54a05f7e26ce3b188ce0647c64aff01f8642f1d2a1b304ea4816bc01852f0141092d622590b43ed7dfc97b773fcbefd16e8a43492145b3f814018394c058258202167cfa4624da6ccaebf616f6f7172529d2c1b6f5009ab48ce30b96ae2f42d8b5840e5bb73f60bb71deadc73b050849d5fa1e04567ceb4b18922decd3e4a9e88f66b0c6e9af496c0fb244e8f242eafa5270eb02892967fa5f8b5424a2489cd47c7078258200fc0944d73048732736fd51709474394f15d6a80c1112ea1387363fbe2a84a6558407422265edf385566262680112c3a8cb48b3bac6335a01332e3feb9cd94f9ecb5341893bdb0eb9493967924fb91aa4c318348663f2352ae929518bfc3c0d35204825820f32575c043fa72ef8440ddd330d5bfbf8329e2edec09a1cc73b3a820075be229584022cec5d2470005d2b333bfe17c8dbeb3ff97320412aba6e12ade614bbdb7e03b5270f6328dc267f5d12a2966d96ceacd36d41f8488e46d8f8ba8d74294aa000dffa080";
        String cborFixedArrays      = "85828f1a0061049a1a0287161c58209551cd55b9b7d62adb870b7374abda8c9ba0e1e1b2b3ba751f8e78f6fdae385158207a0119cad9e1a38ab7f4d4404f29ce053c0132b1c2bd33e2a019125e730255315820955ba016959d42f50f2e77d4069f5ed1265a6337a7b2105643bbf15fc196be04825840a030e6f1fa1bfed1a01c9fd96cfbe92194ded6767c81e713712d4779e2d2bcfaaec20bdb70ea119de27dc4401a4f587bb0fae88eb0b51c8f8ad64ea981413236585050cebdf8683684386990e4ded2123188765c6c122a81ec6e2c1758b042213d28be6cff3e374ab16caf0765de9ea49bd51e76a4367c582a99213ce32118ae57b5f8c1ee0c51ce73a33bc9aba9f083fa038258400001caa1ffd2e37b046938da88d9e685d7cf8a439b552007e487b212b5e7138db5432d41c11332031d13691240abfeff16fce55c8b87f117a08ea19f4cbc2ed758501429ed338c846396e8a2bb7fd04689564330ff5468fa05d2dbcd41f93acdcd6a4a1be5add655ac381c560d51758c82ce36beb89b8a6d73e75f5d2914dd301be46a31c466b5ef1137ff64671e1aae240c1910f75820e6099e584837666d6ddde68355196445ded22c6d50b9fcde25f15dfe39c1d9f4582082db70495f6104810bd4632e79833ebd19c56308983063b0fe9b61023840bd080219012b5840d1479bb78d334a0f778b12345c4123b3103e1144347898a8ba0934047762ed4e3ad505de44ade0dab62962581ab5c6712495e17078df3b14b4dd105b49e9ca0b06005901c0586e3c6bdac14cb5ba9a772314d623eb5278ac31539fda4e4c827cde5676f8cf4ad17ebb9bed2021f03db09af44c35fb6870cedc61c12dd3639e1bf905e2b60cf9aa4e5673968f59a6a2f9d1aae74e5b19fc048098d64beee449198d5c02dcaff635b160b51bf5f8f474b50753a50bc736bfa70a5b8d6274e558ed4c412ffde88ed0d1fb72aaa41aa6d3f661cac3fd7e2774ad5b176a3281b9f613fdfacfecb62a063d690cadc1e6cfb75c2c6407e0d1481d875656820ca493f879e2ea52f831c40a48ffb7082a2f6bd6b36067e5cd998a9ffd4b7caa9207eb8da95c61845e54d0f9aeb485e086a0bab5d21ad80d8e3ca07303b95c8a86cbcf7f973fba6ab7b9bf1ef60b979fe93387a18b3f7a7cf85ff35b6926a932c8e7a81b866633a7f4b013dc0f56da6cded899cb654c2756fd4b7a115b70d18bd792794c2f8e9616826f6c8eb83bc7c4a0d7ea8d050883782d248f249f8a1671b856f561dd5b8aa234d1432ce70c4d5c1b46325df3be1422f65b714b0f6b7e10eb109f5806adc62c113af5ba1bccd5d18670fb91f1dd82a7d79ce037dbcd4470d5e471812f0161dd4717ebe0126c4aa81526e8c2117b8c752f5326f2fd856510b0689a6cba07413d171c82a4008182582088a29f840e8a8ff7f0c145320705b9bb498423c8b0f28ffbc4300bc3573cbb1500018282581d610b35f9ff0387bc7e3985b42ec0b118004991d2d29769beef1625b6af1a02160ec082583901706e890ef0ca6362fcdc8a2675c31cd57bfd6d1fb5337a95b812128464ad0c7e9684167e241c5ae594f94cc186f256d2f586f9074266bba11a0049bebf021a00028c81031a02873220a300981c8258200e6051e181758445d11631650bda34a02c7824c502fda6c389c0e4d07363570d018258200e6051e181758445d11631650bda34a02c7824c502fda6c389c0e4d07363570d18238258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6158258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6168258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6178258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6182d8258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6182e8258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a6182f8258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a618308258206691fa16531d01f5508468a7dfac52f6f8bafe23bff5a0a9bdb09395278928a618318258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182028258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182048258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac1820b8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac1820f8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac18218198258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182181a8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac182181b8258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac18218208258207cd77910644537082b1113c3441d7b7805a275753720632e4f5ce83b76eac1821825825820c4b075cc362edc4995254c59cb74c6a967b3765fb1bfda27d01e6ef43ce7c7121832825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce12825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce14825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce16825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181a825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181b825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181e825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce181f825820cc0cf88a761b7331aaddac5ef2876c556e14ba9f0dc6e92027602f8ffb238fce1823018282581d61ee9c3318ead2183091af7067887224cdcc39a2bd6f28ed87ffb2c422821a005b8d80a1581cd894897411707efa755a76deb66d26dfd50593f2e70863e1661e98a0a14a7370616365636f696e7319dac082581d6129fe632bb0f66526aa201858b062fa94d6fc09df17dbcb7ee3062e69821a02f52267a2581c4247d5091db82330100904963ab8d0850976c80d3f1b927e052e07bda146546f6b68756e196d60581c5ad8deb64bfec21ad2d96e1270b5873d0c4d0f231b928b4c39eb2435a14661646f7369611b00000041314cf000021a0005ce1982a1008182582028e7bef9759ce838757bcc71f03c787947af96db547e572b11a08ad4d6aa08b55840d66a278874f8755cc1b63626e0e0ac4b75a00cd6c4389a1efa4e1f5a9b4a9d6d77c6ed4f78d34f2a44619493c1c21a9a24fa3db7dc3d9a9694f943f81d4e2305a100981c825820e347f92a5cc637b086d53be17ae86a2d22da43257fe4ceee503753c949e3d3735840c3e704d2021633973be6868e23ce1f2ca0a07ce47e811d3771af6dfa10722a579acd962b6210ee20f34ae2acef762830337ac1a8d38ed46fc03d683d15bc10098258206c563992abd10ec6991bbbd21232953346446c1d74d33bc11309088ad2e092da5840ea3747bc23b3082416bd6f677aae8f821819259bda3363761ac82d9ad87f50b9da1c7c3eac5c7a0e09f78efaf874f9a615a26181cd22555d4c8f1a2fc01ad90682582094f9d8a6c58868a73315523c1186c4fe909cc4ede91de74e7701e13a6fe3d0f65840d9ed4aa0d10c3fb4c9e45757f8409a82b16aa0c6815f94f376381fd4de9920b49dc9954505db147780c412df3574849a3d3513ee2dc2169788ff9deef0089406825820aa202e3db01e2a3055622a0ae3423cdbd1276fe69175d14a19e73dc93d16095f5840fee4ba89cc656ee6bab6fee903695ab234bacd05364b082fa9d92c432c4ea5f2130b59bf97170fd444dab2a0b4fccee4264546a8cbe102964fadfd133f80be0b825820905c537ff56c8f6946d5fa93399b2439a7b850bd602b84d53e67948a67892b955840a9027eab92f082cd0c3e241c4912649caa870c8485636305bb8ca34e50b3e020c363957ceb3a4693e4205b5abd4d470bad814c5690a424d24e20cd499456480c8258205eb08350d659faaa81781d011a47b3a6c043183ac828a3b8e5fbe11e8169857c584030644a8fb444ac69692136bb5691adc4dabf4ad3bc333dfd4f6138bc8bb953e196eb608e03893e8fa12515a467f6f2f22a8e48bd5f984ca5046c2fc8382a9707825820c68d25dea37731d9d0258873ac2079b67ee2f6fd58f349351324807a901ce3fa58403ee4c9204dfd1a8adc82a7f9b6d0564284dc5a096d63d4b19b4e5a50edabbc9cff652c16c0e3558d94f9091ab5ab7f6d91b985cb0217471f3f702e8efc698d088258204de38aa589f1b4381aedcea963579d78a51c823186c6067f2dc24544263a869658401ff5a358febfd0dec467f0a90c87f123d50b13a5558624273b469c56c3b1ba6a43901629a5d06698c893db91c933be1ffc8c25b57046c1e88f8efa5f3718af0e82582064c5b2773e53e1380a5d6e5be992521df0aaff30b255560593bb995d20bf928a584029a561017b2fa187937abca33fca3284ad0dfb699fd78d70b7d240e161c0811aeeb843f1cbc7dc5dd53f9aa4f93adbbe1fba28cd6149d949fa171c536628470b8258201f581b6b9cb7b8bcecdf15bd0fd801e1213433414ad5cbd1c88bba70ff24133258404986455145be08d6e813bb4ca3449f409c6425aa7f6b90288db91c8a1fe1d9004776bf880419cfedbed4aac1f0170456ec48ff31b0a7f9a554c743a88697820282582044bbf14ae126cd4b2c7abf20f311b3f521f58af54caaf7417424dd1952be520b5840a3a0582fbc20a68da81c2f38c454b1960ac25645df248b747bef178a5f15077a6b1a63277dd171fa7221e92f1d0cdeeb42bca909b0eaad2767696d99b6cf410f8258202ef2013b7e10a79a3934083b495b92a0bdbeecc38f833baee7c463c6a7ca95a15840970b027f8417c7e19f73ee6bc8b0c9a0ddb2a0d52b96f4a76ff272ffca936b8b33924e14a134e57767ad2195ab37c35c7b047dc46978074959594d656dd583078258209729c2ea45052d445c734587401da769d2067d1a02a72f28d0c80ce4890d81745840fd815f374d63f105447fd2df603753be498082704d2cc0e741e596f262b57189ba62d10b6f300082e51e45090c04a8aad61a7f4a831e3487d08c4556331adf0e8258201056b042842e1fc8899e801775808d73b130e01959bb4e0e0cbafadacb8760b658403b5b679beef1ef0d96416e9641736bdd6810fb07089259bd017d21b077d7a15fafa3b48d9514849252eb4283c7ea37e340e72c9eac83eec519c48210b87d9d038258201c97fc364cf33d4979fc1ee703fa1d6ead8dd9bc6010614e87100ea61563faef58409251db400caf92a024658ba7534005d1c97e91511878b4fd9ca701437f7f6604ff3c8d35304d416ab3ec1c8efd0633c08bd7b56de79be821cbb680d91b88180882582001f47938bc0f72f1343848c51a961f43e156caaf5bda44f20c13c85fcb1744435840fabdc4f7170fe5e2a66877729b93801440e937f9320195c4408ce463b9e374d92a3396814562e6c0166cfc1a32e67e55905b8d9f6d55ddce7311249172c56f048258204629be8daad9b8014081f4b82829f6388aa9261928e2c17058e0a93b8e8f42035840865c85c8285313c54fd2db93de5b103836a4fa7b602e33f13f9b32363f8d4851ae0c3924d56902b098ba122ccd03a2f957e1cb06a4e76b2a78ff7e83d11b4d0a8258205b2820b589f011c3777a97154309d1725e3d6a4e8fec64ca39185dc1ba05b66c58409a16df51b366e369e119a1a215258752fc11a837311cb830a1dd9652d7f1cdf869be9fee2239bfd43cce45476e3e8340ca95e9fd50ed5d596ede20f0a499780e82582036e16c2ed6f1a467e834936aad7f5d5a3987936d9329e3ef699ae2e416a65f1358405ff16b63963015e1564296b13f14ed5f657d57fef0eafb4ba5d014b79c9d893ba3dd7c510930349a7838a0bcd210c0341575a4933c3a3c6a36988260753c8707825820c8e9bff01f7eaf91f53ff98e5ea4670ba98f73d858ba80107e91ede9911405e358409f4b63ad8de5b01a9eb13255ae075f3a8df95dcf4833f24b82cf3dcaa165a98973de336f84e752bc12768e71a91b0fe3339ea60b38ad909b3ed5ab61822a72018258208359570e262d76a4e57b48b722dc71e9e7b5a866ee0bc9739523c5f1c3b4d176584070e6f4a696affda27ea7acef8836804a7e87de5d899c623ff13844b95b1a2cf423d86c820fa2dca991d39022cf0f7a273cdde4d61e4fe81a8b328947a4acf30e825820acda4ffe2e9038523f8556efe074e3c81e1fa32a1bc6741cb63fbb7ea1826c25584052b0954234d8ebe6311539e280f36ffc2c8aa40bb39aaaa344ed71855628fdd89c5130b816fe789932294c1be779da673d8bbe3989002abcb490d69476a9150f8258202f23960d4302698fe690e18fd0cba07859feebc21d419d85ee0698ef1f42ff8f5840eeda68eb63a82e075c0434d1340e98dd9c2662d5c711c038f709c168e860e0195ac21c06aeb8f109b56919df91769d45ff39a0f80921c92ebd4567d844e6f706825820bb23899b812a62738c28e5e2d86f1751ae097df0c674405d0378981974160e885840590a44566f477460699b880291215c4ad37ad47645f50c6eb4f844a877e76eaecda4d874c3c507edd8ff209e2850f8bd49c450a617e88ac7fd037738681fd409825820d9f10541f4ec9b7fa98be9547144298dff3cf22e4076a953564c7b69489fa1fe58402f14e54a05f7e26ce3b188ce0647c64aff01f8642f1d2a1b304ea4816bc01852f0141092d622590b43ed7dfc97b773fcbefd16e8a43492145b3f814018394c058258202167cfa4624da6ccaebf616f6f7172529d2c1b6f5009ab48ce30b96ae2f42d8b5840e5bb73f60bb71deadc73b050849d5fa1e04567ceb4b18922decd3e4a9e88f66b0c6e9af496c0fb244e8f242eafa5270eb02892967fa5f8b5424a2489cd47c7078258200fc0944d73048732736fd51709474394f15d6a80c1112ea1387363fbe2a84a6558407422265edf385566262680112c3a8cb48b3bac6335a01332e3feb9cd94f9ecb5341893bdb0eb9493967924fb91aa4c318348663f2352ae929518bfc3c0d35204825820f32575c043fa72ef8440ddd330d5bfbf8329e2edec09a1cc73b3a820075be229584022cec5d2470005d2b333bfe17c8dbeb3ff97320412aba6e12ade614bbdb7e03b5270f6328dc267f5d12a2966d96ceacd36d41f8488e46d8f8ba8d74294aa000da080";

        CborReader reader = CborReader.createFromByteArray(decode(cborIndefiniteArrays));
        CborObject obj = reader.readDataItem();
        String cborIndefiniteArraysOutput = encode(obj.toCborByteArray());
        assertEquals(cborIndefiniteArrays, cborIndefiniteArraysOutput);

        reader = CborReader.createFromByteArray(decode(cborFixedArrays));
        obj = reader.readDataItem();
        String cborFixedArraysOutput = encode(obj.toCborByteArray());
        assertEquals(cborFixedArrays, cborFixedArraysOutput);
    }
    @Test
    void testDeepActualBlock() throws Exception {
        byte[] encoded;
        try (InputStream input =
                getClass()
                        .getResourceAsStream(
                                "/com/google/iot/cbor/bad_block_preprod.cbor.hex")) {
            assertNotNull(input);
            encoded =
                    decode(
                            new String(input.readAllBytes(), StandardCharsets.US_ASCII).trim());
        }

        assertEquals(44_439, encoded.length);
        assertEquals(
                "b08d698c90a0f3543848507155cd69d549da509a2ed7ae189920e60e7ef07ff7",
                digest(encoded));

        CborArray outer = (CborArray) CborObject.createFromCborByteArray(encoded);
        assertEquals(2, outer.size());
        assertInteger(outer.listValue().get(0), 4);
        CborByteString taggedPayload = (CborByteString) outer.listValue().get(1);
        assertEquals(CborTag.CBOR_DATA_ITEM, taggedPayload.getTag());

        byte[] payload = flatten(taggedPayload.byteArrayValue());
        assertEquals(44_432, payload.length);
        assertEquals(
                "ca0feb0e4ee2169c4d1baea466b1f128c50b81dab6cb50f9e874cf277a5f6710",
                digest(payload));

        CborArray decoded = (CborArray) CborObject.createFromCborByteArray(payload);
        CborArray converted = taggedPayload.toJavaObject(CborArray.class);
        for (CborArray result : new CborArray[] {decoded, converted}) {
            assertEquals(2, result.size());
            assertInteger(result.listValue().get(0), 7);
            TreeStats stats = treeStats(result);
            assertEquals(11_381, stats.arrays);
            assertEquals(293, stats.maps);
            assertEquals(10_772, stats.maxDepth);
        }
    }

    @Test
    void testDeepCollectionContinuations() throws Exception {
        int depth = 20_000;

        CborObject value = CborObject.createFromCborByteArray(nestedArrays(depth, false));
        for (int i = 0; i < depth; i++) {
            CborArray array = (CborArray) value;
            assertEquals(1, array.size());
            value = array.listValue().get(0);
        }
        assertInteger(value, 0);

        value = CborObject.createFromCborByteArray(nestedArrays(depth, true));
        for (int i = 0; i < depth; i++) {
            CborArray array = (CborArray) value;
            assertEquals(1, array.size());
            value = array.listValue().get(0);
        }
        assertInteger(value, 0);

        value = CborObject.createFromCborByteArray(nestedMapValues(depth, false));
        for (int i = 0; i < depth; i++) {
            CborMap map = (CborMap) value;
            assertEquals(1, map.size());
            Map.Entry<CborObject, CborObject> entry = map.mapValue().get(0);
            assertInteger(entry.getKey(), 0);
            value = entry.getValue();
        }
        assertInteger(value, 0);

        value = CborObject.createFromCborByteArray(nestedMapValues(depth, true));
        for (int i = 0; i < depth; i++) {
            CborMap map = (CborMap) value;
            assertEquals(1, map.size());
            Map.Entry<CborObject, CborObject> entry = map.mapValue().get(0);
            assertInteger(entry.getKey(), 0);
            value = entry.getValue();
        }
        assertInteger(value, 0);

        byte[] mapKeys = new byte[depth * 2 + 1];
        for (int i = 0; i < depth; i++) {
            mapKeys[i] = (byte) 0xa1;
        }
        for (int i = depth + 1; i < mapKeys.length; i++) {
            mapKeys[i] = 1;
        }
        value = CborObject.createFromCborByteArray(mapKeys);
        for (int i = 0; i < depth; i++) {
            CborMap map = (CborMap) value;
            assertEquals(1, map.size());
            Map.Entry<CborObject, CborObject> entry = map.mapValue().get(0);
            assertInteger(entry.getValue(), 1);
            value = entry.getKey();
        }
        assertInteger(value, 0);

        byte[] alternating = new byte[depth * 2 + 1];
        int length = 0;
        for (int i = 0; i < depth; i++) {
            if ((i & 1) == 0) {
                alternating[length++] = (byte) 0x81;
            } else {
                alternating[length++] = (byte) 0xa1;
                alternating[length++] = 0;
            }
        }
        alternating[length++] = 0;
        value =
                CborObject.createFromCborByteArray(
                        java.util.Arrays.copyOf(alternating, length));
        for (int i = 0; i < depth; i++) {
            if ((i & 1) == 0) {
                CborArray array = (CborArray) value;
                assertEquals(1, array.size());
                value = array.listValue().get(0);
            } else {
                CborMap map = (CborMap) value;
                assertEquals(1, map.size());
                Map.Entry<CborObject, CborObject> entry = map.mapValue().get(0);
                assertInteger(entry.getKey(), 0);
                value = entry.getValue();
            }
        }
        assertInteger(value, 0);
    }

    @Test
    void testDeepTagLoopAndIsolation() throws Exception {
        byte[] tags = new byte[20_001];
        java.util.Arrays.fill(tags, 0, 20_000, (byte) 0xc0);
        tags[20_000] = 1;
        CborObject value = CborObject.createFromCborByteArray(tags);
        assertInteger(value, 1);
        assertEquals(0, value.getTag());

        CborArray array = (CborArray) CborObject.createFromCborByteArray(decode("82c00102"));
        assertEquals(0, array.listValue().get(0).getTag());
        assertEquals(CborTag.UNTAGGED, array.listValue().get(1).getTag());
    }

    @Test
    void testDeepStreamBoundaries() throws Exception {
        byte[] item = nestedArrays(20_000, false);
        byte[] sequence = concat(item, new byte[] {2});

        CborReader reader =
                CborReader.createFromInputStream(new ByteArrayInputStream(sequence), 2);
        assertNestedArrays(reader.readDataItem(), 20_000);
        assertEquals(item.length, reader.bytesParsed());
        assertInteger(reader.readDataItem(), 2);
        assertEquals(sequence.length, reader.bytesParsed());
        assertFalse(reader.hasRemainingDataItems());

        byte[] prefixed = new byte[sequence.length + 3];
        System.arraycopy(sequence, 0, prefixed, 3, sequence.length);
        reader = CborReader.createFromByteArray(prefixed, 3, 2);
        assertNestedArrays(reader.readDataItem(), 20_000);
        assertEquals(item.length, reader.bytesParsed());
        assertInteger(reader.readDataItem(), 2);
        assertEquals(sequence.length, reader.bytesParsed());
        assertFalse(reader.hasRemainingDataItems());

        reader =
                CborReader.createFromInputStream(new ByteArrayInputStream(sequence), 1);
        assertNestedArrays(reader.readDataItem(), 20_000);
        assertEquals(item.length, reader.bytesParsed());
        assertFalse(reader.hasRemainingDataItems());

        assertThrows(
                CborParseException.class,
                () -> CborObject.createFromCborByteArray(sequence));
    }

    @Test
    void testDeepFailureUnwinding() {
        int depth = 20_000;
        byte[] definite = nestedArrays(depth, false);
        byte[] indefinite = nestedArrays(depth, true);
        assertThrows(
                CborParseException.class,
                () ->
                        CborObject.createFromCborByteArray(
                                java.util.Arrays.copyOf(definite, definite.length - 1)));
        assertThrows(
                CborParseException.class,
                () ->
                        CborObject.createFromCborByteArray(
                                java.util.Arrays.copyOf(indefinite, indefinite.length - 1)));

        for (String hex : new String[] {"c0", "81ff", "bf00ff", "9fc0ff"}) {
            assertThrows(
                    CborParseException.class,
                    () -> CborObject.createFromCborByteArray(decode(hex)));
        }

        assertThrows(
                CborParseException.class,
                () ->
                        CborObject.createFromCborByteArray(
                                concat(new byte[] {(byte) 0x5f}, definite, new byte[] {(byte) 0xff})));
        assertThrows(
                CborParseException.class,
                () ->
                        CborObject.createFromCborByteArray(
                                concat(new byte[] {(byte) 0x7f}, definite, new byte[] {(byte) 0xff})));
        assertThrows(
                CborParseException.class,
                () ->
                        CborObject.createFromCborByteArray(
                                decode("9b0000000100000000")));
    }

    private static byte[] nestedArrays(int depth, boolean indefinite) {
        byte[] bytes = new byte[indefinite ? depth * 2 + 1 : depth + 1];
        java.util.Arrays.fill(bytes, 0, depth, indefinite ? (byte) 0x9f : (byte) 0x81);
        if (indefinite) {
            java.util.Arrays.fill(bytes, depth + 1, bytes.length, (byte) 0xff);
        }
        return bytes;
    }

    private static byte[] nestedMapValues(int depth, boolean indefinite) {
        byte[] bytes = new byte[indefinite ? depth * 3 + 1 : depth * 2 + 1];
        int offset = 0;
        for (int i = 0; i < depth; i++) {
            bytes[offset++] = indefinite ? (byte) 0xbf : (byte) 0xa1;
            bytes[offset++] = 0;
        }
        bytes[offset++] = 0;
        if (indefinite) {
            java.util.Arrays.fill(bytes, offset, bytes.length, (byte) 0xff);
        }
        return bytes;
    }

    private static void assertNestedArrays(CborObject value, int depth) {
        for (int i = 0; i < depth; i++) {
            CborArray array = (CborArray) value;
            assertEquals(1, array.size());
            value = array.listValue().get(0);
        }
        assertInteger(value, 0);
    }

    private static void assertInteger(CborObject value, long expected) {
        assertInstanceOf(CborInteger.class, value);
        assertEquals(expected, ((CborInteger) value).longValue());
    }

    private static TreeStats treeStats(CborObject root) {
        int arrays = 0;
        int maps = 0;
        int maxDepth = 0;
        ArrayDeque<TreeNode> pending = new ArrayDeque<>();
        pending.push(new TreeNode(root, 1));
        while (!pending.isEmpty()) {
            TreeNode node = pending.pop();
            if (node.value instanceof CborArray array) {
                arrays++;
                maxDepth = Math.max(maxDepth, node.depth);
                for (CborObject child : array.listValue()) {
                    pending.push(new TreeNode(child, node.depth + 1));
                }
            } else if (node.value instanceof CborMap map) {
                maps++;
                maxDepth = Math.max(maxDepth, node.depth);
                for (Map.Entry<CborObject, CborObject> entry : map.mapValue()) {
                    pending.push(new TreeNode(entry.getKey(), node.depth + 1));
                    pending.push(new TreeNode(entry.getValue(), node.depth + 1));
                }
            }
        }
        return new TreeStats(arrays, maps, maxDepth);
    }

    private static byte[] flatten(byte[][] segments) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] segment : segments) {
            output.writeBytes(segment);
        }
        return output.toByteArray();
    }

    private static byte[] concat(byte[]... arrays) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            output.writeBytes(array);
        }
        return output.toByteArray();
    }

    private static String digest(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record TreeNode(CborObject value, int depth) {}

    private record TreeStats(int arrays, int maps, int maxDepth) {}
}
