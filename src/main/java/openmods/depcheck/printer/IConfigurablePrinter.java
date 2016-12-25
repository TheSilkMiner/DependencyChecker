package openmods.depcheck.printer;

import java.io.File;
import javax.annotation.Nullable;

/**
 * A printer which can (and/or must) be configured through
 * an external configuration file.
 *
 * @author TheSilkMiner
 * @since 0.1
 */
public interface IConfigurablePrinter extends IPrinter {

	/**
	 * Method called before printing which must be used by
	 * implementations to load the configuration needed for
	 * the task.
	 *
	 * @param file
	 *      The file from where to load the configuration.
	 *      It is guaranteed that the specified file exists and
	 *      is readable. If either or both of the above conditions
	 *      are not met, the supplied file is {@code null}.
	 *
	 * @since 0.1
	 */
	void populateSettings(@Nullable final File file);

	/**
	 * Defines if the configuration settings is needed before
	 * printing.
	 *
	 * <p>If this method returns {@code true} and the configuration
	 * could not be applied (an {@link Exception} is thrown inside
	 * {@link #populateSettings(File)}, then the entire software comes
	 * to a forced halt.</p>
	 *
	 * <p>If this method returns {@code false}, no action is taken.
	 * If implementors decide to return {@code false} from this method,
	 * though, their printing method must <b>NOT</b> fail in case the
	 * configuration could not be applied.</p>
	 *
	 * @implNote By default this method returns {@code true}.
	 *
	 * @return
	 *      If the configuration must be applied before printing.
	 *
	 * @since 0.1
	 */
	default boolean isConfigurationNeeded() {
		return true;
	}
}
