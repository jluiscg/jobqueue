package com.lcortes.jobqueue;

import com.lcortes.jobqueue.engine.WorkerPool;
import com.lcortes.jobqueue.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.autoconfigure.exclude=" +
				"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
				"org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration," +
				"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
)
class DemoApplicationTests {

	@MockitoBean
	private JobRepository jobRepository;

	@MockitoBean
	private WorkerPool workerPool;

	@Test
	void contextLoads() {
	}

}
