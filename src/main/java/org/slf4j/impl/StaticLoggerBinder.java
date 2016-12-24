package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

import openmods.depcheck.log.LoggerFactory;

/**
 * Binder to our custom logging.
 *
 * <p>This class is an hack: it should not be emulated in other
 * projects and/or source codes. Please do <b>NOT</b> try to
 * implement a similar algorithm in another software. This class
 * shouldn't even exist!</p>
 *
 * <p><strong><center>WARNING!</center></strong><br />
 * This class is duplicated in the classpath so it is normal that
 * SLF4J outputs errors while in IDE.<br />
 * After building, though, this class should be the only one
 * present, having it replaced the JDK-14 implementation. This
 * process is highly dependent on the order the gradle tasks
 * are run. Please make sure that everything is correct before
 * compiling and/or running this software.<br />
 * Also, try to not edit the {@code build.gradle} file for this
 * exact purpose. If you do, make sure that the resulting jar
 * files all have this class as the <b>unique</b>
 * {@linkplain StaticLoggerBinder} class, thank you.</p>
 *
 * @author TheSilkMiner
 * @since 0.1
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

	private static StaticLoggerBinder SINGLETON;

	@SuppressWarnings("unused")
	public static String REQUESTED_API_VERSION = "1.6.99";

	private ILoggerFactory loggerFactory;

	private StaticLoggerBinder() {
		this.loggerFactory = new LoggerFactory();
	}

	@SuppressWarnings("WeakerAccess")
	public static StaticLoggerBinder getSingleton() {
		if (SINGLETON == null) SINGLETON = new StaticLoggerBinder();
		return SINGLETON;
	}

	public static void disableVariedLogging() {
		getSingleton().loggerFactory = new JDK14LoggerFactory();
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return this.loggerFactory;
	}

	/*
	 * (non-Javadoc)
	 *
	 * Please do not override this method in order to return a certain constant value.
	 * This is set-up as is explicitly to allow the entire hack this class is.
	 * Thank you for your comprehension.
	 */
	@Override
	public String getLoggerFactoryClassStr() {
		return this.getLoggerFactory().getClass().getName();
	}
}
