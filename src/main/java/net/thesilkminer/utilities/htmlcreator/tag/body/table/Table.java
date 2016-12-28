package net.thesilkminer.utilities.htmlcreator.tag.body.table;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the table tag.
 */
public class Table extends HtmlTag {

	static class TableElement extends HtmlTag {

		protected TableElement(@Nonnull final String tagName) {
			super(tagName);
		}

		@Override
		public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
			return tag instanceof Table || tag instanceof TableElement;
		}
	}

	public Table() {
		super("table");
	}
}
