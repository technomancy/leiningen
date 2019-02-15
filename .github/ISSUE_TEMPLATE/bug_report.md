---
name: Bug Report
about: Create a report to help us improve
---

**Initial debugging steps**
Before creating a report, _especially_ around exceptions being thrown when running Leiningen, please check if the error still occurs after:

- [ ] Updating to using the latest released version of Leiningen.
- [ ] Moving your `~/.lein/profiles.clj` (if present) out of the way. This contains third-party dependencies and plugins that can cause problems inside Leiningen.
- [ ] Updating any old versions of plugins in your `project.clj`. Old versions of plugins like nREPL and CIDER can cause problems with newer versions of Leiningen.
- [ ] (If you are using Java 9 or newer), updating your dependencies to their most recent versions. Recent JDK's have introduced changes which can break some Clojure libraries.

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Run the command '...'
3. See error

**Actual behavior**
What actually happened?

**Expected behavior**
A clear and concise description of what you expected to happen.

**Link to sample project**
If relevant, please provide a link to a project or a `project.clj` that others can use to help debug the issue.

**Logs**
If applicable, add logs to help explain your problem, including the command that you ran. If the logs are very large, please put them in a [gist](https://gist.github.com/).

**Environment**
- Leiningen Version: [e.g. 2.9.0]. Get this by running `lein version`.
- JDK Version: [e.g. openjdk version "11.0.1"]. Get this by running `java -version`.
- OS: [e.g. Linux, macOS].
- Anything else that might be relevant to your problem?

**Additional context**
Add any other context you have about the problem here. Did this work previously on older versions of Leiningen, or older JDK's?
