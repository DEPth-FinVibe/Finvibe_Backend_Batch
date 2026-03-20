package depth.finvibe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
	classes = FinvibeApplication.class,
	properties = "langchain4j.google-ai-gemini.chat-model.api-key=test"
)
@ActiveProfiles("test")
class FinvibeApplicationTests {

	@Test
	void contextLoads() {
	}

}
