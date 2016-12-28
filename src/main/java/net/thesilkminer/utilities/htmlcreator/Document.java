package net.thesilkminer.utilities.htmlcreator;

import net.thesilkminer.utilities.htmlcreator.tag.base.Html;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * A class representing a complete HTML document.
 *
 * @author TheSilkMinet
 * @since 1.0
 */
public class Document {

	public static class Builder {

		private HtmlVersion version;
		private Html html;

		private Builder() {
			this.version = HtmlVersion.HTML_5;
		}

		public Builder version(@Nonnull final HtmlVersion version) {
			this.version = Preconditions.checkNotNull(version);
			return this;
		}

		public Builder html(@Nonnull final Html html) {
			this.html = Preconditions.checkNotNull(html);
			return this;
		}

		@Nonnull
		public Document build() {
			return new Document(DocumentType.of(this.version), Preconditions.checkNotNull(this.html));
		}
	}

	private final DocumentType type;
	private final Html html;

	private Document(@Nonnull final DocumentType type, @Nonnull final Html html) {
		this.type = type;
		this.html = html;
	}

	@Contract(pure = true)
	@Nonnull
	public static Builder builder() {
		return new Builder();
	}

	@Nonnull
	public DocumentType getType() {
		return this.type;
	}

	@Nonnull
	public Html getHtml() {
		return this.html;
	}

	@Override
	public String toString() {
		String builder = "";
		builder += this.getType() + "\n";
		builder += this.getHtml() + "\n";
		return builder;
	}
}
