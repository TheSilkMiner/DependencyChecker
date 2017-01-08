package openmods.depcheck;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import openmods.depcheck.dependencies.DependencyCollector;
import openmods.depcheck.dependencies.DependencyResolveResult;
import openmods.depcheck.parser.SourceDependencies;
import openmods.depcheck.parser.SourceParser;
import openmods.depcheck.parser.TargetParser;
import openmods.depcheck.printer.IConfigurablePrinter;
import openmods.depcheck.printer.IPrinter;
import openmods.depcheck.printer.ResultPrinter;
import openmods.depcheck.utils.AliasesMap;
import openmods.depcheck.utils.Benchmark;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DependencyChecker {

	private static final class Parameters {
		private List<String> directories;
		private String printer;
		private String output;
		private boolean noCache;
		private boolean disableMatcherFail;
		private String printerSettings;
		private boolean disableVariedLogging;
		private boolean help;

		// Mainly for debug purposes
		@Nonnull
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("directories", this.directories)
					.add("printer", this.printer)
					.add("output", this.output)
					.add("noCache", this.noCache)
					.add("disableMatcherFail", this.disableMatcherFail)
					.add("printerSettings", this.printerSettings)
					.add("disableVariedLogging", this.disableVariedLogging)
					.add("help", this.help)
					.toString();
		}
	}

	private static final String VERSION = "0.1";

	private Logger logger;

    public static void main(final String... args) {
    	new Thread(() -> new DependencyChecker().run(args)).start();
    }

    private void run(@Nonnull final String... args) {
    	Thread.currentThread().setName("Software Thread");
    	final Parameters arguments = this.parseArguments(args);

	    if (arguments.disableVariedLogging) StaticLoggerBinder.disableVariedLogging();

	    this.logger = LoggerFactory.getLogger(DependencyChecker.class);
	    this.logger.info("Currently running DependencyChecker {} from {} with {}", VERSION, System.getProperty("user.dir"), arguments);

	    if (arguments.help) {
	    	this.showHelp();
	    	return;
	    }

	    this.execute(arguments);
    }

    @Contract(value = "!null -> !null; null -> fail", pure = true)
    @Nonnull
    private Parameters parseArguments(@Nonnull final String... args) {
    	final Benchmark benchmark = Benchmark.create("Parameters parsing", 0.2D, 0.5D)
			    .orElseThrow(RuntimeException::new);
    	benchmark.begin();
    	final Parameters result = new Parameters();
    	this.populateDefaults(result);

    	if (!this.checkForArguments(args)) {
    		if (args.length != 0) result.directories = Arrays.asList(args);
    		return result;
	    }

	    // Parsing arguments with look-after strategy
	    // Given an argument, we already know the number of parameters (except for directories, which is parsed last)
	    // This allows us to just check that amount ahead and throw an error in case the structure is invalid
	    final Map<String, String> parameters = Maps.newHashMap();
    	final Map<String, List<String>> varArgs = Maps.newHashMap();
    	boolean insideVarargs = false;
    	String previousArgument = "";

    	for (int i = 0; i < args.length; ++i) {
    		final String arg = args[i];
    		if (!this.isArgumentStart(arg)) {
    			if (Objects.equals("", previousArgument)) this.throwCommandUsageException(i, arg, result);
    			if (insideVarargs) varArgs.get(previousArgument).add(arg);
    			else parameters.put(previousArgument, arg);
    			if (!insideVarargs) previousArgument = "";
    			continue;
		    }
		    insideVarargs = false;
		    previousArgument = arg;
		    if (this.isVarargArgument(arg)) {
    			insideVarargs = true;
    			varArgs.put(previousArgument, Lists.newArrayList());
		    } else {
		    	parameters.put(previousArgument, null);
		    }
	    }

	    this.populateResults(result, parameters, varArgs);
    	benchmark.end();
    	return result;
    }

    @Contract("null -> fail")
    private void populateDefaults(@Nonnull final Parameters params) {
    	params.directories = Lists.newArrayList();
    	params.directories.add("data");
    	//noinspection SpellCheckingInspection
	    params.printer = "openmods.depcheck.printer.ResultPrinter";
	    params.output = "output.html";
	    params.noCache = false;
	    params.disableMatcherFail = false;
	    params.printerSettings = "settings.json";
	    params.disableVariedLogging = false;
	    params.help = false;
    }

    @Contract(value = "null -> fail", pure = true)
    private boolean checkForArguments(@Nonnull final String... args) {
    	return Arrays.stream(args).anyMatch(this::isArgumentStart);
    }

    @Contract(pure = true)
    private boolean isArgumentStart(@Nonnull final String arg) {
    	return arg.startsWith("--");
    }

    @Contract(pure = true)
    private boolean isVarargArgument(@Nonnull final String arg) {
    	final String realArg = arg.substring("--".length());
    	return Objects.equals(realArg, "directories");
    }

    @Contract("_, _, _ -> fail")
    private void throwCommandUsageException(final int index, @Nullable final String parsing, @Nullable final Parameters params) {
    	final String message = "Illegal argument \"%s\" at index %s\nCurrently parsed parameters: %s";
    	throw new IllegalArgumentException(String.format(message,
			    parsing,
			    index != -1? Integer.toString(index) : "*UNKNOWN*",
			    params));
    }

    private void populateResults(@Nonnull final Parameters params,
                                 @Nonnull final Map<String, String> commands,
                                 @Nonnull final Map<String, List<String>> varArgs) {
    	for (final Map.Entry<String, String> command : commands.entrySet()) {
    		if (command.getValue() == null) this.setParamField(params, this.getParamField(params, command.getKey()), true);
    		else this.setParamField(params, this.getParamField(params, command.getKey()), command.getValue());
	    }

	    for (final Map.Entry<String, List<String>> command : varArgs.entrySet()) {
		    this.setParamField(params, this.getParamField(params, command.getKey()), command.getValue());
	    }
    }

    @Nonnull
    private Field getParamField(@Nonnull final Parameters params, @Nonnull final String name) {
    	try {
    		return params.getClass().getDeclaredField(this.toName(name.substring("--".length())));
	    } catch (final NoSuchFieldException e) {
    		this.throwCommandUsageException(-1, name, params);
    		throw new RuntimeException(); // Dead code
	    }
    }

    @Nonnull
    private String toName(@Nonnull final String from) {
    	final StringBuilder it = new StringBuilder(from.length());
    	boolean hyphenBefore = false;
    	for (final char c : from.toCharArray()) {
    		if (c == '-') {
    			hyphenBefore = true;
    			continue;
		    }
		    it.append(hyphenBefore? Character.toUpperCase(c) : c);
    		hyphenBefore = false;
	    }
    	return it.toString();
    }

    private void setParamField(@Nonnull final Parameters params, @Nonnull final Field field, @Nonnull final Object newVal) {
    	try {
    		field.setAccessible(true);
    		field.set(params, newVal);
    		field.setAccessible(false);
	    } catch (final IllegalAccessException e) {
    		throw new IllegalStateException(e);
	    }
    }

    private void showHelp() {
	    final Parameters defaults = new Parameters();
	    this.populateDefaults(defaults);

	    this.logger.info("Showing help");
	    for (int i = 0; i < 3; ++i) this.logger.info("");
    	this.logger.info("********* DEPENDENCY CHECKER DETAILS *********");
    	this.logger.info("Dependency Checker version " + VERSION);
    	this.logger.info("Developed by boq");
    	this.logger.info("Run directory: " + System.getProperty("user.dir"));
    	this.logger.info("Java version: " + System.getProperty("java.version") + " (by " + System.getProperty("java.vendor") + ")");
    	this.logger.info("JVM version: " + System.getProperty("java.vm.version") + " (by " + System.getProperty("java.vm.vendor") + ")");
    	this.logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
    	this.logger.info("");
    	this.logger.info("********* DEPENDENCY CHECKER HELP *********");
    	this.logger.info("NOTE: Square brackets indicate optional arguments, curly brackets needed ones");
    	this.logger.info("");
    	this.logger.info("USAGE: ./DependencyChecker [arguments]");
    	this.logger.info("");
    	this.logger.info("ARGUMENTS: ");
    	this.logger.info("--directories {directories}    Specifies the working directories of the software. Defaults to " + defaults.directories);
    	this.logger.info("--printer [class]              Specifies the class of the printer to use. Defaults to " + defaults.printer);
    	this.logger.info("--printer-settings [file]      Specifies the printer settings file path. Defaults to " + defaults.printerSettings);
    	this.logger.info("--output [file]                Specifies the output file for the scan results. Defaults to " + defaults.output);
    	this.logger.info("NOTE: If no argument is specified, everything defaults to --directories");
	    this.logger.info("");
	    this.logger.info("SWITCHES: ");
    	this.logger.info("--no-cache                  Disables the entire caching mechanism: cache isn't used if available nor stored");
    	this.logger.info("--disable-matcher-fail      Disables software failure in case an illegally named file is found in the working directory");
    	this.logger.info("--disable-varied-logging    Uses the previous logging implementation (JDK default) instead of the custom solution");
    	this.logger.info("--help                      Shows this help screen. If this switch is given, everything else is ignored.");
	    for (int i = 0; i < 3; ++i) this.logger.info("");
    }

    private void execute(@Nonnull final Parameters arguments) {
	    for (final String dir : arguments.directories) {
		    final File topDir = new File(dir);
		    this.logger.info("Processing dir: {}", topDir.getAbsolutePath());
		    final Benchmark dirProcessing = Benchmark.create("Directory processing", Double.MAX_VALUE, Double.MAX_VALUE)
				    .orElseThrow(RuntimeException::new);
		    dirProcessing.begin();
		    final SourceParser depWalker = new SourceParser(topDir, arguments.noCache);
		    if (arguments.disableMatcherFail) SourceParser.disableMatcherFailure();
		    final SourceDependencies availableDependencies = depWalker.collectAvailableDependencies();
		    dirProcessing.end();

		    final Benchmark targetProcessing = Benchmark.create("Target processing", Double.MAX_VALUE, Double.MAX_VALUE)
				    .orElseThrow(RuntimeException::new);
		    targetProcessing.begin();
		    final DependencyCollector collector = new DependencyCollector(availableDependencies);
		    new TargetParser(topDir).accept(collector);
		    targetProcessing.end();

		    final List<DependencyResolveResult> results = collector.getResults();

		    final Benchmark printing = Benchmark.create("Printing results", 0.7D, 1.0D)
				    .orElseThrow(RuntimeException::new);
		    printing.begin();
		    this.getPrinterClass(arguments, topDir).print(
				    java.nio.file.Paths.get(arguments.output).isAbsolute()? new File(arguments.output) : new File(topDir, arguments.output),
				    availableDependencies,
				    results);
		    printing.end();

		    this.logger.info("Operation completed successfully without errors");
	    }
    }

    @Nonnull
	private IPrinter getPrinterClass(@Nonnull final Parameters parameters, @Nonnull final File currentlyParsingDirectory) {
    	try {
    		this.logger.info("Attempting to instantiate printer");
    		this.logger.trace("Printer supplied: " + parameters.printer);
    		final Class<?> clazz = this.attemptToGetClass(parameters.printer, currentlyParsingDirectory);
    		final Object constructedObject = clazz.getConstructor().newInstance();
    		final IPrinter printer = IPrinter.class.cast(constructedObject);
    		if (printer instanceof IConfigurablePrinter) {
    			this.attemptConfiguration((IConfigurablePrinter) printer, parameters.printerSettings, currentlyParsingDirectory);
		    }
    		return printer;
	    } catch (final ReflectiveOperationException | ClassCastException e) {
    		this.logger.error("An error has occurred while attempting to load printer {}", parameters.printer, e);
    		this.logger.error("Falling back to default");
    		this.logger.trace(ResultPrinter.class.getCanonicalName());
    		return new ResultPrinter();
	    }
    }

    @Nonnull
    private Class<?> attemptToGetClass(@Nonnull final String className, @Nonnull final File currentDir) throws ReflectiveOperationException {
    	try {
    		return Class.forName(className);
	    } catch (final ClassNotFoundException e) {
    		this.logger.warn("Class not found: {}. Attempting to look for an alias", className, e);
	    }

	    this.logger.info("Considering {} as an alias. Attempting resolution", className);
	    return Class.forName(AliasesMap.of(currentDir).getClassForAlias(className).orElseThrow(this::get));
    }

    @Contract(pure = true)
    @Nonnull
    private ReflectiveOperationException get() {
    	return new ReflectiveOperationException("Unable to load aliased printer. Is the alias correct? Or is it even registered?");
    }

    private void attemptConfiguration(@Nonnull final IConfigurablePrinter printer,
                                      @Nonnull final String settingsFile,
                                      @Nonnull final File currentDir) {
    	File settings = new File(currentDir, settingsFile);
    	if (!settings.exists()) {
    		this.logger.warn("File not found: {} in {}. Attempting to use fallback", settingsFile, currentDir.getAbsolutePath());
    		settings = new File(System.getProperty("user.dir"), settingsFile);
    		if (!settings.exists()) {
    			this.logger.warn("The specified file {} does not exist in {}. Considering the file as non-existent",
					    settingsFile, System.getProperty("user.dir"));
    			settings = null;
		    }
	    }
	    if (settings == null || !settings.canRead()) {
    		this.logger.warn("Unable to read the given file {}. Skipping it", settings == null? "null" : settings.getAbsolutePath());
    		settings = null;
	    }
	    try {
		    printer.populateSettings(settings);
	    } catch (final Throwable t) {
    		this.logger.error("Error while applying configuration settings", t);
    		if (printer.isConfigurationNeeded()) throw new RuntimeException("Configuration settings could not be applied", t);
	    }
    }
}
