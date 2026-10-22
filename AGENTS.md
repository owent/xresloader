# xresloader development

- Read `doc/parallelism.md` before changing conversion state, input caches, or batch scheduling.
- Keep per-conversion options, scheme bindings, validators, script engines and mutable descriptor extensions in task TLS. Clear them when a worker finishes, including failure paths. Preserve existing singleton accessors for callback compatibility.
- Keep IO data caches global, keyed by resolved input identity. Publish complete read-only data under a write lock and allow concurrent cache hits under a read lock. Do not call an access-order LRU mutation under a read lock.
- Excel streaming loads use per-file locks outside the global index lock. Retain each file lock until all builders and waiters finish, including failed loads and eviction paths.
- POI Workbook and FormulaEvaluator are mutable sessions. Share input bytes, not these objects, between conversions; close task-owned workbooks.
- Preserve input-order file writes, UE first-write deduplication, and import-settings merging. Keep accepted work bounded and wait for failed as well as successful tasks before dumping mappings or exiting.
- For behavior changes, save a pre-change executable JAR outside `target`, then compare all sample outputs using `tools/verify_samples.py` in the same fixture directory. Check default execution and `--parallelism 1`, `4`, `8`; do not normalize output bytes or overwrite the baseline to hide differences.
- Run `mvn clean verify` and Gradle `test shadowJar` when relevant, then `mvn verify` without cleaning to check build interoperability. Keep Maven classes in `target/maven`, separate from Gradle's `target/classes/java`. Both clean tasks remove `target`; avoid concurrent builds. Keep source changes separate from generated sample data.
- Protocol schema caches must include the ordered input identities and dependency-resolution options in their keys. Share only read-only protocol data; task validators, bound extensions and failure state must not enter a global schema cache.
- SharedCache loaders run under per-key locks, outside the global index lock. Keep cache dependencies acyclic (schema -> parsed input -> bytes); copy shared protocol maps before incremental mutation.
- For performance changes, compare old and new JARs in interleaved rounds, record CPU/heap settings and system load, and hash every output. Keep jcstress outcome checks distinct from a whole-program race detector.
- Refer to private benchmark workloads in documentation only as 项目1 and 项目2; omit their real project names and source paths.
- Keep documentation focused on current behavior and reproducible checks; omit task history, local execution records and neighboring-repository details.
