package net.thesilkminer.utilities.htmlcreator.tag;

import net.thesilkminer.utilities.htmlcreator.attribute.HtmlAttribute;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.Contract;

import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Useful utility class that can be used to turn every tag into a builder for itself.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public final class TagWrapper<T extends HtmlTag> {

	private final T wrapped;

	private TagWrapper(@Nonnull final T tag) {
		this.wrapped = tag;
	}

	@Nonnull
	public static <T extends HtmlTag> TagWrapper<T> builder(@Nonnull final T tag) {
		return new TagWrapper<>(Preconditions.checkNotNull(tag));
	}

	@Nonnull
	public static <T extends HtmlTag> TagWrapper<T> builder(@Nonnull final Class<T> tag) {
		try {
			return builder(tag.getConstructor().newInstance());
		} catch (final ReflectiveOperationException e) {
			throw Throwables.propagate(e);
		}
	}

	@Nonnull
	public TagWrapper<T> voidElement(final boolean voidElement) {
		this.wrapped.setVoidElement(voidElement);
		return this;
	}

	@Nonnull
	public TagWrapper<T> child(@Nonnull final HtmlTag tag) {
		this.wrapped.addChild(tag);
		return this;
	}

	@Nonnull
	public TagWrapper<T> childList(@Nonnull final Collection<HtmlTag> tags) {
		tags.forEach(this::child);
		return this;
	}

	@Nonnull
	public TagWrapper<T> attribute(@Nonnull final HtmlAttribute attribute) {
		this.wrapped.addAttribute(attribute);
		return this;
	}

	@Nonnull
	public TagWrapper<T> content(@Nonnull final String content) {
		this.wrapped.setContent(content);
		return this;
	}

	@Contract(pure = true)
	@Nonnull
	public T build() {
		return this.wrapped;
	}

	@Contract(pure = true)
	@Nonnull
	@Override
	public final String toString() {
		return this.build().toString();
	}
}
