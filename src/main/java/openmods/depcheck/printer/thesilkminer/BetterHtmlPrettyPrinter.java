package openmods.depcheck.printer.thesilkminer;

import net.thesilkminer.utilities.htmlcreator.Document;
import net.thesilkminer.utilities.htmlcreator.HtmlVersion;
import net.thesilkminer.utilities.htmlcreator.attribute.AttributeWrapper;
import net.thesilkminer.utilities.htmlcreator.attribute.Class;
import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;
import net.thesilkminer.utilities.htmlcreator.attribute.Id;
import net.thesilkminer.utilities.htmlcreator.tag.base.Body;
import net.thesilkminer.utilities.htmlcreator.tag.base.Head;
import net.thesilkminer.utilities.htmlcreator.tag.base.Html;
import net.thesilkminer.utilities.htmlcreator.tag.body.A;
import net.thesilkminer.utilities.htmlcreator.tag.body.Div;
import net.thesilkminer.utilities.htmlcreator.tag.body.Img;
import net.thesilkminer.utilities.htmlcreator.tag.body.details.Details;
import net.thesilkminer.utilities.htmlcreator.tag.body.details.Summary;
import net.thesilkminer.utilities.htmlcreator.tag.body.table.Tbody;
import net.thesilkminer.utilities.htmlcreator.tag.body.table.Td;
import net.thesilkminer.utilities.htmlcreator.tag.body.table.Th;
import net.thesilkminer.utilities.htmlcreator.tag.body.table.Thead;
import net.thesilkminer.utilities.htmlcreator.tag.body.table.Tr;
import net.thesilkminer.utilities.htmlcreator.tag.body.text.H1;
import net.thesilkminer.utilities.htmlcreator.tag.body.text.H2;
import net.thesilkminer.utilities.htmlcreator.tag.body.text.H3;
import net.thesilkminer.utilities.htmlcreator.tag.body.text.P;
import net.thesilkminer.utilities.htmlcreator.tag.head.Meta;
import net.thesilkminer.utilities.htmlcreator.tag.head.Style;
import net.thesilkminer.utilities.htmlcreator.tag.head.Title;
import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;
import net.thesilkminer.utilities.htmlcreator.tag.TagWrapper;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import openmods.depcheck.dependencies.DependencyResolveResult;
import openmods.depcheck.parser.SourceDependencies;
import openmods.depcheck.printer.IConfigurablePrinter;
import openmods.depcheck.utils.TypedElement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A custom pretty printer which produces configurable output
 * more appealing to the end-user than the default printer.
 *
 * @author TheSilkMiner
 * @since 0.1
 * @version 1.0
 */
public class BetterHtmlPrettyPrinter implements IConfigurablePrinter {

	@SuppressWarnings("SpellCheckingInspection")
	private static final class PrinterSettings {

		@SerializedName("useFiraCode")
		private boolean preferFiraCode;

		@SerializedName("codeFonts")
		private String fontString;

		@SerializedName("useImages")
		private boolean useImages;

		@SerializedName("removeExtensions")
		private boolean noJar;

		@SerializedName("charset")
		private String charset;

		{
			this.preferFiraCode = true;
			this.fontString = "\"Courier New\", Courier, monospace";
			this.useImages = true;
			this.noJar = false;
			this.charset = "UTF-8";
		}

		@Nonnull
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("preferFiraCode", this.preferFiraCode)
					.add("fontString", this.fontString)
					.add("useImages", this.useImages)
					.add("noJar", this.noJar)
					.add("charset", this.charset)
					.toString();
		}
	}

	private static final class CompatibilityData {

		private static class SourceModCompatibilityTable {

			private static class MissingTargetDependencies {

				private static class MissingSourceDependencies {
					final Set<String> missingClasses = Sets.newHashSet();
					final SetMultimap<String, TypedElement> missingElements = HashMultimap.create();
				}

				private final Map<String, MissingSourceDependencies> targetClass = Maps.newHashMap();

				@Nullable
				private MissingSourceDependencies get(@Nonnull final String targetCls) {
					return this.targetClass.computeIfAbsent(targetCls, k -> new MissingSourceDependencies());
				}
			}

			final Table<File, ArtifactVersion, MissingTargetDependencies> missingDependencies = HashBasedTable.create();

			@Nonnull
			MissingTargetDependencies getOrCreate(@Nonnull final File target, @Nonnull final ArtifactVersion sourceVersion) {
				MissingTargetDependencies result = this.missingDependencies.get(target, sourceVersion);
				if (result == null) {
					result = new MissingTargetDependencies();
					this.missingDependencies.put(target, sourceVersion, result);
				}

				return result;
			}
		}

		private final Set<File> allTargets = Sets.newHashSet();
		private final Map<String, SourceModCompatibilityTable> modCompatibilityTable = Maps.newHashMap();

		@Nullable
		private SourceModCompatibilityTable get(@Nonnull final String sourceMod) {
			return this.modCompatibilityTable.computeIfAbsent(sourceMod, k -> new SourceModCompatibilityTable());
		}

		@Nonnull
		private DependencyResolveResult.MissingDependencySink createForTarget(@Nonnull final File target) {
			return new DependencyResolveResult.MissingDependencySink() {
				@Override
				public void acceptMissingClass(final String targetCls,
				                               final String sourceMod,
				                               final String sourceCls,
				                               final Set<String> versions) {
					final SourceModCompatibilityTable table = Preconditions.checkNotNull(CompatibilityData.this.get(sourceMod));

					for (final String version : versions) {
						Preconditions.checkNotNull(table.getOrCreate(target, new DefaultArtifactVersion(version))
								.get(targetCls)).missingClasses.add(sourceCls);
					}
				}

				@Override
				public void acceptMissingElement(final String targetCls,
				                                 final String sourceMod,
				                                 final String sourceCls,
				                                 final TypedElement sourceElement,
				                                 final Set<String> versions) {
					final SourceModCompatibilityTable table = Preconditions.checkNotNull(CompatibilityData.this.get(sourceMod));

					for (final String version : versions) {
						Preconditions.checkNotNull(table.getOrCreate(target, new DefaultArtifactVersion(version))
								.get(targetCls)).missingElements.put(sourceCls, sourceElement);
					}
				}
			};
		}

		void load(@Nonnull final DependencyResolveResult dependencies) {
			this.allTargets.add(dependencies.jarFile);
			dependencies.visit(this.createForTarget(dependencies.jarFile));
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(BetterHtmlPrettyPrinter.class);
	private static final String VERSION = "1.0";

	// WOW! Just... WOW!
	private static final String CSS = "body {\n\tbackground-color: #1C1C1C;\n\tcolor: #AFAFAF;\n" +
			"\tfont-family: Verdana, Helvetica, Arial, sans-serif, serif;\n\tfont-size: 0.8em;\n" +
			"\ttext-align: center;\n}\np {\n\tcolor: inherit;\n}\na:link, a:hover, a:active, a:visited {\n" +
			"\ttext-decoration: none;\n\tcolor: inherit;\n}\ntable {\n\tborder-collapse: collapse;\n" +
			"}\nth.hide {\n\tborder-top: 0px solid #1C1C1C;\n\tborder-left: 0px solid #1C1C1C;\n" +
			"}\nth, td {\n\ttext-align: center;\n\tborder: 1px solid black;\n}\n.r {\n\tcolor: red;\n" +
			"}\n.r a:hover {\n\tcolor:orange;\n}\n.g {\n\tcolor: green;\n}\ndiv.missing {\n\tdisplay: none;\n" +
			"\toverflow: auto;\n}\ndiv.missing:target {\n\tdisplay: block;\n\tborder: 3px solid orange;\n" +
			"}\ndiv.missingTextContainer {\n\toverflow: auto;\n\tborder: 1px solid #AFAFAF;\n" +
			"\tpadding-bottom: 10px;\n\tpadding-left: 2px;\n}\np.missingText {\n\tfont-family: $FAM$;\n" +
			"\tfont-size: small;\n\twhite-space: nowrap;\n\ttext-align: left;\n}\nimg.tick, img.cross {\n" +
			"\twidth: 25px;\n\theight: 25px;\n\tborder: 0px inherit inherit;\n\tcolor: inherit;\n" +
			"\tfont-size: 1.1em;\n}\ndiv.header {\n\tbackground-color: #0A0A0A;\n\tcolor: #FFFFFF;\n" +
			"\ttext-align: center;\n\tdisplay: block;\n\twidth: 100%;\n\theight: 100px;\n\tline-height: 100px;\n" +
			"\tvertical-align: middle;\n\ttop: 1px;\n\tleft: 1px;\n\tmargin: 0px 0px 0px 0px;\n}\n" +
			"div.tableContainer {\n\toverflow: auto;\n\twidth: 100%;\n\tpadding-bottom: 10px;\n" +
			"\tpadding-left: 2px;\n}\nh1.headerText {\n\tmargin: auto auto auto auto;\n\tcolor: orange;\n" +
			"}\nh2, h3, h4, summary {\n\tcolor: orange;\n}\nsummary {\n\tfont-weight: bold;\n" +
			"\tborder-bottom: 8px solid #1C1C1C;\n}\ndiv.splitter {\n\twidth: 1px;\n\theight: 20px;\n" +
			"}\ndetails {\n\tcursor: default;\n\tpadding-top: 5px;\n}\ndiv.hideEverything:target {\n" +
			"\t/* TODO Find out why this does not work */\n\tdisplay: none;\n}\ndiv.hideEverything {\n" +
			"\ttext-align: right;\n\tdisplay: block;\n}\nsummary {\n\tcursor: pointer\n}";

	private PrinterSettings settings = new PrinterSettings();

	@Override
	public void populateSettings(@Nullable final File file) {
		if (file == null) return;
		try {
			this.settings = new GsonBuilder().create().fromJson(new FileReader(file), PrinterSettings.class);
			LOG.info("Successfully configured printer");
		} catch (final IOException e) {
			LOG.warn("Unable to load settings file. Using defaults.", e);
			this.settings = new PrinterSettings();
		}
	}

	@Override
	public boolean isConfigurationNeeded() {
		return false;
	}

	@Override
	public void print(@Nonnull final File file,
	                  @Nonnull final SourceDependencies availableDependencies,
	                  @Nonnull final Collection<DependencyResolveResult> results) {
		LOG.info("Using printer {} ({}) version {} on file {} ({}) with arguments {}",
				this.getClass().getSimpleName(),
				this.getClass().getName(),
				VERSION,
				file,
				file.getAbsolutePath(),
				this.settings);
		final Path path = file.toPath();
		this.handleFileDeletionAndCreation(path);

		final long begin = System.nanoTime();
		try (final BufferedWriter out = Files.newBufferedWriter(path, this.getCharset())) {
			this.write(out, availableDependencies, results);
		} catch (final IOException e) {
			LOG.error("An error has occurred while writing the file", e);
			throw Throwables.propagate(e);
		}
		final long time = System.nanoTime() - begin;
		LOG.info("Printing completed");
		LOG.info("Time elapsed: {} nanoseconds ({} milli-seconds, {} seconds)",
				time,
				TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS),
				TimeUnit.SECONDS.convert(time, TimeUnit.NANOSECONDS));
	}

	private void handleFileDeletionAndCreation(@Nonnull final Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (final IOException e) {
			LOG.error("Impossible to delete the specified file. We'll try to write on it anyway. Hopefully it works", e);
		}

		try {
			Files.createFile(path);
		} catch (final FileAlreadyExistsException e) {
			LOG.error("The given file already exists: I guess it wasn't deleted correctly previously. Hopefully it will work", e);
		} catch (final IOException e) {
			LOG.error("An error has occurred while creating the file. This may crash the entire printer", e);
		}
	}

	@Nonnull
	private Charset getCharset() {
		if (!Charset.isSupported(this.settings.charset)) {
			LOG.error("Specified charset {} is not supported by the current system. Reverting to default ({})",
					this.settings.charset,
					Charset.defaultCharset());
			return Charset.defaultCharset();
		}
		LOG.info("Attempting charset resolution");
		try {
			final Charset it = Charset.forName(this.settings.charset);
			LOG.info("Successfully resolved charset. Using {}", it);
			return it;
		} catch (final IllegalCharsetNameException | UnsupportedCharsetException e) {
			LOG.warn("Charset resolution failed due to an exception. Reverting to default ({})", Charset.defaultCharset());
			LOG.warn("Exception: ", e);
			return Charset.defaultCharset();
		}
	}

	private void write(final BufferedWriter out, final SourceDependencies dependencies, final Collection<DependencyResolveResult> results) throws IOException {
		try {
			out.write(this.constructHtml(dependencies, results).toString());
			out.flush();
		} catch (final IOException e) {
			throw new IOException("An error has occurred while attempting to write on the specified file", e);
		}
	}

	@Nonnull
	private Document constructHtml(final SourceDependencies dependencies, final Collection<DependencyResolveResult> results) {
		return Document.builder()
				.version(HtmlVersion.HTML_5)
				.html(Html.builder()
						.head(this.constructHead())
						.body(this.constructBody(dependencies, results))
						.build())
				.build();
	}

	@Nonnull
	private Head constructHead() {
		return this.asBuilder(Head.class)
				.child(this.asBuilder(Meta.class)
						.attribute(this.with(Meta.Charset.class)
								.value("UTF-8")
								.build())
						.build())
				.child(this.asBuilder(Title.class)
						.content("Dependencies")
						.build())
				.child(this.asBuilder(Style.class)
						.attribute(this.with(Style.Type.class)
								.value("TEXT_CSS")
								.build())
						.content(this.constructCss())
						.build())
				.build();
	}

	@Contract(pure = true)
	@Nonnull
	@SuppressWarnings("SpellCheckingInspection")
	private String constructCss() {
		String toReplace = this.settings.fontString;
		if (this.settings.preferFiraCode) toReplace = "\"Fira Code\", " + toReplace;
		return CSS.replace("$FAM$", toReplace);
	}

	@Nonnull
	private Body constructBody(final SourceDependencies dependencies, final Collection<DependencyResolveResult> results) {
		final TagWrapper<Body> body = this.asBuilder(Body.class)
				.child(this.asBuilder(Div.class)
						.attribute(this.with(Class.class)
								.value("header")
								.build())
						.child(this.asBuilder(H1.class)
								.attribute(this.with(Class.class)
										.value("headerText")
										.build())
								.content("Dependency check results")
								.build())
						.build());
		this.constructReport(body, dependencies, this.convertDataToCompatibilityData(results));
		body.child(this.asBuilder(Div.class)
				.attribute(this.with(Class.class)
						.value("hideEverything")
						.build())
				.child(this.asBuilder(A.class)
						.attribute(this.with(A.Href.class)
								.value("#")
								.build())
						.child(this.asBuilder(P.class)
								.content("Hide everything")
								.build())
						.build())
				.build());
		return body.build();
	}

	@Nonnull
	private CompatibilityData convertDataToCompatibilityData(final Collection<DependencyResolveResult> results) {
		final CompatibilityData it = new CompatibilityData();
		results.forEach(it::load);
		return it;
	}

	private void constructReport(@Nonnull final TagWrapper<Body> body, final SourceDependencies dependencies, final CompatibilityData data) {
		this.constructSummary(body, dependencies, data);
		body.child(this.asBuilder(Div.class).attribute(this.with(Class.class).value("splitter").build()).build());
		this.constructDetails(body, data);
	}

	private void constructSummary(@Nonnull final TagWrapper<Body> body, final SourceDependencies dependencies, final CompatibilityData data) {
		final List<File> allTargets = Lists.newArrayList(data.allTargets);
		allTargets.sort(Comparator.comparing(File::getName));
		dependencies.getAllModIds().stream().sorted().forEach(source -> {
			final CompatibilityData.SourceModCompatibilityTable compatibilityTable = Preconditions.checkNotNull(data.get(source));
			final List<ArtifactVersion> allVersions = dependencies.getMod(source).allVersions().stream()
					.map(DefaultArtifactVersion::new)
					.sorted()
					.collect(Collectors.toList());
			body.child(this.asBuilder(H2.class)
					.content(source)
					.build());
			body.child(this.asBuilder(Div.class)
					.attribute(this.with(Class.class)
							.value("tableContainer")
							.build())
					.child(this.asBuilder(net.thesilkminer.utilities.htmlcreator.tag.body.table.Table.class)
							.child(this.asBuilder(Thead.class)
									.child(this.asBuilder(Tr.class)
											.child(this.asBuilder(Th.class)
													.attribute(this.with(Class.class)
															.value("hide")
															.build())
													.build())
											.childList(
													allVersions.stream()
															.map(it -> this.asBuilder(Th.class)
																	.content(this.toString(it))
																	.build())
															.collect(Collectors.toList())
											)
											.build())
									.build())
							.child(this.asBuilder(Tbody.class)
									.childList(this.constructSummaryRows(source, allTargets, allVersions, compatibilityTable))
									.build())
							.build())
					.build());
		});
	}

	@Nonnull
	private <T> String toString(@Nullable final T it) {
		if (it == null) return "$null$";
		final String toString = it.toString();
		if (this.settings.noJar && toString.endsWith(".jar")) return toString.substring(0, toString.length() - ".jar".length());
		return toString;
	}

	@Nonnull
	private Collection<HtmlTag> constructSummaryRows(@Nonnull final String source,
	                                                 @Nonnull final List<File> allTargets,
	                                                 @Nonnull final List<ArtifactVersion> allVersions,
	                                                 @Nonnull final CompatibilityData.SourceModCompatibilityTable table) {
		return allTargets.stream()
				.map(target -> this.asBuilder(Tr.class)
						.child(this.asBuilder(Td.class)
								.content(this.toString(target.getName()))
								.build())
						.childList(this.constructSummaryRow(source, allVersions, table, target))
						.build())
				.collect(Collectors.toList());
	}

	@Nonnull
	private Collection<HtmlTag> constructSummaryRow(@Nonnull final String source,
	                                                @Nonnull final List<ArtifactVersion> allVersions,
	                                                @Nonnull final CompatibilityData.SourceModCompatibilityTable table,
	                                                @Nonnull final File target) {
		final List<HtmlTag> list = Lists.newArrayList();
		allVersions.forEach(it -> {
			if (table.missingDependencies.contains(target, it))	this.constructMissingEntry(list, source, it, target.getName());
			else this.constructAvailableEntry(list);
		});
		return list;
	}

	private void constructMissingEntry(@Nonnull final List<HtmlTag> tags,
	                                   @Nonnull final String source,
	                                   @Nonnull final ArtifactVersion version,
	                                   @Nonnull final String name) {
		tags.add(this.asBuilder(Td.class)
				.attribute(this.with(Class.class)
						.value("r")
						.build())
				.child(this.asBuilder(A.class)
						.attribute(this.with(A.Href.class)
								.value("#" + this.createAnchor(name, source, version))
								.build())
						.child(this.asBuilder(Img.class)
								.attribute(this.with(Img.Alt.class)
										.value("\u2612")
										.build())
								.attribute(this.with(Img.Src.class)
										.value(this.settings.useImages? "https://upload.wikimedia.org/wikipedia/commons/b/ba/Red_x.svg" : "")
										.build())
								.attribute(this.with(Img.Title.class)
										.value("Various classes and methods are missing")
										.build())
								.attribute(this.with(Class.class)
										.value("cross")
										.build())
								.build())
						.build())
				.build());
	}

	@Contract(pure = true)
	@Nonnull
	private String createAnchor(@Nonnull final String target,
	                            @Nonnull final String source,
	                            @Nonnull final ArtifactVersion version) {
		return this.createAnchor(target, source, version, "__");
	}

	@Contract(pure = true)
	@Nonnull
	private String createAnchor(@Nonnull final String target,
	                            @Nonnull final String source,
	                            @Nonnull final ArtifactVersion version,
	                            @Nonnull final String separator) {
		return String.format("%s%s%s%s%s", this.toString(target), separator, this.toString(source), separator, this.toString(version));
	}

	private void constructAvailableEntry(@Nonnull final List<HtmlTag> tags) {
		tags.add(this.asBuilder(Td.class)
				.attribute(this.with(Class.class)
						.value("g")
						.build())
				.child(this.asBuilder(Img.class)
						.attribute(this.with(Img.Alt.class)
								.value("\u2611")
								.build())
						.attribute(this.with(Img.Src.class)
								.value(this.settings.useImages? "https://upload.wikimedia.org/wikipedia/commons/a/ac/Green_tick.svg" : "")
								.build())
						.attribute(this.with(Img.Title.class)
								.value("Production ready")
								.build())
						.attribute(this.with(Class.class)
								.value("tick")
								.build())
						.build())
				.build());
	}

	private void constructDetails(@Nonnull final TagWrapper<Body> body, @Nonnull final CompatibilityData data) {
		data.modCompatibilityTable.forEach((source, table) ->
				table.missingDependencies.cellSet().forEach(it -> {
					final String target = Preconditions.checkNotNull(it.getRowKey()).getName();
					final ArtifactVersion version = Preconditions.checkNotNull(it.getColumnKey());

					body.child(this.asBuilder(Div.class)
							.attribute(this.with(Class.class)
									.value("missing")
									.build())
							.attribute(this.with(Id.class)
									.value(this.createAnchor(target, source, version))
									.build())
							.child(this.asBuilder(H3.class)
									.content(this.createAnchor(target, source, version, ":"))
									.build())
							.childList(this.constructDetail(it))
							.build());
				}));
	}

	@Nonnull
	private Collection<HtmlTag> constructDetail(
			@Nonnull final Table.Cell<File, ArtifactVersion, CompatibilityData.SourceModCompatibilityTable.MissingTargetDependencies> entry) {
		final Collection<HtmlTag> tag = Lists.newArrayList();
		Preconditions.checkNotNull(entry.getValue()).targetClass.forEach((targetClass, missingSourceDependencies) ->
			tag.add(this.asBuilder(Details.class)
					.child(this.asBuilder(Summary.class)
							.content(targetClass)
							.build())
					.child(this.asBuilder(Div.class)
							.attribute(this.with(Class.class)
									.value("missingTextContainer")
									.build())
							.child(this.asBuilder(P.class)
									.attribute(this.with(Class.class)
											.value("missingText")
											.build())
									.content(this.constructDetailedMissingEntries(missingSourceDependencies))
									.build())
							.build())
					.build()));
		return tag;
	}

	@Nonnull
	private String constructDetailedMissingEntries(
			@Nonnull final CompatibilityData.SourceModCompatibilityTable.MissingTargetDependencies.MissingSourceDependencies it) {
		final List<String> missing = Lists.newArrayList();
		missing.addAll(it.missingClasses);
		it.missingElements.entries().forEach(entry -> missing.add(entry.getKey() + " " + entry.getValue()));
		missing.sort(Comparator.naturalOrder());
		return Joiner.on('\n').join(missing);
	}

	@Contract(pure = true)
	@Nonnull
	private <T extends HtmlTag> TagWrapper<T> asBuilder(@Nonnull final java.lang.Class<T> tag) {
		return TagWrapper.builder(tag);
	}

	@Contract(pure = true)
	@Nonnull
	private <T extends HtmlAttribute> AttributeWrapper<T> with(@Nonnull final java.lang.Class<T> attribute) {
		return AttributeWrapper.builder(attribute);
	}
}
