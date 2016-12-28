package net.thesilkminer.utilities.htmlcreator.tag.body.details;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nullable;

/**
 * Represents the summary tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public class Summary extends HtmlTag {

	public Summary() {
		super("summary");
	}

	@Override
	public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
		return tag instanceof Details;
	}
}
