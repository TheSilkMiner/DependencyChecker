package net.thesilkminer.utilities.htmlcreator.tag.base;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Main tag of every HTML document. It MUST be the only first-level tag in a document.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Html extends HtmlTag {

	public static class Builder {

		private Head head;
		private Body body;

		private Builder() {}

		@Nonnull
		public Builder head(@Nonnull final Head head) {
			this.head = Preconditions.checkNotNull(head);
			return this;
		}

		@Nonnull
		public Builder body(@Nonnull final Body body) {
			this.body = Preconditions.checkNotNull(body);
			return this;
		}

		@Nonnull
		public Html build() {
			final Html it = new Html();
			it.addChild(this.head);
			it.addChild(this.body);
			return it;
		}
	}

	private Html() {
		super("html");
	}

	@Contract(pure = true)
	@Nonnull
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
		return false;
	}
}
