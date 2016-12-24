package openmods.depcheck.log;

import com.google.common.collect.Maps;
import org.slf4j.ILoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory used to create the various loggers.
 *
 * @author TheSilkMiner
 * @since 0.1
 */
public class LoggerFactory implements ILoggerFactory {

	private final ConcurrentMap<String, org.slf4j.Logger> LOGGER_MAP;

	public LoggerFactory() {
		this.LOGGER_MAP = Maps.newConcurrentMap();
		java.util.logging.Logger.getLogger("");
	}

	@Override
	public org.slf4j.Logger getLogger(final String name) {
		final String realName = Objects.equals(name, org.slf4j.Logger.ROOT_LOGGER_NAME)? "" : name;
		final org.slf4j.Logger logger = this.LOGGER_MAP.get(realName);

		if (logger != null)	return logger;

		final Logger toWrap = Logger.of(name);
		final org.slf4j.Logger newInstance = new LoggerAdapter(toWrap);
		final org.slf4j.Logger oldInstance = this.LOGGER_MAP.putIfAbsent(name, newInstance);
		return oldInstance == null? newInstance : oldInstance;
	}
}
