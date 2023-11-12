package io.github.vcvitaly;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Streams;
import okio.Options;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * ${NAME}.
 *
 * @author Vitalii Chura
 */
public class Main {
    public static void main(String[] args) throws IOException, ApiException, InterruptedException {
        final var namespace = "default";
        final var podName = "nginx-785c6c55c8-bcw99";
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        Exec exec = new Exec();
        boolean tty = System.console() != null;
        // final Process proc = exec.exec("default", "nginx-4217019353-k5sn9", new String[]
        //   {"sh", "-c", "echo foo"}, true, tty);
        final Process proc =
                exec.exec(namespace, podName, new String[] {"sh"}, true, tty);

        Thread in =
                new Thread(
                        () -> {
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                                String line;
                                while (!(line = br.readLine()).equalsIgnoreCase("exit")) {
                                    Streams.copy(new ByteArrayInputStream(line.getBytes()), proc.getOutputStream());
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });
        in.start();

        Thread out =
                new Thread(
                        () -> {
                            try {
                                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        System.out.println(line);
                                    }
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });
        out.start();

        proc.waitFor();

        // wait for any last output; no need to wait for input thread
        out.join();

        proc.destroy();

        System.exit(proc.exitValue());
    }

    public static void listPods() throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listNamespacedPod(
                "default", null, null, null, null,
                null, null, null, null, null, null
        );
        for (V1Pod item : list.getItems()) {
            System.out.println(item.getMetadata().getName());
        }
    }
}