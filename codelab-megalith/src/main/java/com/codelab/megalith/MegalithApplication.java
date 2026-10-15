package com.codelab.megalith;

import com.codelab.core.task.CodelabTaskRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(
    exclude = {
      HibernateJpaAutoConfiguration.class,
      DataJpaRepositoriesAutoConfiguration.class,
      DataSourceAutoConfiguration.class
    })
public class MegalithApplication {

  static void main(String[] args) {
    SpringApplication application = new SpringApplication(MegalithApplication.class);

    // A selected task (--codelab.task=<name>) runs headless: no web server, one-shot, and the JVM
    // exits with the task's code. A normal boot (no selector) stays a web application.
    boolean headless = CodelabTaskRunner.isRequested(args);
    if (headless) {
      application.setWebApplicationType(WebApplicationType.NONE);
    }

    ConfigurableApplicationContext context = application.run(args);

    if (headless) {
      System.exit(
          SpringApplication.exit(
              context, () -> context.getBean(CodelabTaskRunner.class).getExitCode()));
    }
  }
}
