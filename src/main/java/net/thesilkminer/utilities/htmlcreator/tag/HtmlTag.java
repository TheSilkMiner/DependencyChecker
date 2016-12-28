package net.thesilkminer.utilities.htmlcreator.tag;

import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.Contract;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract class used to represent a general HTML tag.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public abstract class HtmlTag {

	private final String tagName;
	private final List<HtmlTag> children;
	private final List<HtmlAttribute> attributes;
	private String content;
	private boolean voidElement;

	protected HtmlTag(@Nonnull final String tagName) {
		this.tagName = Preconditions.checkNotNull(tagName);
		this.children = Lists.newArrayList();
		this.attributes = Lists.newArrayList();
		this.content = "";
		this.voidElement = false;
	}

	public boolean canBeChildrenOf(@Nullable final HtmlTag tag) {
		return tag != null;
	}

	public boolean mustBeEmpty() {
		return this.canBeEmpty();
	}

	public boolean canBeEmpty() {
		return false;
	}

	@Nonnull
	public String getTagName() {
		return this.tagName;
	}

	public boolean isVoidElement() {
		return this.voidElement;
	}

	public void setVoidElement(final boolean voidElement) {
		if (!voidElement) Preconditions.checkState(!this.mustBeEmpty());
		if (voidElement) Preconditions.checkState(this.canBeEmpty());

		this.voidElement = voidElement;

		if (this.voidElement) {
			this.children.clear();
			this.content = "";
		}
	}

	@Nonnull
	public List<HtmlTag> getChildren() {
		return ImmutableList.copyOf(this.children);
	}

	public void addChild(@Nonnull final HtmlTag tag) {
		Preconditions.checkState(!this.isVoidElement(), "Tag must not be void");
		Preconditions.checkArgument(tag.canBeChildrenOf(this));
		this.children.add(Preconditions.checkNotNull(tag));
	}

	@Nonnull
	public List<HtmlAttribute> getAttributes() {
		return ImmutableList.copyOf(this.attributes);
	}

	public void addAttribute(@Nonnull final HtmlAttribute attribute) {
		Preconditions.checkState(!this.getAttributes().contains(Preconditions.checkNotNull(attribute)));
		Preconditions.checkArgument(attribute.canApplyToTag(this), "Attribute can't be applied");
		this.attributes.add(attribute);
	}

	@Nonnull
	public String getContent() {
		return this.content;
	}

	public void setContent(@Nonnull final String content) {
		this.content = Preconditions.checkNotNull(content);
	}

	@Nonnull
	protected String indentToStringRepresentation(@Nonnull final String rep) {
		final String[] lines = rep.split("\\n");
		String line = "";
		for (final String ln : lines) line += "\t" + ln + "\n";
		return line.substring(0, line.length() - 1);
	}

	@Contract(pure = true)
	@Nonnull
	@Override
	public final String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append('<').append(this.getTagName());
		this.attributes.forEach(it -> builder.append(" ").append(it));
		if (this.isVoidElement()) {
			return builder.append(" />").toString();
		}
		builder.append('>');
		if (!this.getChildren().isEmpty()) {
			this.getChildren().forEach(it -> builder.append('\n').append(this.indentToStringRepresentation(it.toString())));
		}
		if (!this.getContent().isEmpty()) {
			if (this.getContent().contains("\n")) builder.append("\n").append(this.indentToStringRepresentation(this.getContent())).append('\n');
			else builder.append(this.getContent());
		}
		if (builder.toString().endsWith(">")) builder.append('\n');
		builder.append("</").append(this.getTagName()).append('>');
		return builder.toString();
	}
}
