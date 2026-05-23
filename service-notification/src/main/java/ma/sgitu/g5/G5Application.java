package ma.sgitu.g5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class G5Application {

	public static void main(String[] args) {
		SpringApplication.run(G5Application.class, args);
	}

}
