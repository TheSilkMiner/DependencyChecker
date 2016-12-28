package net.thesilkminer.utilities.htmlcreator.tag.body;

import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;
import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nullable;

/**
 * Represents the a tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class A extends HtmlTag {

	public static class Href extends HtmlAttribute {

		public Href() {
			super("href");
		}

		@Override
		public boolean canApplyToTag(@Nullable final HtmlTag tag) {
			return tag instanceof A;
		}
	}

	public static class Name extends HtmlAttribute {

		public Name() {
			super("name");
		}

		@Override
		public boolean canApplyToTag(@Nullable final HtmlTag tag) {
			return tag instanceof A;
		}
	}

	public A() {
		super("a");
	}
}
