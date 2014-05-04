AutoIvyDeps
===========

A TeamCity plugin to auto-discover and set up snapshot dependencies based on Ivy dependencies.

The current approach requires that Ivy descriptors are published before it can work. Basically when changes are detected the plugin goes through every active build, checks if it has published an Ivy descriptor, and uses this to detect which build corresponds to the changed descriptor, and to discover dependencies.

