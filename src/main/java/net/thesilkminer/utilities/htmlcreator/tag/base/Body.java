package net.thesilkminer.utilities.htmlcreator.tag.base;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the body tag of an HTML document.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Body extends HtmlTag {

	public static class BodyHtmlTag extends HtmlTag {

		protected BodyHtmlTag(@Nonnull final String tagName) {
			super(tagName);
		}

		@Override
		public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
			return tag instanceof Body;
		}
	}

	public Body() {
		super("body");
	}

	@Override
	public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
		return tag instanceof Html;
	}
}
