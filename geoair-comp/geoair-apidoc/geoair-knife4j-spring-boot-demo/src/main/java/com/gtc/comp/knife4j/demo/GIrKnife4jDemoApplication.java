package com.gtc.comp.knife4j.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
// @ComponentScan(value = "com.gtc")
public class GIrKnife4jDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GIrKnife4jDemoApplication.class, args);
		// 是否启用swagger 在
		// 启动后访问 localhost:port/doc 即可看到接口文档页面
		// 注意 springBoot 2.4.0 以上的版本，application.yml配置文件中的 matching-strategy
		// 要修改成ant_path_matcher

	}

	@Component
	class OpenBrowser implements CommandLineRunner {

		@Override
		public void run(String... args) throws Exception {
			try {
				Runtime.getRuntime().exec("cmd   /c   start   http://localhost:8080/doc.html");// 可以指定自己的路径
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

}
