package depth.finvibe.batch.config;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledBatchJobSupport {

	private final JobLauncher jobLauncher;

	public void launch(Job job) {
		JobParameters jobParameters = new JobParametersBuilder()
			.addLong("triggeredAt", Instant.now().toEpochMilli())
			.toJobParameters();

		try {
			jobLauncher.run(job, jobParameters);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to launch batch job: " + job.getName(), ex);
		}
	}
}
