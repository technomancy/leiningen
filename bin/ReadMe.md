Leiningen Bash Testing
======================

The Bash files in this directory have a test suite.

To run the test suite:

```bash
make test
```

To run the test suite in verbose mode:

```bash
make test v=1
```

To run a single test file:

```bash
make test t=test/00-shellcheck.t
```

To remove the generated test support files:

```bash
make clean
```


## Dependencies

The `test/00-shellcheck.t` test requires the `shellcheck` (v0.9.0+) command.
You can get it here:
https://github.com/koalaman/shellcheck/releases/tag/v0.9.0
