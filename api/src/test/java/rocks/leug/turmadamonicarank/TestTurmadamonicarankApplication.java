package rocks.leug.turmadamonicarank;

import org.springframework.boot.SpringApplication;

public class TestTurmadamonicarankApplication {

	public static void main(String[] args) {
		SpringApplication.from(TurmadamonicarankApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
