package openmods.depcheck.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Class used to start benchmarks to test the entire time of running of a software.
 *
 * <p>This class also provides possibility to output error messages when the process
 * has taken too long to complete.</p>
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public final class Benchmark {

	private static final Map<String, Benchmark> CURRENT = Maps.newHashMap();
	private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);

	private final String process;
	private final double warningThreshold;
	private final double errorThreshold;

	private long begin;
	private boolean running;
	private long end;

	private Benchmark(final String process, final double warningThreshold, final double errorThreshold) {
		this.process = process;
		this.warningThreshold = warningThreshold * 1000;
		this.errorThreshold = errorThreshold * 1000;
	}

	/**
	 * Creates a new benchmark.
	 *
	 * <p>If a benchmark has already been created for a certain process and that benchmark
	 * is currently running, then an {@link Optional#empty() empty <code>Optional</code>} is returned,
	 * otherwise a wrapped {@link Benchmark} instance.</p>
	 *
	 * @param process
	 * 		The process ID.
	 * @param warningThreshold
	 * 		The warning threshold (in seconds).
	 * @param errorThreshold
	 * 		The error threshold (in seconds).
	 * @return
	 * 		The benchmark optional.
	 *
	 * @since 1.0
	 */
	@Nonnull
	public static Optional<Benchmark> create(@Nonnull final String process, final double warningThreshold, final double errorThreshold) {
		LOGGER.info("Requested benchmark for process " + Preconditions.checkNotNull(process, "process"));
		if (CURRENT.get(process) != null && CURRENT.get(process).isRunning()) {
			LOGGER.warn("Attempted to generate benchmark for already running session.");
			return Optional.empty();
		}
		return Optional.of(new Benchmark(process, warningThreshold, errorThreshold));
	}

	public void begin() {
		if (this.running) {
			LOGGER.warn("Attempted to start already in progress benchmark. This is a serious programming error");
			return;
		}
		if (CURRENT.get(process) != null) {
			LOGGER.warn("A benchmark is already in progress for the given process. This is a serious programming error");
			return;
		}
		LOGGER.info("Started benchmark for process " + this.process);
		CURRENT.put(process, this);
		this.begin = System.currentTimeMillis();
		this.running = true;
	}

	@SuppressWarnings("WeakerAccess")
	public boolean isRunning() {
		return this.running;
	}

	public void end() {
		this.end(true);
	}

	@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
	public void end(final boolean log) {
		if (!this.running) {
			LOGGER.warn("Attempted to stop not started benchmark. This is a serious programming error.");
			return;
		}
		if (CURRENT.get(process) == null) {
			LOGGER.warn("A benchmark was already stopped for the given process. This is a serious programming error");
			return;
		}
		LOGGER.info("Stopped benchmark for process " + this.process);
		CURRENT.put(process, null);
		this.end = System.currentTimeMillis();
		this.running = false;
		if (log) this.log();
	}

	private void log() {
		final long microTime = this.end - this.begin;
		final double seconds = ((double) microTime) / 1000;
		LOGGER.info(String.format("Process %s completed in %g seconds (%d micro-seconds)", this.process, seconds, microTime));

		if (microTime >= this.errorThreshold) {
			LOGGER.error("The process took way too long to complete! Please check your current system and report this to the developer");
			LOGGER.error("Expected threshold was: " + this.errorThreshold);
			LOGGER.error("Completion time was: " + microTime);
			return;
		}
		if (microTime >= this.warningThreshold) {
			LOGGER.warn("The process took a longer time to complete! Please check your current system");
			LOGGER.warn("Expected threshold was: " + this.warningThreshold);
			LOGGER.warn("Completion time was: " + microTime);
			return;
		}
		LOGGER.info("Everything has been completed normally");
	}
}
