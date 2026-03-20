package depth.finvibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
	scanBasePackages = "depth.finvibe",
	nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@ComponentScan(
	basePackages = "depth.finvibe",
	nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
	excludeFilters = {
	@ComponentScan.Filter(
			type = FilterType.REGEX,
			pattern = "depth\\.finvibe\\.BackendApplication"
		)
	}
)
@EnableKafka
@EnableScheduling
@EnableAsync
@EnableJpaAuditing
@ConfigurationPropertiesScan(basePackages = "depth.finvibe")
public class FinvibeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinvibeApplication.class, args);
	}

}
