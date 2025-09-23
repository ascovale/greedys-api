Custom OpenAPI Generator (Dart)
================================

Build JAR:
  mvn -q -DskipTests package

Result:
  target/custom-dart-codegen-1.0.0-SNAPSHOT.jar
  target/lib/ (runtime deps)

Use with dockerized openapi-generator (mount jar + deps):
  docker run --rm -v "$PWD:/local" -w /local \
    -v "$PWD/common_lib/generator/target:/opt/custom" \
    openapitools/openapi-generator-cli generate \
      -g dart-dio \
      -c custom-config.json \
      --generator-class main.java.org.openapitools.custom.CustomDartClientCodegen \
      -i swagger.json -o out

Simpler (copy fat dir to single folder and classpath env):
  export JAVA_OPTS="-cp /opt/custom/custom-dart-codegen-1.0.0-SNAPSHOT.jar:/opt/custom/lib/*"
  (then run normal generate with --generator-class)

PowerShell (Windows + WSL): see integration in root script once added.
Custom Dart OpenAPI Generator extension

This folder contains a minimal OpenAPI Generator extension that loads a JSON file called `response-wrappers.json` and marks response wrapper models so templates can avoid generating them.

Files added:
- `src/main/java/org/openapitools/custom/CustomDartClientCodegen.java` - extension of the Dart client codegen that reads `response-wrappers.json` and sets vendorExtensions.
- `src/main/java/org/openapitools/custom/WrapperInfo.java` - small holder POJO for wrapper metadata.
- `../../tools/download_wrappers.ps1` - script to download `response-wrappers.json` from the server.

Usage
1. From `restaurant_app`, run the download script:

```powershell
.\.\tools\download_wrappers.ps1
```

2. Build this extension into a jar using your usual Java build (Maven/Gradle). Example (not included):

```
# mvn package
```

3. Pass the generator extension jar to OpenAPI Generator with `-t` or `--library` flags depending on your setup, or install the extension into the generator runtime.

Notes
- The extension expects `response-wrappers.json` to be in the working directory where the generator runs.
- Template modifications are necessary to fully skip imports and types; see earlier conversation for mustache snippets.
