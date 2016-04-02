package openmods.depcheck;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;

public class SourceDependencies {

    private final Map<String, ModInfo> mods = Maps.newHashMap();

    public ModInfo addMod(String pkgPrefix, String modId) {
        final ModInfo mod = new ModInfo(pkgPrefix, modId);

        final Optional<ModInfo> conflict = mods.values().stream().filter(m -> m.matchPackage(pkgPrefix) || mod.matchPackage(m.pkgPrefix)).findAny();
        conflict.ifPresent(c -> {
            throw new IllegalArgumentException(String.format("Conflicting mod registration: %s vs %s", pkgPrefix, c.pkgPrefix));
        });

        mods.put(modId, mod);
        return mod;
    }

    public Optional<ModInfo> identifyMod(String pkg) {
        return mods.values().stream().filter(m -> m.matchPackage(pkg)).findAny();
    }

    public ModInfo getMod(String modId) {
        return mods.get(modId);
    }
}
