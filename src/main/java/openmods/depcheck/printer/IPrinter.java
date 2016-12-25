package openmods.depcheck.printer;

import openmods.depcheck.dependencies.DependencyResolveResult;
import openmods.depcheck.parser.SourceDependencies;

import java.io.File;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * A functional interface representing a printer.
 *
 * <p>Implementations of this interface are not tied to any specific contract.
 * The printing can be done on whatever resource is desired. A {@link File} is
 * provided for ease of use though.</p>
 *
 * <p>Refer to {@link #print(File, SourceDependencies, Collection)} for more information.</p>
 *
 * <p>Every implementation must supply a parameter-less constructor for ease
 * of creation. In case a parametric constructor is needed, you should refrain
 * from doing so and implement {@link IConfigurablePrinter} instead: this allows
 * you to read settings and/or set various fields prior to printing.</p>
 *
 * @author TheSilkMiner
 * @since 0.1
 */
@FunctionalInterface
public interface IPrinter {

	/**
	 * Prints the results of the entire parsing and collection to the output
	 * specified by this {@link IPrinter} instance.
	 *
	 * <p>Usually this should be a file, but it can be whatever the implementation
	 * chooses it to be.</p>
	 *
	 * @param file
	 *      The file where the output should be located.
	 * @param availableDependencies
	 *      A {@link SourceDependencies} with all the dependencies currently satisfied.
	 *      They can either represent missing classes or an "all ok" check.
	 * @param results
	 *      A collection containing all the results of the compatibility check.
	 *
	 * @since 0.1
	 */
	void print(@Nonnull final File file,
	           @Nonnull final SourceDependencies availableDependencies,
	           @Nonnull final Collection<DependencyResolveResult> results);
}
