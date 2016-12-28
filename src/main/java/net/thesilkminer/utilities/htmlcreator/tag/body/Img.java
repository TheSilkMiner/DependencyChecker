package net.thesilkminer.utilities.htmlcreator.tag.body;

import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;
import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents an image tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Img extends HtmlTag {

	private static class ImgAttribute extends HtmlAttribute {

		protected ImgAttribute(@Nonnull final String name) {
			super(name);
		}

		@Override
		public boolean canApplyToTag(@Nullable final HtmlTag tag) {
			return tag instanceof Img;
		}
	}

	public static class Alt extends ImgAttribute {

		public Alt() {
			super("alt");
		}
	}

	public static class Src extends ImgAttribute {

		public Src() {
			super("src");
		}
	}

	public static class Title extends ImgAttribute {

		public Title() {
			super("title");
		}
	}

	public Img() {
		super("img");
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
