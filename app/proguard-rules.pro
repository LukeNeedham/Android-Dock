# Rules for the release build (R8). The manifest's activities and service are kept automatically, and
# the libraries ship their own rules. Add a rule here only if a release build breaks on something that
# is looked up by name at run time.

# The shared debug library reads and writes these as JSON. It has not been tested with R8, and release
# builds still save crashes, so keep them from being renamed.
-keep class com.lukeneedham.common.logger.LogEntry { *; }
-keep class com.lukeneedham.common.crash.CrashEntry { *; }
