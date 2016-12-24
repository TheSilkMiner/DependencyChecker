package openmods.depcheck.log;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Custom logging class used to provide better formatting
 * and redirect everything to the correct streams.
 *
 * <p>This behaviour can be replaced and instead the default
 * logging handler be used if the switch is enabled.</p>
 *
 * @author TheSilkMiner
 * @since 0.1
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Logger {

	private static class LogOutputHandler extends Handler {

		private static class LogFormatter extends Formatter {

			private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

			@Override
			public String format(final LogRecord record) {
				final StringBuilder builder = new StringBuilder();

				builder.append(dateFormat.format(new Date(record.getMillis())));
				builder.append(" [");
				builder.append(Thread.currentThread().getName());
				builder.append("] [");

				final String sourceClassName = record.getSourceClassName();

				builder.append(sourceClassName.substring(sourceClassName
						.lastIndexOf('.') + 1));
				builder.append("] [");
				builder.append(record.getLevel());
				builder.append("] ");
				builder.append(this.formatMessage(record));
				builder.append("\n");


				return builder.toString();
			}
		}

		@Override
		public void publish(final LogRecord record) {
			if (this.getFormatter() == null) {
				this.setFormatter(new LogFormatter());
			}

			try {
				final String message = this.getFormatter().format(record);

				if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
					System.err.write(message.getBytes());
				} else {
					System.out.write(message.getBytes());
				}
			} catch (final Exception exception) {
				this.reportError(null, exception, ErrorManager.FORMAT_FAILURE);
			}

		}

		@Override
		public void close() throws SecurityException {}

		@Override
		public void flush() {}
	}

	private static final Map<String, Logger> LOGGERS = Maps.newHashMap();

	private final java.util.logging.Logger logger;

	private Logger(@Nonnull final String name) {
		this.logger = java.util.logging.Logger.getLogger(name);
		this.logger.setUseParentHandlers(false);
		this.logger.addHandler(new LogOutputHandler());
		//this.addFileHandlerLogger(); //TODO Implement logging file correctly
	}

	public static Logger of(@Nonnull final String name) {
		Preconditions.checkNotNull(name, "name");
		if (!LOGGERS.containsKey(name)) LOGGERS.put(name, new Logger(name));
		return LOGGERS.get(name);
	}

	@Contract(pure = true)
	@Nonnull
	final java.util.logging.Logger accessInternalLogger() {
		return this.logger;
	}

	public void trace(@Nonnull final String msg) {
		this.log(Level.ALL, msg, null);
	}

	public void finest(@Nonnull final String msg) {
		this.log(Level.FINEST, msg, null);
	}

	public void finer(@Nonnull final String msg) {
		this.log(Level.FINER, msg, null);
	}

	public void fine(@Nonnull final String msg) {
		this.log(Level.FINE, msg, null);
	}

	public void config(@Nonnull final String msg) {
		this.log(Level.CONFIG, msg, null);
	}

	public void info(@Nonnull final String msg) {
		this.log(Level.INFO, msg, null);
	}

	public void warning(@Nonnull final String msg, @Nullable final Throwable t) {
		this.log(Level.WARNING, msg, t);
	}

	public void severe(@Nonnull final String msg, @Nullable final Throwable t) {
		this.log(Level.SEVERE, msg, t);
	}

	void internalLog(@Nonnull final Level level, @Nonnull final String msg, @Nullable final Throwable t) {
		this.log(level, msg, t);
	}

	private void log(@Nonnull final Level level, @Nonnull final String msg, @Nullable final Throwable t) {
		if (!this.logger.isLoggable(level)) return;

		final LogRecord record = new LogRecord(level, msg);
		record.setThrown(t);
		record.setLoggerName(this.logger.getName());
		this.logger.log(record);
	}

	@SuppressWarnings("unused")
	private void addFileHandlerLogger() {
		try {
			final String name = new File(System.getProperty("user.dir"), "latest.log").getAbsolutePath();
			final FileHandler fileHandler = new FileHandler(name);
			fileHandler.setFormatter(new LogOutputHandler.LogFormatter());
			this.logger.addHandler(fileHandler);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}
}
