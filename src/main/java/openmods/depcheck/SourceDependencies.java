package openmods.depcheck;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Maps;

public class SourceDependencies implements Serializable {
    private static final long serialVersionUID = -8421936165213857432L;

    private final Map<String, ModInfo> mods = Maps.newHashMap();

    public ModInfo addMod(String pkgPrefix, String modId) {
        ModInfo result = mods.get(modId);
        if (result == null || !result.pkgPrefix.equals(pkgPrefix)) {
            result = new ModInfo(pkgPrefix, modId);
            mods.put(modId, result);
        }
        return result;
    }

    public Optional<ModInfo> identifyMod(String pkg) {
        return mods.values().stream().filter(m -> m.matchPackage(pkg)).findAny();
    }

    public ModInfo getMod(String modId) {
        return mods.get(modId);
    }

    public Set<String> getAllModIds() {
        return mods.keySet();
    }

    public boolean isUpdated() {
        return mods.values().stream().anyMatch(ModInfo::isUpdated);
    }
}
