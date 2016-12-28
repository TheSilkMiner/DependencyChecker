package net.thesilkminer.utilities.htmlcreator.tag.head;

import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;
import net.thesilkminer.utilities.htmlcreator.tag.base.Head;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Contract;

import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * Represents the style tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Style extends Head.HeadHtmlTag {

	public static class Type extends HtmlAttribute {

		public enum Types {
			TEXT_CSS;

			@Contract(pure = true)
			@Nonnull
			@Override
			public String toString() {
				return super.toString().toLowerCase(Locale.ENGLISH).replace('_', '/');
			}
		}

		public Type() {
			super("type");
		}

		@Override
		public void setValue(@Nonnull final String value) {
			try {
				Types.valueOf(value);
			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid value", e);
			}
			super.setValue(Types.valueOf(value).toString());
		}
	}

	public Style() {
		super("style");
	}
}
