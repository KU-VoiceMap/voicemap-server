package org.ku.voicemap.domain.ephemeralToken.service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

@Component
public class PythonExecutor {

    @Value("${python.script.resource-path}")
    private Resource pythonScriptResource;

    private String tempScriptPath;

    @PostConstruct
    public void initializeScript() {
        try (InputStream inputStream = pythonScriptResource.getInputStream()) {

            File tempFile = File.createTempFile("asdasdasd_script", ".py");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                FileCopyUtils.copy(inputStream, outputStream);
            }
            if (!tempFile.setExecutable(true)) {
                throw new IllegalStateException();
            }
            tempFile.deleteOnExit();

            this.tempScriptPath = tempFile.getAbsolutePath();


        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    public String createGoogleAuthToken(String apiKey, int uses, int expireMinutes, int sessionExpireMinutes) {

        if (this.tempScriptPath == null) {
            throw new IllegalStateException();
        }

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command(
            "python",
            this.tempScriptPath,
            "--api-key", apiKey,
            "--uses", String.valueOf(uses),
            "--expire-mins", String.valueOf(expireMinutes),
            "--session-expire-mins", String.valueOf(sessionExpireMinutes)
        );

        processBuilder.redirectErrorStream(false);
        Process process;
        String output;
        int exitCode;

        try {
            process = processBuilder.start();

            output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.joining("\n"));

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("Python 실행 시간 초과 ");
            }

            exitCode = process.exitValue();
        } catch (Exception e) {
            throw new RuntimeException("Python 예외 발생");
        }

        if (exitCode != 0) {
            throw new RuntimeException("Python 실행 실패 ");
        }

        return output;
    }
}
