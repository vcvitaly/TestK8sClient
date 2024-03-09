package io.github.vcvitaly;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class LooplessMainExec {

    public static void main(String[] args) throws IOException, ApiException, InterruptedException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        Exec exec = new Exec();

        System.out.println(executeInProc(exec, new String[]{"ls", "/home"}));
        System.out.println(executeInProc(exec, new String[]{"ls", "/home"}));
//        Thread.sleep(1000);
//        System.out.println(executeInProc(proc, "ls /home"));
    }

    private static List<String> executeInProc(Exec exec, String[] cmdParts) throws IOException, ApiException, InterruptedException {
        boolean tty = System.console() != null;
        final Process proc =
                exec.exec("default", "nginx-6748cb78-7fddb66f44-v8kjt", cmdParts, true, tty);
        final var ref = new Object() {
            List<String> lines = new ArrayList<>();
        };

        final Thread out = Thread.ofVirtual().start(() -> {
            final InputStream inputStream = proc.getInputStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                int i = 0;
                while (true) {
                    if (br.ready()) {
                        String line = br.readLine();
                        ref.lines.add(line);
//                        waitAndLog("if");
                    } else {
                        /*if (!ref.lines.isEmpty()) {
                            ref.lines.forEach(System.out::println);
                            ref.lines = new ArrayList<>();
                        }*/
                        Thread.sleep(1);
                        i++;
                        if (i >= 250) {
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

        out.join();
        waitAndLog("Finished out.join()");

        proc.destroy();
        waitAndLog("Finished proc.destroy()");

        System.out.println("Exit code: " + proc.exitValue());

        return ref.lines;
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
