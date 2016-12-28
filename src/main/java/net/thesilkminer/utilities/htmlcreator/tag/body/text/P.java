package net.thesilkminer.utilities.htmlcreator.tag.body.text;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nonnull;

/**
 * Represents a P tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class P extends HtmlTag {

	public P() {
		super("p");
	}

	@Override
	public void setContent(@Nonnull final String content) {
		final String[] lines = content.split("\\n");
		String realContent = "";
		for (final String line : lines) realContent += line + "<br />\n";
		realContent = realContent.substring(0, realContent.length() - "<br />\n".length());
		super.setContent(realContent);
	}
}
