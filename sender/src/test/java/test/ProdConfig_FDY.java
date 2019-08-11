package test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.Json;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;

/**
 * @author jzb 2019-08-10
 */
public class ProdConfig_FDY {
    @SneakyThrows
    public static void main(String[] args) {
        final ObjectNode objectNode = Json.mapper.createObjectNode()
                .put("host", "192.168.0.38")
                .put("username", "admin")
                .put("password", "tomking")
                .put("clientProvidedName", "mes-auto-ml-sender-FDY")
                .put("product", "FDY")
                .put("watchDir", "/tmp/watchDirTest");
        @Cleanup final FileInputStream fis = new FileInputStream("/tmp/oos/test.data");
        @Cleanup final ObjectInputStream ois = new ObjectInputStream(fis);
        final byte[] bytes = ois.readAllBytes();
        final Map map = Json.mapper.readValue(bytes, Map.class);
        System.out.println(map);
    }
}
