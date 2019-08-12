package test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.Json;
import lombok.Cleanup;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.git.ml.Daemon.CONFIG;

/**
 * @author jzb 2019-08-10
 */
public class InitConfig {
    public static void main(String[] args) {
//        test();
        poy();
        fdy();
    }

    @SneakyThrows
    private static void poy() {
        final ObjectNode objectNode = Json.mapper.createObjectNode()
                .put("host", "192.168.0.38")
                .put("username", "admin")
                .put("password", "tomking")
                .put("clientProvidedName", "mes-auto-ml-sender-POY")
                .put("product", "POY")
                .put("watchDir", "d:/image");
        write(objectNode, "/home/jzb/git/org.jzb/mes-auto-ml/sender/daemon/poy.data");
    }

    @SneakyThrows
    private static void fdy() {
        final ObjectNode objectNode = Json.mapper.createObjectNode()
                .put("host", "192.168.0.38")
                .put("username", "admin")
                .put("password", "tomking")
                .put("clientProvidedName", "mes-auto-ml-sender-FDY")
                .put("product", "FDY")
                .put("watchDir", "d:/image");
        write(objectNode, "/home/jzb/git/org.jzb/mes-auto-ml/sender/daemon/fdy.data");
    }

    @SneakyThrows
    private static void test() {
        final ObjectNode objectNode = Json.mapper.createObjectNode()
                .put("host", "192.168.0.38")
                .put("username", "admin")
                .put("password", "tomking")
                .put("clientProvidedName", "mes-auto-ml-sender")
                .put("product", "FDY")
                .put("watchDir", "D:/daemon/watchDirTest");
        write(objectNode, "/home/jzb/git/org.jzb/mes-auto-ml/sender/daemon/test.data");
    }

    public static void write(ObjectNode objectNode, String file) throws Exception {
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("des");
        final Cipher cipher = Cipher.getInstance("des");
        final byte[] keyBytes = UUID.nameUUIDFromBytes(CONFIG.getBytes(UTF_8)).toString().getBytes(UTF_8);
        final DESKeySpec keySpec = new DESKeySpec(keyBytes);
        final SecretKey secretKey = keyFactory.generateSecret(keySpec);
        final byte[] bytes = Json.mapper.writeValueAsBytes(objectNode);
        @Cleanup final FileOutputStream fos = new FileOutputStream(file);
        @Cleanup final ObjectOutputStream oos = new ObjectOutputStream(fos);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new SecureRandom());
        byte[] cipherData = cipher.doFinal(bytes);
        oos.writeObject(keyBytes);
        oos.writeObject(cipherData);
    }
}
