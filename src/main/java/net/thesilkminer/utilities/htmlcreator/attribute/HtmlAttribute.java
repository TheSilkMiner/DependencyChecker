package net.thesilkminer.utilities.htmlcreator.attribute;

import net.thesilkminer.utilities.htmlcreator.tag.HtmlTag;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract class used to represent a generic HTML attribute.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public abstract class HtmlAttribute {

	private final String name;
	private String value;

	protected HtmlAttribute(@Nonnull final String name) {
		this.name = Preconditions.checkNotNull(name);
		this.value = "";
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getValue() {
		return this.value;
	}

	public void setValue(@Nonnull final String value) {
		this.value = Preconditions.checkNotNull(value);
	}

	public boolean canApplyToTag(@Nullable final HtmlTag tag) {
		return tag != null;
	}

	@Contract(pure = true)
	@Nonnull
	@Override
	public final String toString() {
		return this.name + "=\"" + this.value.replace('"', '\'') + "\"";
	}
}
