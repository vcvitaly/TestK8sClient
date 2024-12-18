package io.github.vcvitaly;

import io.kubernetes.client.Copy;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Streams;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CopyMain {
    public static void main(String[] args) throws Exception {
        String podName = "nginx-756cc597f6-rjmgk";
        String namespace = "default";

        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        Copy copy = new Copy();
        copy.copyFileToPod();
        InputStream dataStream = copy.copyFileFromPod(namespace, podName, "/root/file.txt");
        final OutputStream outputStream = Files.newOutputStream(Path.of("C:\\Users\\vcvit\\DEV\\file.txt"));
        Streams.copy(dataStream, outputStream);
        System.exit(0);
    }
}
