package provided.core;

import clojure.lang.RT;

public class Example {

public static void
main(String... args) {
    System.exit(
        RT.intCast(
            RT.var("clojure.core", "read-string").invoke("0")));
}

}
