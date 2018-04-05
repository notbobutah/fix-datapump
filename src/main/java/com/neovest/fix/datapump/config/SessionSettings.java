package com.neovest.fix.datapump.config;

import com.neovest.fix.datapump.quickfix.Executor;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.InputStream;

@Component
public class SessionSettings extends quickfix.SessionSettings {

    public SessionSettings() throws Exception {
        super("executor.cfg");
    }

    public void getSettings() throws Exception {
        InputStream inputStream = getSettingsInputStream();
        quickfix.SessionSettings settings = new quickfix.SessionSettings(inputStream);
        inputStream.close();

    }

    private static InputStream getSettingsInputStream() throws FileNotFoundException {
        InputStream inputStream = null;
        inputStream = Executor.class.getResourceAsStream("/executor.cfg");
        if (inputStream == null) {
            System.out.println("usage: " + Executor.class.getName() + " [configFile].");
            System.exit(1);
        }
        return inputStream;
    }

}
