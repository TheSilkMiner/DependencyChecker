package net.thesilkminer.utilities.htmlcreator.tag.base;


import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the head tag of an HTML document.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Head extends HtmlTag {

	public abstract static class HeadHtmlTag extends HtmlTag {

		protected HeadHtmlTag(@Nonnull final String tagName) {
			super(tagName);
		}

		@Override
		public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
			return tag instanceof Head;
		}
	}

	public Head() {
		super("head");
	}

	@Override
	public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
		return tag instanceof Html;
	}
}
