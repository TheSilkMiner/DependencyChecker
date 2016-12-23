# DependencyChecker
**Status for developing branch (`master`):** [![Build Status (Travis)](https://travis-ci.org/TheSilkMiner/DependencyChecker.svg?branch=master)](https://travis-ci.org/TheSilkMiner/DependencyChecker)

This is small utility for checking compatibility between mods, based on static analysis.
Created for OpenPeripheralIntegration, but maybe somebody will find an alternative use.

Program accepts directories with following structure:
```
data\
  mods\
    modA\
      modA_X.Y.Z.jar
      modA_X.Y.W.jar
      ...
      meta.json
    modB\
      ...
    ...
  targets\
    targetMod_X.Y.Z.jar
    targetMod_X.Y.W.jar
    ...
```

Example meta.json:
```json
{
    "mod" : "forestry",
    "package" : "forestry",
    "patterns" : [
        {
            "pattern" : "forestry_([0-9.]+)-([0-9.]+).jar",
            "versionGroup" : 2
        }
    ]
}
```
