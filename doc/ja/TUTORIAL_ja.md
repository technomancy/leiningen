<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Tutorial](#tutorial)
  - [What This Tutorial Covers](#what-this-tutorial-covers)
  - [Getting Help](#getting-help)
  - [Leiningen Projects](#leiningen-projects)
  - [Creating a Project](#creating-a-project)
    - [Directory Layout](#directory-layout)
    - [Filename-to-Namespace Mapping Convention](#filename-to-namespace-mapping-convention)
  - [project.clj](#projectclj)
  - [Dependencies](#dependencies)
    - [Overview](#overview)
    - [Artifact IDs, Groups, and Versions](#artifact-ids-groups-and-versions)
    - [Snapshot Versions](#snapshot-versions)
    - [Repositories](#repositories)
    - [Checkout Dependencies](#checkout-dependencies)
    - [Search](#search)
  - [Setting JVM Options](#setting-jvm-options)
  - [Running Code](#running-code)
  - [Tests](#tests)
  - [Profiles](#profiles)
  - [What to do with it](#what-to-do-with-it)
    - [Uberjar](#uberjar)
    - [Framework (Uber)jars](#framework-uberjars)
    - [Server-side Projects](#server-side-projects)
    - [Publishing Libraries](#publishing-libraries)
  - [That's It!](#thats-it)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->



# チュートリアル

Leiningen は Clojure プロジェクトを、髪の毛が燃え上がるような思いをせずに、
自動化するためのものです。もしこのチュートリアルにしたがっていて、
髪の毛が燃え上がるような思いをしたり何かイライラするようなことがあった場合は、
ぜひ[私達に知らせてください](https://github.com/technomancy/leiningen/issues/new)。

Leiningen は様々なプロジェクトに関連するタスクを提案します:

 * 新しいプロジェクトを作成する
 * プロジェクトの依存関係をダウンロードする
 * テストを実行する
 * 完全に設定された REPL の実行
 * Java ソースコード(もしあれば)のコンパイル
 * プロジェクトの実行(もしプロジェクトがライブラリでなければ)
 * プロジェクト相互運用のための maven スタイルの pom ファイルの生成
 * 開発時のプロジェクトのコンパイルとパッケージ
 * [Clojars](https://clojars.org)などのレポジトリを使ったライブラリの公開
 * Clojure で書かれたカスタム自動化タスクの実行(leiningen のプラグイン)

もしあなたが Java の世界からやってきたのであれば、
「Leiningen は Maven と Ant のいい感じのあいのこ」と考えればよいでしょう。
Ruby や Python の仲間たちにとっては、 Leiningen は
RubyGems と Bundler と Rake
もしくは pip と Fabric を一つのツールに組み合わせたものです。


## このチュートリアルの取り扱い範囲

このチュートリアルはプロジェクトの構造、依存関係の管理、
テストの実行、 REPL 、及び開発に関するトピックを簡単に説明します。

JVM は完全に初めての人たち、 Ant や Maven に怒りを感じたことのない人も
慌てないでください。 Leiningen はあなたたちのために設計されています。
このチュートリアルはあなたが Leiningen を使い始めるのを助けて、
プロジェクト自動化と JVM の世界での依存関係管理の
Leiningen の取り組みについて説明します。


## ヘルプを読むには

Leiningen はかなり包括的なヘルプと、
一緒に配布されていることに留意してください:
`lein help` はタスクの一覧を出してくれますし、
`lein help $TASK` は詳細を提供します。
さらに色々なドキュメント、 readme や サンプルの設定、
このチュートリアルなども同様に提供されています。


## Leiningen プロジェクト

Leiningen は*プロジェクト*とともに動作します。
一つのプロジェクトは、
一連の Clojure (Java であることもあるかもしれません)ソースファイルと、
それらに関するメタデータを格納するディレクトリです。
メタデータは `project.clj` という名前の、
プロジェクトのルートディレクトリにあるファイルに格納されています。
これは Leiningen に以下のような情報を提供します:

 * プロジェクトの名前
 * プロジェクトの説明
 * プロジェクトが依存するライブラリはどれか
 * どのクロージャのバージョンを使うか
 * ソースファイルはどこを探せばよいか
 * アプリケーションのメインの名前空間は何か

などなど

多くの Leiningen タスクはプロジェクトの文脈でのみ理解できるものです。
いくつか(たとえば、 `repl` や `help`)などは任意のディレクトリから呼ぶこともできます。

続いてプロジェクトがどのように作られるのか見てみましょう。

## プロジェクトの作成

わたしたちは、あなたが
[README](https://github.com/technomancy/leiningen/blob/stable/README.md)
にしたがって Leiningen をインストールしたと仮定します。
この時新しいプロジェクトを生成することは簡単です:

    $ lein new app my-stuff

	my-stuff という名前のプロジェクトを 'app' テンプレートに基づいて生成

    $ cd my-stuff
    $ find .
    .
    ./.gitignore
    ./doc
    ./doc/intro.md
    ./LICENSE
    ./project.clj
    ./README.md
    ./resources
    ./src
    ./src/my_stuff
    ./src/my_stuff/core.clj
    ./test
    ./test/my_stuff
    ./test/my_stuff/core_test.clj

この例では `app` テンプレートを使用しています。
このテンプレートはライブラリではなく
アプリケーションのプロジェクトのためのものです。
引数の `app` を省略すると `default` テンプレートが使用されますが、
これはライブラリに適したテンプレートです。

### Directory Layout

Here we've got your project's README, a `src/` directory containing the
code, a `test/` directory, and a `project.clj` file which describes your
project to Leiningen. The `src/my_stuff/core.clj` file corresponds to
the `my-stuff.core` namespace.

### Filename-to-Namespace Mapping Convention

Note that we use `my-stuff.core` instead of just `my-stuff` since
single-segment namespaces are discouraged in Clojure. Also note that
namespaces with dashes in the name will have the corresponding file
named with underscores instead since the JVM has trouble loading files
with dashes in the name. The intricacies of namespaces are a common
source of confusion for newcomers, and while they are mostly outside
the scope of this tutorial you can
[read up on them elsewhere](https://8thlight.com/blog/colin-jones/2010/12/05/clojure-libs-and-namespaces-require-use-import-and-ns.html).

## project.clj

あなたの `project.clj` ファイルは以下のような見た目で始まっているでしょう:

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot my-stuff.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
```

もしあなたが `:description` に簡単な一文を記入しないと、
あなたのプロジェクトを検索結果から見つける事は難しくなるので、
ここから初めましょう。同様に `:url` も間違いなく修正するようにしましょう。
`README.md` ファイルも同様です。しかし今は飛ばして前に進み、
`:dependencies` を設定しましょう。
Clojure はここでは単に依存関係の一つでしかないことに留意してください。
多くの言語とはちがい、 Clojure のどのバージョンにも簡単に差し替えられます。

## 依存関係

### 概要

Clojure はホスト言語であり Clojure ライブラリは
その他の JVM 言語と同じ方法で配布されます: jar ファイルとして。

Jar ファイルは基本的には `.zip` ファイルに
JVM 特有のメタデータを追加したものです。
Jar ファイルは通常 `.class` ファイル(JVM のバイトコード)と
`.clj` ソースを格納しており、また設定ファイルや JavaScript ファイルや、
静的データのテキストファイルなども同様に格納しています。

配布されている JVM ライブラリは*識別子*(アーティファクトグループ、アーティファクト ID)と、*バージョン*を持っています。

### アーティファクト ID 、アーティファクトグループ、バージョン

あなたは
[Clojars を Web インターフェースから検索したり](https://clojars.org/search?q=clj-http)
`lein search $TERM` によって検索することが出来ます。
このチュートリアルを書いている時点での
`clj-http` についての Clojars ページは以下のようになっています:

```clj
[clj-http "2.0.0"]
```

依存関係は Maven や Gradle の文法でも表示されます。
Leiningen の出したバージョンは、
そのまま `project.clj` の `:dependencies` 配列にコピーできます。
なのでたとえば、前の例の `project.clj` の
`:dependencies` 行を変更する場合以下のようになります:

```clj
:dependencies [[org.clojure/clojure "1.8.0"]
               [clj-http "2.0.0"]]
```

Leiningen は `clj-http` の jar を自動的にダウンロードし、
あなたのクラスパスに存在するようにする。
lein で明示的に依存関係をダウンロードしたい場合は
`lein deps` を実行すればよいですが、
これはわざわざ実行せずとも必要に応じて実行されます。

配列中で  "clj-http" は "アーティファクト ID" として参照されます。
"2.0.0" はバージョンです。いくつかのライブラリは "グループ ID" を持っており、
これは以下のように表示されます。

```clj
[com.cedarsoft.utils.legacy/hibernate "1.3.7"]
```

グループ ID はスラッシュの前の部分です。
特に Java ライブラリでは、しばしばドメイン名として予約されています。
Clojure ライブラリはしばしば同じグループ ID とアーティファクト ID を使い
(clj-http のように)、その場合にはグループID を省略できます。
より大きのグループの一部分であるライブラリ
(たとえば `ring-jetty-adapter` は `ring` プロジェクトの一部です)
がある場合、グループ ID はしばしば全てのサブプロジェクトで同じになります。

### スナップショットのバージョン

バージョンは "-SNAPSHOT" で終わることがあります。
これの意味するところは、それは正式なリリースではなく、
開発ビルドだということです。スナップショットの依存関係に頼ることは
おすすめできませんが、リリース前にバグを修正する必要があるときなど、
必要な場合もあります。しかしスナップショットバージョンが
stick around であることは保証されないので、
開発リリース以外のケースで自分の管理下にないスナップショットバージョンに
依存しないようにすることが重要です。
スナップショットの依存関係をプロジェクトに追加すると、
Leiningen がよりアクティブに毎日最新の依存関係のバージョンを
取得するようになるので
(一方で通常のリリースバージョンはローカルレポジトリにキャッシュされます)、
多くのスナップショットを抱えている場合は色々と時間がかかるようになります。

注意点として、いくつかのライブラリは、グループ ID とアーティファクト ID と
それらが jar で提供する名前空間の対応をづけるということです。
これは単なる慣習です。全ての場合に当てはまるという保証はありません。
そのため `:require` 節と `:import` を記述する前に
ライブラリのドキュメントを熟読してください。

### レポジトリ

依存関係は*アーティファクトレポジトリ*に格納されています。
Perl で言えば CPAN 、 Python で言えば Cheeseshop (PyPi ともいいます)、
Ruby であれば rubygems.org 、 Node.js であれば NPM などと、
同じものです。 Leiningen は既存の JVM レポジトリのインフラを再利用します。
いくつかのよく知られたオープンソースのレポジトリがあります。
Leiningen はデフォルトでそれらを使います:
[clojars.org](https://clojars.org) と [Maven Central](https://search.maven.org/) です。

[Clojars](https://clojars.org/) は Clojure コミュニティの集約的な
maven レポジトリで、 [Central](https://search.maven.org/) は
より幅広い JVM コミュにエィのレポジトリです。

サードパーティのレポジトリは `:repositories` キーを project.clj に指定することで追加出来ます。
どのようにすれば良いのか
[sample.project.clj](https://github.com/technomancy/leiningen/blob/stable/sample.project.clj)
を見てみましょう。このサンプルは追加のレポジトリとして Sonatype レポジトリを使っていますが、
これは(Clojure や Java の)ライブラリの、最新のスナップショット開発バージョンへのアクセスを提供します。
このサンプルには同様に、レポジトリに関連する更新頻度などの設定が含まれています。

### Checkout Dependencies

Sometimes it is necessary to develop two projects in parallel but it
is very inconvenient to run `lein install` and restart your repl all
the time to get your changes picked up. Leiningen provides a solution
called *checkout dependencies* (or just *checkouts*). To use it,
create a directory called `checkouts` in the project root, like so:

    .
    |-- project.clj
    |-- README.md
    |-- checkouts
    |-- src
    |   `-- my_stuff
    |       `-- core.clj
    `-- test
        `-- my_stuff
            `-- core_test.clj

Then, under the checkouts directory, create symlinks to the root directories of projects you need.
The names of the symlinks don't matter: Leiningen just follows all of them to find
`project.clj` files to use. Traditionally, they have the same name as the directory they point to.

    .
    |-- project.clj
    |-- README.md
    |-- checkouts
    |   `-- suchwow [link to ~/code/oss/suchwow]
    |   `-- commons [link to ~/code/company/commons]
    |-- src
    |   `-- my_stuff
    |       `-- core.clj
    `-- test
        `-- my_stuff
            `-- core_test.clj

Libraries located under the `checkouts` directory take precedence
over libraries pulled from repositories, but this is not a replacement
for listing the project in your main project's `:dependencies`; it
simply supplements that for convenience. That is, given the above directory hierarchy,
`project.clj` should contain something like:

      :dependencies [[org.clojure/clojure "1.9.0"]
                     ...
                     [suchwow "0.3.9"]
                     [com.megacorp/commons "1.3.5"]
                     ...]
                 

Note here that the Maven groupid `com.megacorp` has no effect on the way checkouts work.
The `suchwow` and `commons` links look the same in `checkouts`, and the groupid
hierarchy doesn't need to appear in the way `commons` is actually laid out on disk.

After you've updated `:dependencies`, `lein` will still need to be able
to find the library in some repository like clojars or your `~/.m2`
directory.  If `lein` complains with a message like "Could not find artifact suchwow:jar:0.3.9",
it's possible that `project.clj` and `suchwow/project.clj` use different version numbers.
It's also possible that you're working on the main project and `suchwow` at the same time,
have bumped the version number in both project files, but still have the old version in your
local Maven repository. Run `lein install` in the `suchwow` directory. That is: the `suchwow`
version number must be the same in *three* places:
in suchwow's `project.clj`, in the main project's `project.clj`, *and in some repository the main project uses*. 

If you change the
dependencies of a checkout project you will still have to run `lein
install` and restart your repl; it's just that source changes will be
picked up immediately.

Checkouts are an opt-in feature; not everyone who is working on the
project will have the same set of checkouts, so your project should
work without checkouts before you push or merge.

Make sure not to override the `base` profile while using checkouts. In practice that usually means using `lein with-profile +foo run` rather than `lein with-profile foo run`.

### Search

Leiningen supports searching remote Maven repositories for matching
jars with the command `lein search $TERM`. Currently only searching
Central and Clojars is supported.

### Maven Read Timeout

The underlying Maven Wagon transport reads the `maven.wagon.rto` system property to determine the timeout used
when downloading artifacts from a repository. The `lein` script sets that property to be 10000. 
If that timeout isn't long enough (for example, when using a slow corporate mirror), 
it can be overridden via LEIN_JVM_OPTS:

```bash
export LEIN_JVM_OPTS="-Dmaven.wagon.rto=1800000"
``` 

## Setting JVM Options

To pass extra arguments to the JVM, set the `:jvm-opts` vector. This will override any default JVM opts set by Leiningen.

```clj
 :jvm-opts ["-Xmx1g"]
```

If you want to pass [compiler options](https://clojure.org/reference/compilation#_compiler_options) to the Clojure compiler, you also do this here.

```
:jvm-opts ["-Dclojure.compiler.disable-locals-clearing=true"
           "-Dclojure.compiler.elide-meta=[:doc :file :line :added]" 
           ; notice the array is not quoted like it would be if you passed it directly on the command line.
           "-Dclojure.compiler.direct-linking=true"]
```

You can also pass options to Leiningen in the `JVM_OPTS` environment variable. If you want to provide the Leiningen JVM with custom options, set them in `LEIN_JVM_OPTS`.

## Running Code

Enough setup; let's see some code running. Start with a REPL
(read-eval-print loop):

    $ lein repl
    nREPL server started on port 55568 on host 127.0.0.1 - nrepl://127.0.0.1:55568
    REPL-y 0.3.0
    Clojure 1.5.1
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
        Exit: Control+D or (exit) or (quit)
     Results: Stored in vars *1, *2, *3, an exception in *e

    user=>

The REPL is an interactive prompt where you can enter arbitrary code
to run in the context of your project. Since we've added `clj-http` to
`:dependencies`, we are able to load it here along with code from the
`my-stuff.core` namespace in your project's own `src/` directory:

    user=> (require 'my-stuff.core)
    nil
    user=> (my-stuff.core/-main)
    Hello, World!
    nil
    user=> (require '[clj-http.client :as http])
    nil
    user=> (def response (http/get "https://leiningen.org"))
    #'user/response
    user=> (keys response)
    (:status :headers :body :request-time :trace-redirects :orig-content-encoding)

The call to `-main` shows both println output ("Hello, World!") and
the return value (nil) together.

Built-in documentation is available via `doc`, and you can examine the
source of functions with `source`:

    user=> (source my-stuff.core/-main)
    (defn -main
      "I don't do a whole lot."
      [& args]
      (println "Hello, World!"))

    user=> ; use control+d to exit

If you already have code in a `-main` function ready to go and don't
need to enter code interactively, the `run` task is simpler:

    $ lein run
    Hello, World!

Providing a `-m` argument will tell Leiningen to look for
the `-main` function in another namespace. Setting a default `:main` in
`project.clj` lets you omit `-m`.

For long-running `lein run` processes, you may wish to save memory
with the higher-order trampoline task, which allows the Leiningen JVM
process to exit before launching your project's JVM.

    $ lein trampoline run -m my-stuff.server 5000

If you have any Java to be compiled in `:java-source-paths` or Clojure
namespaces listed in `:aot`, they will always be compiled before
Leiningen runs any other code, via any `run`, `repl`,
etc. invocations.

## Tests

We haven't written any tests yet, but we can run the failing tests
included from the project template:

    $ lein test

    lein test my-stuff.core-test

    lein test :only my-stuff.core-test/a-test

    FAIL in (a-test) (core_test.clj:7)
    FIXME, I fail.
    expected: (= 0 1)
      actual: (not (= 0 1))

    Ran 1 tests containing 1 assertions.
    1 failures, 0 errors.
    Tests failed.

Once we fill it in the test suite will become more useful. Sometimes
if you've got a large test suite you'll want to run just one or two
namespaces at a time; `lein test my-stuff.core-test` will do that. You
also might want to break up your tests using test selectors; see `lein
help test` for more details.

Running `lein test` from the command-line is suitable for regression
testing, but the slow startup time of the JVM makes it a poor fit for
testing styles that require tighter feedback loops. In these cases,
either keep a repl open for running the appropriate call to
[clojure.test/run-tests](https://clojuredocs.org/clojure.test/run-tests)
or look into editor integration such as
[clojure-test-mode](https://github.com/technomancy/clojure-mode).

Keep in mind that while keeping a running process around is convenient,
it's easy for that process to get into a state that doesn't reflect
the files on disk—functions that are loaded and then deleted from the
file will remain in memory, making it easy to miss problems arising
from missing functions (often referred to as "getting
slimed"). Because of this it's advised to do a `lein test` run with a
fresh instance periodically in any case, perhaps before you commit.

## Profiles

Profiles are used to add various things into your project map in
different contexts. For instance, during `lein test` runs, the
contents of the `:test` profile, if present, will be merged into your
project map. You can use this to enable configuration that should only
be applied during test runs, either by adding directories containing
config files to your classpath via `:resource-paths` or by other
means. See `lein help profiles` for more details.

Unless you tell it otherwise, Leiningen will merge the default set of
profiles into the project map. This includes user-wide settings from
your `:user` profile, the `:dev` profile from `project.clj` if
present, and the built-in `:base` profile which contains dev tools
like nREPL and optimizations which help startup time at the expense of
runtime performance. Never benchmark with the default profiles. (See
the FAQ entry for "tiered compilation")

## What to do with it

Generally speaking, there are three different goals that are typical
of Leiningen projects:

* An application you can distribute to end-users
* A server-side application
* A library for other Clojure projects to consume

For the first, you typically build an uberjar. For libraries, you will
want to have them published to a repository like Clojars or a private
repository. For server-side applications it varies as described below.
Generating a project with `lein new app myapp` will start you out with
a few extra defaults suitable for non-library projects, or you can
browse the
[available templates on Clojars](https://clojars.org/search?q=lein-template)
for things like specific web technologies or other project types.

### Uberjar

The simplest thing to do is to distribute an uberjar. This is a single
standalone executable jar file most suitable for giving to
nontechnical users. For this to work you'll need to specify a
namespace as your `:main` in `project.clj` and ensure it's also AOT (Ahead Of Time)
compiled by adding it to `:aot`. By this point, our `project.clj` file
should look like this:

```clj
(defproject my-stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "2.0.0"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.4.0"]]}}
  :main my-stuff.core
  :aot [my-stuff.core])
```

We have also added a development dependency, `ring-devel`. `ring-devel` will not
be available in uberjars, and will not be considered a dependency if you publish
this project to a repository.

The namespace you specify will need to contain a `-main` function that
will get called when your standalone jar is run. This namespace should
have a `(:gen-class)` declaration in the `ns` form at the top. The
`-main` function will get passed the command-line arguments. Let's try
something easy in `src/my_stuff/core.clj`:

```clj
(ns my-stuff.core
  (:gen-class))

(defn -main [& args]
  (println "Welcome to my project! These are your args:" args))
```

Now we're ready to generate your uberjar:

    $ lein uberjar
    Compiling my-stuff.core
    Created /home/phil/my-stuff/target/uberjar+uberjar/my-stuff-0.1.0-SNAPSHOT.jar
    Created /home/phil/my-stuff/target/uberjar/my-stuff-0.1.0-SNAPSHOT-standalone.jar

This creates a single jar file that contains the contents of all your
dependencies. Users can run it with a simple `java` invocation,
or on some systems just by double-clicking the jar file.

    $ java -jar my-stuff-0.1.0-SNAPSHOT-standalone.jar Hello world.
    Welcome to my project! These are your args: (Hello world.)

You can run a regular (non-uber) jar with the `java`
command-line tool, but that requires constructing the classpath
yourself, so it's not a good solution for end-users.

Of course if your users already have Leiningen installed, you can
instruct them to use `lein run` as described above.

### Framework (Uber)jars

Many Java frameworks expect deployment of a jar file or derived archive
sub-format containing a subset of the application's necessary
dependencies.  The framework expects to provide the missing dependencies
itself at run-time.  Dependencies which are provided by a framework in
this fashion may be specified in the `:provided` profile.  Such
dependencies will be available during compilation, testing, etc., but
won't be included by default by the `uberjar` task or plugin tasks
intended to produce stable deployment artifacts.

For example, Hadoop job jars may be just regular (uber)jar files
containing all dependencies except the Hadoop libraries themselves:

```clj
(project example.hadoop "0.1.0"
  ...
  :profiles {:provided
             {:dependencies
              [[org.apache.hadoop/hadoop-core "1.2.1"]]}}
  :main example.hadoop)
```

    $ lein uberjar
    Compiling example.hadoop
    Created /home/xmpl/src/example.hadoop/example.hadoop-0.1.0.jar
    Created /home/xmpl/src/example.hadoop/example.hadoop-0.1.0-standalone.jar
    $ hadoop jar example.hadoop-0.1.0-standalone.jar
    12/08/24 08:28:30 INFO util.Util: resolving application jar from found main method on: example.hadoop
    12/08/24 08:28:30 INFO flow.MultiMapReducePlanner: using application jar: /home/xmpl/src/example.hadoop/./example.hadoop-0.1.0-standalone.jar
    ...

Plugins are required to generate framework deployment jar derivatives
(such as WAR files) which include additional metadata, but the
`:provided` profile provides a general mechanism for handling the
framework dependencies.

### Server-side Projects

There are many ways to get your project deployed as a server-side
application. Aside from the obvious uberjar approach, simple
programs can be packaged up as tarballs with accompanied shell scripts
using the [lein-tar plugin](https://github.com/technomancy/lein-tar)
and then deployed using
[pallet](https://hugoduncan.github.com/pallet/),
[chef](https://chef.io/), or other mechanisms.
Web applications may be deployed as uberjars using embedded Jetty with
`ring-jetty-adapter` or as .war (web application archive) files
created by the
[lein-ring plugin](https://github.com/weavejester/lein-ring). For
things beyond uberjars, server-side deployments are so varied that they
are better-handled using plugins rather than tasks that are built-in
to Leiningen itself.

It's possible to involve Leiningen during production, but there are
many subtle gotchas to that approach; it's strongly recommended to use
an uberjar if you can. If you need to launch with the `run` task, you
should use `lein trampoline run` in order to save memory, otherwise
Leiningen's own JVM will stay up and consume unnecessary memory.

In addition it's very important to ensure you take steps to freeze all
the dependencies before deploying, otherwise it could be easy to end
up with
[unrepeatable deployments](https://github.com/technomancy/leiningen/wiki/Repeatability).
Consider including `~/.m2/repository` in your unit of deployment
(tarball, .deb file, etc) along with your project code. It's
recommended to use Leiningen to create a deployable artifact in a
continuous integration setting. For example, you could have a
[Jenkins](https://jenkins-ci.org) CI server run your project's full
test suite, and if it passes, upload a tarball to S3.  Then deployment
is just a matter of pulling down and extracting the known-good tarball
on your production servers. Simply launching Leiningen from a checkout
on the server will work for the most basic deployments, but as soon as
you get a number of servers you run the risk of running with a
heterogeneous cluster since you're not guaranteed that each machine
will be running with the exact same codebase.

Also remember that the default profiles are included unless you
specify otherwise, which is not suitable for production. Using `lein
trampoline with-profile production run -m myapp.main` is
recommended. By default the production profile is empty, but if your
deployment includes the `~/.m2/repository` directory from the CI run
that generated the tarball, then you should add its path as
`:local-repo` along with `:offline? true` to the `:production`
profile. Staying offline prevents the deployed project from diverging
at all from the version that was tested in the CI environment.

Given these pitfalls, it's best to use an uberjar if possible.

### Publishing Libraries

If your project is a library and you would like others to be able to
use it as a dependency in their projects, you will need to get it into
a public repository. While it's possible to
[maintain your own private repository](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md)
or get it into [Central](https://search.maven.org), the easiest way is
to publish it at [Clojars](https://clojars.org). Once you have
[created an account](https://clojars.org/register) there, publishing
is easy:

    $ lein deploy clojars
    Created ~/src/my-stuff/target/my-stuff-0.1.0-SNAPSHOT.jar
    Wrote ~/src/my-stuff/pom.xml
    No credentials found for clojars
    See `lein help deploying` for how to configure credentials.
    Username: me
    Password:
    Retrieving my-stuff/my-stuff/0.1.0-SNAPSHOT/maven-metadata.xml (1k)
        from https://clojars.org/repo/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/my-stuff-0.1.0-20120531.032047-14.jar (5k)
        to https://clojars.org/repo/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/my-stuff-0.1.0-20120531.032047-14.pom (3k)
        to https://clojars.org/repo/
    Retrieving my-stuff/my-stuff/maven-metadata.xml (1k)
        from https://clojars.org/repo/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/maven-metadata.xml (1k)
        to https://clojars.org/repo/
    Sending my-stuff/my-stuff/maven-metadata.xml (1k)
        to https://clojars.org/repo/

Once that succeeds it will be available as a package on which other
projects may depend. For instructions on storing your credentials so
they don't have to be re-entered every time, see `lein help
deploying`. When deploying a release that's not a snapshot, Leiningen
will attempt to sign it using [GPG](https://gnupg.org) to prove your
authorship of the release. See the
[deploy guide](https://github.com/technomancy/leiningen/blob/stable/doc/DEPLOY.md).
for details of how to set that up. The deploy guide includes
instructions for deploying to other repositories as well.

## That's It!

Now go start coding your next project!
