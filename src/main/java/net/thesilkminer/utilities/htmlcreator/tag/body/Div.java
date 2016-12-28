package net.thesilkminer.utilities.htmlcreator.tag.body;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;
import net.thesilkminer.utilities.htmlcreator.tag.base.Body;

import javax.annotation.Nullable;

/**
 * Represents the div tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Div extends Body.BodyHtmlTag {

	public Div() {
		super("div");
	}

	@Override
	public boolean canBeEmpty() {
		return false;
	}

	@Override
	public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
		return tag != null;
	}
}
