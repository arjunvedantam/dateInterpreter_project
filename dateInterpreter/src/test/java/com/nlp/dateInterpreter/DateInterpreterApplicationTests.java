package com.nlp.dateInterpreter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.nlp.dateInterpreter.repository.DateInterpreterRepository;

@SpringBootTest(properties = {
		"server.port=9600",
		"nl.model.api-key=test-key",
		"nl.model.endpoint=https://api.openai.com/v1/chat/completions",
		"nl.model.name=gpt-4o-mini",
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
				"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class DateInterpreterApplicationTests {

	@MockBean
	private DateInterpreterRepository dateInterpreterRepository;

	@Test
	void contextLoads() {
	}

}
