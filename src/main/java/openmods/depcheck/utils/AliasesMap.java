package openmods.depcheck.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * A class used to map aliases to the various class names.
 *
 * @author TheSilkMiner
 *
 * @since 0.1
 */
public class AliasesMap {

	private static final Logger LOGGER = LoggerFactory.getLogger(AliasesMap.class);
	private static final Map<String, String> GLOBAL_ALIASES = loadGlobalEntries();

	private final Map<String, String> currentAliases;

	private AliasesMap(final String currentDirPath) {
		this.currentAliases = this.loadLocalAliases(currentDirPath);
	}

	@Contract(pure = true)
	@Nonnull
	private static Map<String, String> loadGlobalEntries() {
		final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

		try (final FileReader unwrappedIn = new FileReader(new File(System.getProperty("user.dir"), "aliases.txt"));
		     final BufferedReader in = new BufferedReader(unwrappedIn)) {
			in.lines().forEach(it -> {
				if (it.isEmpty()) return;

				final String[] pair = it.split("=");
				if (pair.length != 2) {
					LOGGER.warn("Invalid line in global aliases file: {}", it);
					return;
				}

				builder.put(pair[0], pair[1]);
			});
		} catch (final IOException e) {
			LOGGER.warn("An error has occurred while trying to load global aliases", e);
		}

		return builder.build();
	}

	@Contract(value = "null -> fail", pure = true)
	@Nonnull
	public static AliasesMap of(@Nonnull final File dir) {
		return new AliasesMap(Preconditions.checkNotNull(dir).getAbsolutePath());
	}

	@Contract(pure = true)
	@Nonnull
	private Map<String, String> loadLocalAliases(@Nonnull final String dir) {
		final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

		try (final FileReader unwrappedIn = new FileReader(new File(dir, "aliases.txt"));
		     final BufferedReader in = new BufferedReader(unwrappedIn)) {
			in.lines().forEach(it -> {
				if (it.isEmpty()) return;

				final String[] pair = it.split("=");
				if (pair.length != 2) {
					LOGGER.warn("Invalid line in local aliases file: {}", it);
					return;
				}

				builder.put(pair[0], pair[1]);
			});
		} catch (final IOException e) {
			LOGGER.warn("An error has occurred while trying to load local aliases", e);
		}

		return builder.build();
	}

	@Nonnull
	public Optional<String> getClassForAlias(@Nonnull final String name) {
		Preconditions.checkNotNull(name);
		final List<String> allAliases = Lists.newArrayList();
		allAliases.addAll(
				this.currentAliases.entrySet()
						.stream()
						.peek(this::peekCurrentAlias)
						.filter(it -> Objects.equals(name, it.getKey()))
						.map(Map.Entry::getValue)
						.peek(this::peekCurrentAlias)
						.collect(Collectors.toList())
		);
		allAliases.addAll(
				GLOBAL_ALIASES.entrySet()
					.stream()
					.peek(this::peekCurrentAlias)
					.filter(it -> Objects.equals(name, it.getKey()))
					.map(Map.Entry::getValue)
					.peek(this::peekCurrentAlias)
					.filter(it -> !allAliases.contains(it))
					.peek(this::peekCurrentAlias)
					.collect(Collectors.toList())
		);
		if (allAliases.size() != 1) {
			if (allAliases.size() < 0) LOGGER.info("No aliases found for " + name);
			else LOGGER.info("Found {} aliases for {} {}. Returning the first available...", allAliases.size(), name, allAliases);
		}
		return allAliases.stream().findFirst(); // Hacky way of using streams, but who cares?
	}

	private <T> void peekCurrentAlias(final T t) {
		LOGGER.trace("Peek result: " + t);
	}
}
