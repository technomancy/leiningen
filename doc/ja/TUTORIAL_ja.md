<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [チュートリアル](#%E3%83%81%E3%83%A5%E3%83%BC%E3%83%88%E3%83%AA%E3%82%A2%E3%83%AB)
  - [このチュートリアルの取り扱い範囲](#%E3%81%93%E3%81%AE%E3%83%81%E3%83%A5%E3%83%BC%E3%83%88%E3%83%AA%E3%82%A2%E3%83%AB%E3%81%AE%E5%8F%96%E3%82%8A%E6%89%B1%E3%81%84%E7%AF%84%E5%9B%B2)
  - [ヘルプを読むには](#%E3%83%98%E3%83%AB%E3%83%97%E3%82%92%E8%AA%AD%E3%82%80%E3%81%AB%E3%81%AF)
  - [Leiningen プロジェクト](#leiningen-%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88)
  - [プロジェクトの作成](#%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88%E3%81%AE%E4%BD%9C%E6%88%90)
    - [ディレクトリのレイアウト](#%E3%83%87%E3%82%A3%E3%83%AC%E3%82%AF%E3%83%88%E3%83%AA%E3%81%AE%E3%83%AC%E3%82%A4%E3%82%A2%E3%82%A6%E3%83%88)
    - [ファイル名から名前空間への対応づけの変換](#%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB%E5%90%8D%E3%81%8B%E3%82%89%E5%90%8D%E5%89%8D%E7%A9%BA%E9%96%93%E3%81%B8%E3%81%AE%E5%AF%BE%E5%BF%9C%E3%81%A5%E3%81%91%E3%81%AE%E5%A4%89%E6%8F%9B)
  - [project.clj](#projectclj)
  - [依存関係](#%E4%BE%9D%E5%AD%98%E9%96%A2%E4%BF%82)
    - [概要](#%E6%A6%82%E8%A6%81)
    - [アーティファクト ID 、アーティファクトグループ、バージョン](#%E3%82%A2%E3%83%BC%E3%83%86%E3%82%A3%E3%83%95%E3%82%A1%E3%82%AF%E3%83%88-id-%E3%82%A2%E3%83%BC%E3%83%86%E3%82%A3%E3%83%95%E3%82%A1%E3%82%AF%E3%83%88%E3%82%B0%E3%83%AB%E3%83%BC%E3%83%97%E3%83%90%E3%83%BC%E3%82%B8%E3%83%A7%E3%83%B3)
    - [スナップショットのバージョン](#%E3%82%B9%E3%83%8A%E3%83%83%E3%83%97%E3%82%B7%E3%83%A7%E3%83%83%E3%83%88%E3%81%AE%E3%83%90%E3%83%BC%E3%82%B8%E3%83%A7%E3%83%B3)
    - [レポジトリ](#%E3%83%AC%E3%83%9D%E3%82%B8%E3%83%88%E3%83%AA)
    - [依存関係のチェックアウト](#%E4%BE%9D%E5%AD%98%E9%96%A2%E4%BF%82%E3%81%AE%E3%83%81%E3%82%A7%E3%83%83%E3%82%AF%E3%82%A2%E3%82%A6%E3%83%88)
    - [検索](#%E6%A4%9C%E7%B4%A2)
    - [Maven 読み込みタイムアウト](#maven-%E8%AA%AD%E3%81%BF%E8%BE%BC%E3%81%BF%E3%82%BF%E3%82%A4%E3%83%A0%E3%82%A2%E3%82%A6%E3%83%88)
  - [JVM オプションの設定](#jvm-%E3%82%AA%E3%83%97%E3%82%B7%E3%83%A7%E3%83%B3%E3%81%AE%E8%A8%AD%E5%AE%9A)
  - [コードの実行](#%E3%82%B3%E3%83%BC%E3%83%89%E3%81%AE%E5%AE%9F%E8%A1%8C)
  - [テスト](#%E3%83%86%E3%82%B9%E3%83%88)
  - [プロファイル](#%E3%83%97%E3%83%AD%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB)
  - [Leiningen で何をするか](#leiningen-%E3%81%A7%E4%BD%95%E3%82%92%E3%81%99%E3%82%8B%E3%81%8B)
    - [Uberjar](#uberjar)
    - [フレームワークの(Uber)jars](#%E3%83%95%E3%83%AC%E3%83%BC%E3%83%A0%E3%83%AF%E3%83%BC%E3%82%AF%E3%81%AEuberjars)
    - [サーバサイドプロジェクト](#%E3%82%B5%E3%83%BC%E3%83%90%E3%82%B5%E3%82%A4%E3%83%89%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88)
    - [ライブラリの公開](#%E3%83%A9%E3%82%A4%E3%83%96%E3%83%A9%E3%83%AA%E3%81%AE%E5%85%AC%E9%96%8B)
  - [おわり!](#%E3%81%8A%E3%82%8F%E3%82%8A)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->



# チュートリアル

Leiningen は Clojure プロジェクトを、髪の毛が燃え上がるような思いをせずに、
自動化するためのものです。もしこのチュートリアルにしたがっていて、
髪の毛が燃え上がるような思いをしたり何かイライラするようなことがあった場合は、
ぜひ[私達に知らせてください](https://codeberg.org/leiningen/leiningen/issues/new)。

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
[README](https://codeberg.org/leiningen/leiningen/blob/stable/README.md)
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

### ディレクトリのレイアウト

ここで、プロジェクトの README 、コードの入った `src/` ディレクトリ、
`test/` ディレクトリ、 Leiningen のためにプロジェクトについて記述された
`project.clj` ファイルが得られます。 `src/my_stuff/core.clj` は
名前空間 `my-stuff.core` に対応します。

### ファイル名から名前空間への対応づけの変換

ここで、ただの `my-stuff` ではなく、
`my-stuff.core` を使ったことに注意してください。
これは Clojure では単一セグメントの名前空間は非推奨であるためです。
同様に名前にダッシュの入った名前空間は、
アンダースコアの入ったファイル名に対応することに注意してください。
これは JVM は名前にダッシュの入ったファイルをロードしようとすると
トラブルを引き起こすためです。
名前空間の複雑さは新しくはじめた人たちにとってよくある混乱の元です。
そういった話題についてはほとんどがこのチュートリアルの範囲外で、
[別の場所で読むことが出来ます](https://8thlight.com/blog/colin-jones/2010/12/05/clojure-libs-and-namespaces-require-use-import-and-ns.html)。

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
[sample.project.clj](https://codeberg.org/leiningen/leiningen/blob/stable/sample.project.clj)
を見てみましょう。このサンプルは追加のレポジトリとして Sonatype レポジトリを使っていますが、
これは(Clojure や Java の)ライブラリの、最新のスナップショット開発バージョンへのアクセスを提供します。
このサンプルには同様に、レポジトリに関連する更新頻度などの設定が含まれています。

### 依存関係のチェックアウト

時には複数のプロジェクト、メインのプロジェクトとそれが依存するプロジェクトを、
並行して開発する必要もあるでしょう。そんな場合、何か変更を反映しようとするたびに、
`lein install` を実行して REPL を再起動するのは、とても不便です。
Leiningen は*依存関係のチェックアウト*(もしくは単に *チェックアウト*)
という解決策を提供しています。この機能を使うためには、 `checkouts`
というディレクトリを、以下のようにプロジェクトのルートディレクトリに作成します:

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

そして、 checkouts ディレクトリの下に、必要なプロジェクトのルートディレクトリへの
シンボリックリンクを作成します。シンボリックリンクの名前は重要ではありません。
Leiningen はそれらの全てを辿って `project.clj` ファイルを探します。
習慣的には、シンボリックリンクはそれが指すディレクトリと同じ名前にします。

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

`checkouts` ディレクトリ以下に配置されたライブラリは、
レポジトリからプルするライブラリより優先されるようになりますが、
メインのプロジェクトの `:dependencies` にそのプロジェクトを
記述することの代わりにはなりません。チェックアウト機能は、
単に利便性のために、依存関係の探索場所を追加するものです。
したがって、上述のディレクトリ構造では、 `project.clj` は以下のような内容を含むべきです:

      :dependencies [[org.clojure/clojure "1.9.0"]
                     ...
                     [suchwow "0.3.9"]
                     [com.megacorp/commons "1.3.5"]
                     ...]
                 

Maven のグループ ID の `com.megacorp` はチェックアウトの挙動にはなんの影響も無いことに注意してください。
`suchwow` と `commons` の２つのリンクは、 `checkouts` のなかでは同じものとして扱われるので、
グループ ID の階層構造が、 `commons` がディスク上に実際の配置の仕方で表現される必要はありません。

`:dependencies` を更新したときには、 `lein` が、 clojars や
`~/.m2` ディレクトリといったいくつかのレポジトリに、
ライブラリを見つけることが出来る必要があります。
`lein` が "Could not find artifact suchwow:jar:0.3.9"
(アーティファクト suchwow:jar:0.3.9 が見つかりません)などと言い出したときは、
`project.clj` と `suchwow/project.clj` が異なるバージョン番号を使っている可能性があります。
別の可能性として、メインのプロジェクトと  `suchwow` で同時に作業をしていて、
両方のプロジェクトファイルでバージョン番号を上げても、
古いバージョンがローカルの Maven レポジトリに残っていることもあります。
`suchwow` ディレクトリで `lein install` を実行してください。
つまり、 `suchwow` のバージョン番号は*３つ*の場所で同じでなければなりません。
まず suchwow の `project.clj` 、メインのプロジェクトの `project.clj` 、
*そしてメインのレポジトリが使っている使っているいくつかのレポジトリ*です。

チェックアウトプロジェクトの依存関係を変えるときには、
やはり同様に `lein install` を実行し REPL を再起動する必要があります。
ソースの変更が直ちに取り込まれるでしょう。

チェックアウトはオプトインの機能です。あるプロジェクトで作業をする全ての人が
同じプロジェクト群をチェックアウトしているひつようはありません。
ですので、プロジェクトはプッシュしたりマージする前に、
チェックアウトなしでも動くようにすべきです。

チェックアウトを使っていても `base` プロファイルが上書きしないよう注意してください。
実際には、これは `lein with-profile foo run` ではなく、
`lein with-profile +foo run` を使うという事を通常は意味します。

### 検索

Leiningen はリモートの Maven の検索をサポートしています。
一致する jar を `lein search $TERM` で検索できます。
現時点では Central と Clojars のみがサポートされています。

### Maven 読み込みタイムアウト

Leiningen の使用している Maven Wagon 転送はシステムプロパティ  `maven.wagon.rto` を読み込み、
アーティファクトをレポジトリからダウンロードする際のタイムアウト値を決定します。
`lein` スクリプトはそのプロパティを 10000 に設定しています。
もしタイムアウトの長さが十分でない場合(例えば企業の遅いミラーサイトをつかている場合)、
LEIN_JVM_OPTS を通してオーバーライドすることが出来ます:

```bash
export LEIN_JVM_OPTS="-Dmaven.wagon.rto=1800000"
``` 

## JVM オプションの設定

JVM に追加の引数を渡すには、配列  `:jvm-opts`  をセットします。
これにより Leiningen が任意の JVM オプションのデフォルト値を上書きします。

```clj
 :jvm-opts ["-Xmx1g"]
```

もし　Clojure コンパイラに[コンパイラオプション](https://clojure.org/reference/compilation#_compiler_options)を渡したいのであれば、
このときに同じようにして渡すことが出来ます。

```
:jvm-opts ["-Dclojure.compiler.disable-locals-clearing=true"
           "-Dclojure.compiler.elide-meta=[:doc :file :line :added]" 
           ; notice the array is not quoted like it would be if you passed it directly on the command line.
           "-Dclojure.compiler.direct-linking=true"]
```

同様にして `JVM_OPTS` 環境変数を通じて、 Leiningen にオプションを渡すことが出来ます。
カスタムオプションつきの Leiningen JVM を起動したいのであれば、 `LEIN_JVM_OPTS` に設定してください。

## コードの実行

十分セットアップをしました。いよいよコードが実行されるのを見てみましょう。
REPL(read-eval-print loop)を起動します:

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

REPL は対話的インターフェースで、任意のコードを入力してプロジェクトの文脈で実行出来ます。
`clj-http` を `:dependencies` に追加したので、
ここでプロジェクトの `src/` ディレクトリにあるコードの
名前空間 `my-stuff.core` からロードすることが出来ます:

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

`-main` の呼び出しは println の出力("Hello, World!")と、
返り値(nil)の両方を表示しています。

ビルトインのドキュメントは `doc` を通じて利用可能です。
関数のソースコードは `source` で確認することが出来ます:

    user=> (source my-stuff.core/-main)
    (defn -main
      "I don't do a whole lot."
      [& args]
      (println "Hello, World!"))

    user=> ; use control+d to exit

すでに `-main` 関数に実行出来るだけのコードを実装しているなら、
対話的にコードを入力するひつようはなく、 `run` タスクを実行するのが簡単です:

    $ lein run
    Hello, World!

`-m` 引数を与えると、 Leiningen は `-main` 関数を他の名前空間から探します。
`project.clj` でデフォルトの `:main` を指定する事で、  `-m` を省略することができます。

`lein run` で実行するプロセスが非常に長時間に渡る場合、
高次のトランポリンタスクによりメモリを節約したいと思うかもしれません。
このタスクはプロジェクトの JVM が起動する前に、
Leiningen JVM のプロセスが終了出来るようにするものです。

    $ lein trampoline run -m my-stuff.server 5000

なにかコンパイルされるべき Java コードが、
`:java-source-paths` や `:aot` で列挙された Clojure 名前空間にあった場合、
Leiningen は `run` や `repl` などのタスクにより他のコードを実行する前に、
それらをコンパイルします。

## テスト

まだ一行もテストを書いて居ませんでした。
しかし必ず失敗するテストがプロジェクトのテンプレートに含まれています:

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

これを埋めるとテストスィートはより便利になります。
大きなテストスィートの場合には、
一度に一つか２つの名前空間を実行したいこともあるでしょう。
`lein test my-stuff.core-test` により可能です。
同様にテストセレクタを使ってテストを分割したいと思うかもしれません。
`lein help test` で詳細を確認してください。

コマンドラインから `lein test` を実行することは、
リグレッションテストに最適です。
しかし JVM の起動時間の遅さは、
よりタイトなフィードバックループを要求するテストスタイルには、
あまり合いません。
そのような場合は、 REPL を開けたままにしておき、
[clojure.test/run-tests](https://clojuredocs.org/clojure.test/run-tests)
を呼び出すか、エディタに統合された、
[clojure-test-mode](https://github.com/technomancy/clojure-mode)
を確認するなどしましょう。

走行中のプロセスをそのままにしておくことは便利ですが、
そのプロセスはディスク上のファイルを反映していない状態に
簡単になってしまいます。
関数がロードされた後にファイルから削除してもメモリに残るので、
存在しない関数の引き起こす問題を見逃しやすくなります
(よく「スライム化する」などと呼ばれます)。
このため、コミット前などに `lein test` をかならず定期的に
新しいインスタンスで実行することが推奨されます。

## プロファイル

プロファイルは異なる文脈でプロジェクトマップに
色々な要素を追加するために使われます。たとえば、
`:test` プロファイルの内容は、それがもし存在するなら、
`lein test` で実行されているあいだは、
プロジェクトマップにマージされます。
`:resource-paths` を通じてクラスパスに
設定ファイルを含むディレクトリを追加するなどの方法で、
テストが実行される間にだけ適用される設定を有効にするために
これを使う事が出来ます。 `lein help profiles` で詳細を確認してください。

特に明言しない限り、 Leiningen はデフォルトのプロファイルセットを
プロジェクトマップにマージします。
これには
`:user` プロファイルのなかのユーザ全体の設定や、
もしあるなら `project.clj` の `:dev` プロファイル、
nREPL などの開発ツールとランタイム性能を犠牲にした
起動時間の最適化などが含まれた組み込みの `:base` プロファイルを含みます。
デフォルトのプロファイルでベンチマークを実行しないでください。
(「段階的コンパイル」に関する FAQ の項目を参考にしてください)

## Leiningen で何をするか

一般的に言って、Leiningen を使ったプロジェクトでは
以下の 3 つのゴールが一般的です:

* エンドユーザに配布するアプリケーション
* サーバサイドアプリケーション
* 他の Clojure プロジェクトが利用するライブラリ

最初のケースでは、 uberjar をビルドするのが一般的でしょう。
ライブラリを開発する場合、 Clojars や
プライベートレポジトリといったレポジトリでそれを公開したいとことでしょう。
サーバサイドアプリケーションの場合は以下に述べるような
いくつかの場合があります。 `lein new app myapp` でプロジェクトを生成すると、
ライブラリの開発ではないプロジェクトに適した、
いくつかの追加のデフォルトとともにプロジェクトを開始することが出来ます。
[利用可能なテンプレートは Clojars で閲覧可能で](https://clojars.org/search?q=lein-template)、
特定の Web テクノロジーを利用するためのものや、
そのほかのタイプのプロジェクトのためのテンプレートがあります。

### Uberjar

uberjar ファイルを配布することは非常に簡単です。
これは単一のスタンドアロンで実行可能な jar ファイルで、
非技術者のユーザに配布するのに適しています。
uberjar ファイルを配布するには、
`project.clj` の `:main` に名前空間を指定し、`:aot` にも追加することで、
その名前空間が真っ先にコンパイルされるようにします。
この時点で `project.clj` ファイルは以下のようになっているはずです:

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

同様に開発時の依存関係、 `ring-devel` を追加しています。
`ring-devel` はコンパイルされた uberjars では使えません。
レポジトリにこのプロジェクトを公開する時には依存関係ではないとみなされます。

指定された名前空間は `-main` 関数を含む必要がでてきます。
`-main` 関数は、スタンドアロンの jar が実行されるときに呼ばれます。
この名前空間は、ファイルの一番上の、 `ns` フォームの中に
`(:gen-class)` 宣言が書かれていなければなりません。
`-main` 関数には、コマンドライン引数が渡されます。
`src/my_stuff/core.clj` で何か簡単な事を試してみましょう:

```clj
(ns my-stuff.core
  (:gen-class))

(defn -main [& args]
  (println "Welcome to my project! These are your args:" args))
```

これで uberjar を生成する準備ができました:

    $ lein uberjar
    Compiling my-stuff.core
    Created /home/phil/my-stuff/target/uberjar+uberjar/my-stuff-0.1.0-SNAPSHOT.jar
    Created /home/phil/my-stuff/target/uberjar/my-stuff-0.1.0-SNAPSHOT-standalone.jar

この操作は一つの jar ファイルを生成します。
生成された jar ファイルには全ての依存関係のファイルが含まれます。
ユーザは jar ファイルを単純に `java` を起動して実行できます。
いくつかのシステムではダブルクリックで jar ファイルを実行できます。

    $ java -jar my-stuff-0.1.0-SNAPSHOT-standalone.jar Hello world.
    Welcome to my project! These are your args: (Hello world.)

通常の(uber ではない) jar ファイルは、
コマンドラインツールの `java` を使って実行できます。
しかしこの方法はクラスパスの組み立てが必要であり、
エンドユーザが対象のばあい、良い解決策ではありません。

もちろん、ユーザがすでに Leiningen をインストールしている場合は、
上で述べたようにユーザに `lein run` を使うように言うのも一つでしょう。

### フレームワークの(Uber)jars

多くの Java フレームワークはデプロイは、jar ファイルか、
もしくはアプリケーションが必要とする依存関係の部分集合を含んだ派生の
圧縮ファイルの形式によるという前提に立っています。
そういったフレームワークは足りない依存関係は
実行時に提供されるものと期待しています。
そういった依存関係はコンパイルやテストの際には利用可能ですが、
`uberjar` タスクやプラグインタスクのような
安定したデプロイメントアーティファクトを作成することが目的の場合は、
デフォルトで含まれることはありません。

例えば Hadoop のジョブの jar は通常の(uber)jar ファイルで、
Hadoop ライブラリそれ自体を除く全ての依存関係を含んでいます:

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

プラグインはフレームワークの jar 派生のデプロイファイル(WAR ファイルなど)を
生成することを必要とします。これは追加のメタデータを含みます。
ただし `:provided` プロファイルがフレームワークの依存関係を取り扱うための
一般的なメカニズムを提供しています。

### サーバサイドプロジェクト

サーバサードアプリケーションとしてプロジェクトをデプロイする方法は沢山あります。
明らかな uberjar のアプローチをべつにすると、
単純なプログラムは、 [lein-tar プラグイン](https://github.com/technomancy/lein-tar)を使ってシェルスクリプトつきの tar ファイルとしてパッケージ化して、
[pallet](https://hugoduncan.github.com/pallet/) や [chef](https://chef.io/) などの仕組みをつかってデプロイします。
Web アプリケーションは、 `ring-jetty-adapter` で組み込みの Jetty を使った uberjar としてデプロイするか、
[lein-ring プラグイン](https://github.com/weavejester/lein-ring)によって作られた
.war (web アプリケーションアーカイブ)ファイルとしてデプロイされます。
uberjar 以上のことをしようとした場合、サーバサイドデプロイは多様であり、
プラグインによって取り扱うほうが、 Leiningen 組み込みのタスクよりも良いのです。

プロダクション環境で Leiningen を実行することも可能ですが、それには沢山の微妙な問題があります。
可能であれば uberjar を使うことが強く推奨されます。 `run` タスクを立ち上げる必要があるときは、
メモリを節約するために `lein trampoline run` を使うべきです。そうでなければ、
Leiningen の自分自身の JVM は実行され続け、不必要なメモリを消費します。

加えて非常に重要なこととして、デプロイする前に全ての依存関係を凍結するステップを踏まなければなりません。
そうしなければ[繰り返せないデプロイ](https://codeberg.org/leiningen/leiningen/wiki/Repeatability)
問題によって止まってしまいます。一つのデプロイ(tar ファイル, .deb ファイルなど)にプロジェクトのコードに加えて、
`~/.m2/repository` を含むことを検討すべきです。継続インテグレーションを設定する際には、
デプロイ可能なアーティファクトを作るために Leiningen を使う事が推奨されます。
たとえば [Jenkins](https://jenkins-ci.org) CI サーバを持っていて、プロジェクトの完全なテストスィートを実行しており、
テストを全て通るなら、 tar ファイルを S3 にアップロードするというような具合です。
この場合デプロイはプロダクションサーバに、便利だとよく知られた tar ファイルを取得して抽出するだけのことです。
サーバ上でチェックアウトから単純に Leiningen を起動することは、最も基本的なデプロイの役に立ちます。
しかしサーバの数が増えてくると、異種混合クラスタを動作させるリスクが出てきます。
これは各マシンが全く同じコードベースで実行されるという保証がないからです。

特に指定しない限り、デフォルトのプロファイルが含まれることに注意してください。
デフォルトのプロファイルはプロダクションに適しています。
`lein trampoline with-profile production run -m myapp.main` の使用が推奨されます。
デフォルトではプロダクションプロファイルは空です。しかし、tar ファイルを生成する CI の実行により、
デプロイに `~/.m2/repository` ディレクトリが含まれるなら、`:local-repo` という形でそのパスを追加し、
`:offline? true` を、 `:production` プロファイルに追加します。オフラインのままにしておくと、
デプロイされたプロジェクトが CI 環境でテストされたバージョンから、完全に逸脱することを防ぎます。

こういった落とし穴があるので、可能な限り urberjar を使う事が最善です。

### ライブラリの公開

もしプロジェクトがライブラリで、他の人がプロジェクトの中で
依存関係としてそのライブラリを使えるようにしたいときは、
そのライブラリをパブリックレポジトリに置く必要が出てきます。
自分自身の[プライベートレポジトリを運用する](https://codeberg.org/leiningen/leiningen/blob/stable/doc/DEPLOY.md)
こともできますし、
[Central](https://search.maven.org)に置くことも出来ますが、
最も簡単なのは[Clojars](https://clojars.org)で公開する方法でしょう。
一度[アカウントを作れば](https://clojars.org/register)、公開は容易です:

    $ lein deploy clojars
    No credentials found for clojars
    See `lein help deploying` for how to configure credentials to avoid prompts.
    Username: me
    Password:
    Created ~/src/my-stuff/target/my-stuff-0.1.0-SNAPSHOT.jar
    Wrote ~/src/my-stuff/pom.xml
    Retrieving my-stuff/my-stuff/0.1.0-SNAPSHOT/maven-metadata.xml
        from https://repo.clojars.org/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/my-stuff-0.1.0-20190525.161117-2.jar (9k)
        to https://repo.clojars.org/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/my-stuff-0.1.0-20190525.161117-2.pom (2k)
        to https://repo.clojars.org/
    Retrieving my-stuff/my-stuff/maven-metadata.xml
        from https://repo.clojars.org/
    Sending my-stuff/my-stuff/0.1.0-SNAPSHOT/maven-metadata.xml (1k)
        to https://repo.clojars.org/
    Sending my-stuff/my-stuff/maven-metadata.xml (1k)
        to https://repo.clojars.org/

一度このプロセスが成功すると、
他のプロジェクトが依存できるパッケージとして利用出来るようになります。
クレデンシャルを保存することで、毎回入力せずとも良くする方法があります。
`lein help deploying` を参考にしてください。
スナップショット版ではなくリリース版をデプロイするときは、
Leiningen は [GPG](https://gnupg.org) を使って署名を行い、
リリースの著作権を証明します。どのように設定を行うかの詳細については、
[デプロイガイド](https://codeberg.org/leiningen/leiningen/blob/stable/doc/DEPLOY.md)を参考にしてください。
デプロイガイドでは他のレポジトリへのデプロイ方法の説明もしています。

## おわり!

さあ、次のあなたのプロジェクトのコーディングをはじめましょう!
