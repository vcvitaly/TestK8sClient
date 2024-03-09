package io.github.vcvitaly;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Streams;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class LooplessMain {

    public static void main(String[] args) throws IOException, ApiException, InterruptedException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        Exec exec = new Exec();
        boolean tty = System.console() != null;
        final Process proc =
                exec.exec("default", "nginx-6748cb78-7fddb66f44-v8kjt", new String[] {"sh"}, true, tty);

        Thread.ofVirtual().start(() -> {
            try (
                    final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()))
            ) {
                Thread.sleep(3);
                bw.write("ls /home\n");
//                Thread.sleep(5);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            waitAndLog("Exiting the in thread");
        });

        final Thread out = Thread.ofVirtual().start(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                List<String> lines = new ArrayList<>();
                int i = 0;
                while (true) {
                    if (br.ready()) {
                        String line = br.readLine();
                        lines.add(line);
//                        waitAndLog("if");
                    } else {
                        if (!lines.isEmpty()) {
                            lines.forEach(System.out::println);
                            lines = new ArrayList<>();
                        }
                        Thread.sleep(1);
                        i++;
                        if (i >= 10) {
                            break;
                        }
//                        waitAndLog("else");
                    }
                }
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
            waitAndLog("Exiting the out thread");
        });

        proc.waitFor();
        waitAndLog("Finished proc.waitFor()");

        // wait for any last output; no need to wait for input thread
        out.join();
        waitAndLog("Finished out.join()");

        proc.destroy();
        waitAndLog("Finished proc.destroy()");

        System.exit(proc.exitValue());
    }

    private static void waitAndLog(String s) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(s);
    }
}
