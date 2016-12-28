package net.thesilkminer.utilities.htmlcreator;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * Represents a document type declaration.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class DocumentType {

	private final HtmlVersion version;
	private final String cachedToString;

	private DocumentType(@Nonnull final HtmlVersion version) {
		this.version = version;
		this.cachedToString = this.cacheString(this.version);
	}

	@Contract(pure = true)
	@Nonnull
	public static DocumentType of(@Nonnull final HtmlVersion version) {
		return new DocumentType(Preconditions.checkNotNull(version));
	}

	@Nonnull
	private String cacheString(@Nonnull final HtmlVersion version) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE ");

		if (version.isV4()) builder.append("HTML");
		else builder.append("html");

		return version.dtd() == null?
				builder.append(">").toString() :
				builder.append(" PUBLIC ").append(version.dtd()).append('>').toString();
	}

	@Nonnull
	public HtmlVersion getVersion() {
		return this.version;
	}

	@Override
	public String toString() {
		return this.cachedToString;
	}
}
