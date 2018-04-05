package com.neovest.fix.datapump;

import com.neovest.fix.datapump.quickfix.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import quickfix.SessionSettings;

import java.io.InputStream;

@SpringBootApplication
@ComponentScan("com.neovest.fix.datapump")
public class DatapumpApplication {
	Logger log = LoggerFactory.getLogger(this.getClass().getName());

	static Executor executor;


	public static void main(String[] args) throws Exception {
		InputStream inputStream = executor.getSettingsInputStream(args);
		SessionSettings settings = new SessionSettings(inputStream);
		inputStream.close();

		executor = new Executor(settings);
		executor.start();

		SpringApplication.run(DatapumpApplication.class, args);
	}
}
