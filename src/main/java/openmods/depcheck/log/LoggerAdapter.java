package openmods.depcheck.log;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import java.util.logging.Level;

/**
 * Adapter for {@link Logger} in order to be used with SLF4J.
 *
 * @author TheSilkMiner
 * @since 0.1
 */
public class LoggerAdapter extends MarkerIgnoringBase {

	private final transient Logger logger;

	LoggerAdapter(final Logger logger) {
		this.logger = logger;
		this.name = logger.accessInternalLogger().getName();
	}

	@Override
	public boolean isTraceEnabled() {
		return this.logger.accessInternalLogger().isLoggable(Level.ALL);
	}

	@Override
	public void trace(final String msg) {
		this.logger.trace(msg);
	}

	@Override
	public void trace(final String format, final Object arg) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.ALL)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg);
		this.logger.internalLog(Level.ALL, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void trace(final String format, final Object arg1, final Object arg2) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.ALL)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
		this.logger.internalLog(Level.ALL, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void trace(final String format, final Object... arguments) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.ALL)) return;
		final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
		this.logger.internalLog(Level.ALL, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void trace(final String msg, final Throwable t) {
		this.logger.internalLog(Level.ALL, msg, t);
	}

	@Override
	public boolean isDebugEnabled() {
		return this.logger.accessInternalLogger().isLoggable(Level.FINE);
	}

	@Override
	public void debug(final String msg) {
		this.logger.fine(msg);
	}

	@Override
	public void debug(final String format, final Object arg) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.FINE)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg);
		this.logger.internalLog(Level.FINE, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void debug(final String format, final Object arg1, final Object arg2) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.FINE)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
		this.logger.internalLog(Level.FINE, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void debug(final String format, final Object... arguments) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.FINE)) return;
		final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
		this.logger.internalLog(Level.FINE, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void debug(final String msg, final Throwable t) {
		this.logger.internalLog(Level.FINE, msg, t);
	}

	@Override
	public boolean isInfoEnabled() {
		return this.logger.accessInternalLogger().isLoggable(Level.INFO);
	}

	@Override
	public void info(final String msg) {
		this.logger.info(msg);
	}

	@Override
	public void info(final String format, final Object arg) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.INFO)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg);
		this.logger.internalLog(Level.INFO, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void info(final String format, final Object arg1, final Object arg2) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.INFO)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
		this.logger.internalLog(Level.INFO, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void info(final String format, final Object... arguments) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.INFO)) return;
		final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
		this.logger.internalLog(Level.INFO, tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void info(final String msg, final Throwable t) {
		this.logger.internalLog(Level.INFO, msg, t);
	}

	@Override
	public boolean isWarnEnabled() {
		return this.logger.accessInternalLogger().isLoggable(Level.WARNING);
	}

	@Override
	public void warn(final String msg) {
		this.logger.warning(msg, null);
	}

	@Override
	public void warn(final String format, final Object arg) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.WARNING)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg);
		this.logger.warning(tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void warn(final String format, final Object... arguments) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.WARNING)) return;
		final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
		this.logger.warning(tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void warn(final String format, final Object arg1, final Object arg2) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.WARNING)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
		this.logger.warning(tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void warn(final String msg, final Throwable t) {
		this.logger.warning(msg, t);
	}

	@Override
	public boolean isErrorEnabled() {
		return this.logger.accessInternalLogger().isLoggable(Level.SEVERE);
	}

	@Override
	public void error(final String msg) {
		this.logger.severe(msg, null);
	}

	@Override
	public void error(final String format, final Object arg) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.SEVERE)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg);
		this.logger.severe(tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void error(final String format, final Object arg1, final Object arg2) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.SEVERE)) return;
		final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
		this.logger.severe(tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void error(final String format, final Object... arguments) {
		if (!this.logger.accessInternalLogger().isLoggable(Level.SEVERE)) return;
		final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
		this.logger.severe(tuple.getMessage(), tuple.getThrowable());
	}

	@Override
	public void error(final String msg, final Throwable t) {
		this.logger.severe(msg, t);
	}
}
