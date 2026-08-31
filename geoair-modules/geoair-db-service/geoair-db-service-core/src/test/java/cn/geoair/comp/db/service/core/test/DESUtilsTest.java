//package cn.geoair.comp.db.service.core.test;
//
//import static org.junit.Assert.*;
//
//import cn.geoair.comp.db.service.core.basic.util.DESUtils;
//import org.junit.Test;
//
///**
// * {@link DESUtils} 的单元测试。
// */
//public class DESUtilsTest {
//
//    @Test
//    public void testEncryptDecrypt() throws Exception {
//        String original = "root123456";
//        String encrypted = DESUtils.encrypt(original);
//        assertNotNull(encrypted);
//        assertFalse(encrypted.isEmpty());
//
//        String decrypted = DESUtils.decrypt(encrypted);
//        assertEquals(original, decrypted);
//    }
//
//    @Test
//    public void testDecryptNull() throws Exception {
//        assertNull(DESUtils.decrypt(null));
//    }
//
//    @Test
//    public void testEncryptEmpty() throws Exception {
//        String encrypted = DESUtils.encrypt("");
//        assertNotNull(encrypted);
//        assertEquals("", DESUtils.decrypt(encrypted));
//    }
//}
