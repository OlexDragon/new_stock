package irt.components;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ComponentsApp {
    public static void main(String[] args) {

    	if(System.getProperty("os.name").startsWith("Windows"))
    		System.setProperty("spring.config.name", "application-w");
    	else
    		System.setProperty("spring.config.name", "application-l");

    	SpringApplication.run(ComponentsApp.class, args);
    }
}