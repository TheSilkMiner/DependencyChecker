package net.thesilkminer.utilities.htmlcreator.tag.head;

import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;
import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;
import net.thesilkminer.utilities.htmlcreator.tag.base.Head;

import javax.annotation.Nullable;

/**
 * Represents the meta tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Meta extends Head.HeadHtmlTag {

	public static class Charset extends HtmlAttribute {

		public Charset() {
			super("charset");
		}

		@Override
		public boolean canApplyToTag(@Nullable final HtmlTag tag) {
			return tag instanceof Meta;
		}
	}

	public Meta() {
		super("meta");
		this.setVoidElement(true);
	}

	@Override
	public boolean mustBeEmpty() {
		return true;
	}

	@Override
	public boolean canBeEmpty() {
		return true;
	}
}
