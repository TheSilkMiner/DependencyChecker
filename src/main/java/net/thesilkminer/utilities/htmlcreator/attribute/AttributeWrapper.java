package net.thesilkminer.utilities.htmlcreator.attribute;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * Useful utility class that can be used to turn every tag into a builder for itself.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
public final class AttributeWrapper<T extends HtmlAttribute> {

	private final T wrapped;

	private AttributeWrapper(@Nonnull final T tag) {
		this.wrapped = tag;
	}

	@Nonnull
	public static <T extends HtmlAttribute> AttributeWrapper<T> builder(@Nonnull final T attribute) {
		return new AttributeWrapper<>(Preconditions.checkNotNull(attribute));
	}

	@Nonnull
	public static <T extends HtmlAttribute> AttributeWrapper<T> builder(@Nonnull final java.lang.Class<T> attribute) {
		try {
			return builder(attribute.getConstructor().newInstance());
		} catch (final ReflectiveOperationException e) {
			throw Throwables.propagate(e);
		}
	}

	@Nonnull
	public AttributeWrapper<T> value(@Nonnull final String value) {
		this.wrapped.setValue(value);
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
