<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Leiningen プラグイン](#leiningen-%E3%83%97%E3%83%A9%E3%82%B0%E3%82%A4%E3%83%B3)
  - [プラグインを書く](#%E3%83%97%E3%83%A9%E3%82%B0%E3%82%A4%E3%83%B3%E3%82%92%E6%9B%B8%E3%81%8F)
    - [タスクの引数](#%E3%82%BF%E3%82%B9%E3%82%AF%E3%81%AE%E5%BC%95%E6%95%B0)
    - [ドキュメンテーション](#%E3%83%89%E3%82%AD%E3%83%A5%E3%83%A1%E3%83%B3%E3%83%86%E3%83%BC%E3%82%B7%E3%83%A7%E3%83%B3)
  - [コード評価](#%E3%82%B3%E3%83%BC%E3%83%89%E8%A9%95%E4%BE%A1)
    - [プロジェクトコンテクスト内での評価](#%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88%E3%82%B3%E3%83%B3%E3%83%86%E3%82%AF%E3%82%B9%E3%83%88%E5%86%85%E3%81%A7%E3%81%AE%E8%A9%95%E4%BE%A1)
  - [他のプラグインコンテンツ](#%E4%BB%96%E3%81%AE%E3%83%97%E3%83%A9%E3%82%B0%E3%82%A4%E3%83%B3%E3%82%B3%E3%83%B3%E3%83%86%E3%83%B3%E3%83%84)
    - [プロファイル](#%E3%83%97%E3%83%AD%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB)
    - [フック](#%E3%83%95%E3%83%83%E3%82%AF)
    - [プロジェクト ミドルウェア](#%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88-%E3%83%9F%E3%83%89%E3%83%AB%E3%82%A6%E3%82%A7%E3%82%A2)
    - [Maven Wagon](#maven-wagon)
    - [VCSメソッド](#vcs%E3%83%A1%E3%82%BD%E3%83%83%E3%83%89)
  - [プラグインの呼び出し](#%E3%83%97%E3%83%A9%E3%82%B0%E3%82%A4%E3%83%B3%E3%81%AE%E5%91%BC%E3%81%B3%E5%87%BA%E3%81%97)
  - [Clojureのバージョン](#clojure%E3%81%AE%E3%83%90%E3%83%BC%E3%82%B8%E3%83%A7%E3%83%B3)
  - [既存のプラグインのアップグレード](#%E6%97%A2%E5%AD%98%E3%81%AE%E3%83%97%E3%83%A9%E3%82%B0%E3%82%A4%E3%83%B3%E3%81%AE%E3%82%A2%E3%83%83%E3%83%97%E3%82%B0%E3%83%AC%E3%83%BC%E3%83%89)
  - [プロジェクト vs スタンドアロンでの実行](#%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88-vs-%E3%82%B9%E3%82%BF%E3%83%B3%E3%83%89%E3%82%A2%E3%83%AD%E3%83%B3%E3%81%A7%E3%81%AE%E5%AE%9F%E8%A1%8C)
  - [標準タスクの上書き](#%E6%A8%99%E6%BA%96%E3%82%BF%E3%82%B9%E3%82%AF%E3%81%AE%E4%B8%8A%E6%9B%B8%E3%81%8D)
  - [1.x互換性](#1x%E4%BA%92%E6%8F%9B%E6%80%A7)
    - [特定のプロジェクト向けタスク](#%E7%89%B9%E5%AE%9A%E3%81%AE%E3%83%97%E3%83%AD%E3%82%B8%E3%82%A7%E3%82%AF%E3%83%88%E5%90%91%E3%81%91%E3%82%BF%E3%82%B9%E3%82%AF)
  - [楽しんでください](#%E6%A5%BD%E3%81%97%E3%82%93%E3%81%A7%E3%81%8F%E3%81%A0%E3%81%95%E3%81%84)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

[English](../PLUGINS.md)

# Leiningen プラグイン

Leiningenのタスクはleiningen.$TASK名前空間にある$TASKという名前の関数に過ぎません。ですから、Leiningenのプラグインを記述するのは、単にそのような関数を含むプロジェクトを作るだけのことで、下記の内容の多くは、Leiningenそのものに含まれているタスクにも同様に当てはまります。

プラグインを使用するには、プロジェクトマップの`:plugins`にそのプラグインを宣言するだけです。プロジェクトの実行用ではなく、使い勝手を向上させるプラグインは、`project.clj`ファイルに直接記述する代わりに`~/.lein/profiles.clj`の`:user`プロファイルに記述してください。

## プラグインを書く

`lein new plugin myplugin`でプロジェクトを作成するところからはじめ、`leiningen.myplugin`名前空間の`myplugin`関数を編集してください。`project.clj`内にある`:eval-in-leningen true`によって、タスクはサブプロセスではなく、leiningenプロセス内部で動作します。プラグインは、clojureそのものへの依存関係を宣言する必要はありません。
[Leiningen本体の依存関係全て](https://codeberg.org/leiningen/leiningen/src/stable/project.clj)がプラグインから利用可能です。

非常に単純なプラグインの例として、 [ソースコード内の](https://codeberg.org/leiningen/leiningen/src/stable/lein-pprint)
`lein-pprint`ディレクトリを参照してください。

プラグインの開発中、プロジェクト内で`lein install`を再実行し、それからテストプロジェクトに切り替えるのは非常に手間がかかります。一度プラグインをインストールすれば、テストプロジェクト内の`.lein-classpath`ファイルに、プラグインの`src`ディレクトリのパスを記述しておくことでこの手間を省くことができます。そのプラグインが別の開発中のライブラリに依存している場合は、`.lein-classpath`にUNIXでは`:`、Windowsでは`;`のクラスパスセパレータで区切ってライブラリのディレクトリを追加することができます。

出力をする場合は、`println`の代わりに、`leiningen.core.main/info`、`leiningen.core.main/warn`、`leiningen.core.main/debug`のいずれかを使用してください。ユーザの出力設定の制御が適用されます。

### タスクの引数

タスク関数の最初の引数は現在のプロジェクトにしてください。これは`project.clj`を元にしたマップですが、`:name`、`:group`、`:version`、`:root`などのキーが追加されています。このプロジェクトマップがどのようなものか確認するためには、`lein-pprint`プラグインを使ってみてください。プロジェクトと、プロファイルの組み合わせを確認するために`pprint`タスクを実行することができます。

コマンドラインからパラメタをとるタスクが必要な場合、ひとつ以上の引数をとる関数を作ることができます。タスクは単なるClojureの関数であることを強調するために、引数は通常、UNIXの伝統的な`--dashed`スタイルでなく、`:keywords`を受け付けるように記述します。引数は全てStringとして渡されることに留意してください。引数をキーワード、シンボル、整数として扱いたい場合には`read-string`を呼び出すことができますが、それはあなたが記述する関数次第です。他のタスクを関数として呼び出す際も、この点を留意してください。

ほとんどのタスクは、他のプロジェクトの内部でのみ実行されます。もしタスクを、プロジェクトディレクトリの外側で実行できるようにしたい場合は、`^:no-project-needed`メタデータをタスク関数に追加してください。その場合であっても、タスク関数の第一引数はプロジェクトをとるように記述し、プロジェクト外で実行した際に渡されるnilを受け入れるようにしてください。プロジェクト内で実行した場合は、LeiningenはJVMを起動する前に、プロジェクトのルートディレクトに`cd`しますが、IDE連携など、`leiningen-core`ライブラリを使用しているツールは同じようにふるまわない可能性があるため、プロジェクトの`:root`キーワードを確認し、そのディレクトリを起点とすると最大限の移植性が得られます。

### ドキュメンテーション

`lein help`タスクはdocstringを使っています。名前空間レベルのdocstringが定義されていれば、それを短いサマリとして使用します。なければ、関数のdocstringの第一文が使用されます。フォーマットの都合上、サマリは68文字以内に収めるようにしてください。関数の引数名も表示されるので、明快で説明的な引数名を選ぶようにしてください。ユーザーに見せたくない代替の引数を持つ場合、関数のメタデータに`:help-arglists`を設定することができます。その場合は全ての引数について説明するようにしてください。引数は全てStringなので、キーワード、数字、シンボルが必要な場合は`read-string`を呼び出す必要があることに留意してください。

複雑なタスクはサブタスクに分割することがよくあります。サブタスクの変数のベクターを含んだ`:subtasks`メタデータをタスク関数に渡すことによって、`lein help $TASK_CONTAINING_SUBTASKS`を実行した際に、サブタスクを表示することができます。サブタスク一覧は、それぞれのサブタスクのdocstringの第一文を表示します。サブタスクの完全版のヘルプは、`lein help $TASK_CONTAINING_SUBTASKS $SUBTASK`で表示することができます。

特別な指定がない場合、Leiningenは`lein $MYTASK help`の呼び出しを横取りして`lein help $MYTASK`に変換します。タスク内で独自のhelpサブタスクを表示したい場合はタスク関数に`^:pass-through-help`メタデータを指定してこのふるまいを無効化することができます。

## コード評価

プラグインの関数は、Leiningenのプロセス内で実行されるので、既存のLeiningenの関数全てにアクセスすることができます。`leiningen.core.*`名前空間内の、`^:internal`メタデータが付与されていない関数全てとタスク関数は全て公開されているAPIと考えてください。タスク名前空間のタスク以外の関数は内部用で、マイナーバージョンリリースでも変更される可能性があります。

### プロジェクトコンテクスト内での評価

タスクの多くはプロジェクトのコンテクスト内でコードを実行する必要があります。`leiningen.core.eval/eval-in-project`関数はこの目的で使用されます。この関数はプロジェクト引数、評価するフォーム、そして最後にオプションとして、メインフォームの前に評価される、初期化用のフォームをとることができます。この最後のフォームは[ジラルディシナリオ](https://technomancy.us/143)を防ぐために、名前空間を事前にrequireするために用いることができます。

`eval-in-project`関数内ではプロジェクトのクラスパスが有効になっており、Leiningen自体の内部関数とプラグインは無効化されています。

プロジェクトマップは`eval-in-project`に渡す前に改変することができますが、プロファイルをマージすることで変更する方法が、ユーザーによるオーバーライドを可能にするので推奨されます。変更を加えるために、`leiningen.core.project/merge-profiles`を使用してください。

```clj
(def swank-profile {:dependencies [['swank-clojure "1.4.3"]]})

(defn swank
  "Launch swank server for Emacs to connect. Optionally takes PORT and HOST."
  [project port host & opts]
    (let [profile (or (:swank (:profiles project)) swank-profile)
          project (project/merge-profiles project [profile])]
      (eval-in-project project 
                       `(swank.core/-main ~@opts)
                       '(require 'swank.core))))
```

`swank-clojure`依存関係のコードがプロジェクト内で必要なため、独自のプロファイルマップを宣言し、マージしています。しかし、`:swank`プロファイルがプロジェクトマップに定義されている場合はそれを使うため、ユーザはプラグイン内にハードコードされたバージョンに依存したくない場合は自分で違うバージョンを選択することができます。

上記で用いたコードは単に`eval-in-project`と`merge-profiles`をラップしただけなので、プラグインの例としてはふさわしくないことに留意してください。もし実現したいことがこれだけなのであれば、プラグインを実装することなく実現することができます。単に`with-profiles`と必要な関数を呼び出す`run`タスクを使ったエイリアスを定義すればよいのです。

`eval-in-project`を実行する前に、Leiningenは全てのJavaコードと実行に必要なClojureコードをバイトコードに事前にコンパイルして、プロジェクトが実行可能な状態になるよう準備しなければなりません。これはプロジェクトの`:prep-tasks`キーに定義されているタスク全てを実行することで行われます。標準では`["javac" "compile"]`です。もしあなたのプラグインが他の準備作業を必要とする場合、（例えばプロトコルバッファのコンパイル）、ユーザに別のエントリを`:prep-tasks`に追加してもらうよう指示することができます。このタスクは`eval-in-project`を実行するたびに評価されることに留意してください。前回の実行時から何も変わっていなければ素早く終了するように実装してください。

## 他のプラグインコンテンツ

プラグインは主にタスクを提供するためのものですが、他にもプロファイル、フック、ミドルウェア、wagon(依存関係トランスポートメソッド）、バージョン管理方法を含めることができます。

### プロファイル

あなたのプラグインを使用するプロジェクトの多くで必要になりそうなものの、何らかの理由でデフォルトで有効化できない設定があるとき、プラグイン内でプロファイルとして含めることができます。

`src/myplugin/profiles.clj`というファイルをプラグイン内に作成して、下記のマップを定義してください。

```clj
{:default {:x "y and z"}
 :extra {:other "settings"}}
```

マップ内のそれぞれの値がプロファイルで、ユーザーがプロジェクトにマージすることができます。`with-profile`を用いて、実行時に明示的に指定することができます。

    $ lein with-profile plugin.myplugin/extra test

ユーザは`:default`プロファイルを変更することで自動的に有効化することもできます。

```clj
:profiles {:default [:base :system :user :provided :dev :plugin.myplugin/default]
           :other {...}}
```

`:default`プロファイル内のエントリは、`jar`、`uberjar`、`pom`など、下流向けに成果物を生成するタスクと、`with-profile`を指定したタスクを除いた、他の全てのタスクで有効化されます。

### フック

フックを用いて、Leiningen標準タスクのふるまいをある程度変更することができます。フック機能は、Leiningenに含まれている
[Robert Hooke](https://github.com/technomancy/robert-hooke)ライブラリによって提供されています。

clojure.testのフィクスチャ機能に着想を得て、フックは他の関数、多くの場合はタスクをラップし、他の変数をバインディングしたり、返り値を改変したり、関数の実行を条件で制御したりすることでふるまいを変えます。`add-hook`関数は適用するタスクの変数とラッピングする関数を引数にとります。

```clj
(ns lein-integration.plugin
  (:require [robert.hooke]
            [leiningen.test]))

(defn add-test-var-println [f & args]
  `(binding [~'clojure.test/assert-expr
             (fn [msg# form#]
               (println "Asserting" form#)
               ((.getRawRoot #'clojure.test/assert-expr) msg# form#))]
     ~(apply f args)))

;; Place the body of the activate function at the top-level for
;; compatibility with Leiningen 1.x
(defn activate []
  (robert.hooke/add-hook #'leiningen.test/form-for-testing-namespaces
                         #'add-test-var-println))
```

フックは関数合成(compose)しますので、あなたのフックが他のフックの内側で実行される場合があることに気をつけてください。詳細は
[Hookeのドキュメンテーション](https://github.com/technomancy/robert-hooke/blob/master/README.md)を参照してください。`add-hook`への呼び出しは第一、第二引数の両方にVarを用いるべきであることに留意してください。そうすることでフックを重複して追加することなく繰り返しローディング可能になります。これは、Clojureでは関数は同一性が比較できませんが、Varは可能だからです。

もしあなたのフックが、あなたのプラグインを含むプロジェクトで自動的にロードされるようにしたい場合は、`plugin-name.plugin/hooks`関数で呼び出して有効化してください。上記の例ではプラグインは`lein-integration`という名前なので、`lein-integration.plugin/hooks`関数が、`lein-integration`プラグインのロード時に自動的に呼び出されます。

フックは、project.clj内の`:hooks`キーに有効化するためのVarのシークエンスを設定することで、手動でロードすることもできます。下位互換性のために、`:hooks`内でVarの代わりに名前空間も指定しすることもできます。その名前空間内の`activate`関数が呼び出されます。自動ローディングのフックは手動で指定されたフックより前に有効化されます。

### プロジェクト ミドルウェア

プロジェクトミドルウェアは、プロジェクトマップを引数にとり、新しいプロジェクトマップを返す関数にすぎません。ミドルウェアによって、プラグインはプロジェクトマップを変換することができるようになります。しかしミドルウェアは柔軟で透過的なので、デバッグを難しくするという問題点があります。もしプラグイン内のプロファイルを使って必要なことができるのであれば、そのほうがより宣言的で、ふるまいを観察しやすいので、後で起こる頭痛の種を減らすことができます。

下記のミドルウェアはプロジェクトマップの内容をプロジェクトのリソースフォルダ内に挿入してコードから読み取り可能にします。

```clj
(ns lein-inject.plugin)

(defn middleware [project]
  (update-in project [:injections] concat
             `[(spit "resources/project.clj" ~(prn-str project))]))
```

フックと同様、ミドルウェアも`plugin-name.plugin/middleware`内に定義すれば自動的に適用されます。また、project.clj内の`:middleware`キーにプロジェクトマップを変換するVarのシークエンスを定義することで手動でローディングすることもできます。ミドルウェアの自動ローディングが手動で定義されたミドルウェアよりも前に適用されることに留意してください。

また、現在有効なミドルウェアは有効になっているプロファイルに依存していることにも注意してください。つまりアクティブなプロファイルが切り替わるたびにミドルウェア関数を再適用する必要があるということです。オリジナルのプロジェクトマップを保存しておき、`merge-profiles`、`unmerge-profiles`、`set-profiles`を呼び出すたびに元の状態から変換することによって実現しています。ミドルウェア関数は繰り返し呼び出される可能性があるので、冪等性(idempotent)のない副作用を含むべきでありません。

### Maven Wagon

[Pomegranate](https://github.com/cemerick/pomegranate) (依存解決のためにLeiningenによって用いられているライブラリ)は"wagon"ファクトリの登録をサポートしています。wagonはリポジトリ用の非標準トランスポートプロトコルを扱うために用いられ、リポジトリURLのプロトコルに応じて選択されます。もし、あなたのプラグインがwagonファクトリを登録する必要があるのであれば、プロトコルと、そのプロトコル用のwagonインスタンスを返す関数のマップを含む`leiningen/wagons.clj`ファイルを含めることで実現できます。例えば、下記の`wagons.clj`は`dav:`URL用のwagonファクトリを登録します。

```clj
{"dav" #(org.apache.maven.wagon.providers.webdav.WebDavWagon.)}
```

このテクニックを用いたプラグインの例として、[S3 wagon private](https://github.com/technomancy/s3-wagon-private)や 
[lein-webdav](https://github.com/tobias/lein-webdav)を参考にしてください。


### VCSメソッド

Leiningenにはマルチメソッドを用いてリリース関連のバージョン管理タスクを行う、`vcs`タスクが含まれています。標準では、Git向けの実装が含まれていますが、`leiningen.vcs.$SYSTEM`名前空間に含めることで他のシステムのサポートを追加することができます。`vcs`タスクが呼び出される際、`leiningen.vcs`プレフィックス配下の名前空間全てがローディングされます。これらの名前空間では、`leiningen.vcs`内で定義されている`defmulti`向けのメソッドのみを特定のバージョン管理システム用に定義するようにしてください。

## プラグインの呼び出し

プラグインをプロジェクト内で用いるためには、`:dependencies`と同じフォーマットで、`:plugins`キーをproject.cljに追加するだけです。`:dependencies`で扱えるオプションに加え、`:plugins`にはフックやミドルウェアの自動ローディングを無効化するオプションが追加されています。

```clj
(defproject foo "0.1.0"
  :plugins [[lein-pprint "1.1.1"]
            [lein-foo "0.0.1" :hooks false]
            [lein-bar "0.0.1" :middleware false]])
```
## Clojureのバージョン

Leiningen2.4.0以上はClojure1.6.0を使用しています。もしLeiningenのプラグイン内で別のバージョンのClojureを使う必要がある場合は、`eval-in-project`をダミーのプロジェクト引数と共に用いることができます。

```clj
(eval-in-project {:dependencies '[[org.clojure/clojure "1.4.0"]]}
                 '(println "hello from" *clojure-version*))
```

## 既存のプラグインのアップグレード

Leiningenの以前のバージョンはプラグインの動き方に違いがいくつかありますが、アップグレードをするのはそれほど難しくないはずです。

バージョン1.xと2.xの最も大きな違いは`:dev-dependencies`がなくなったことです。Leiningenのプロセスとプロジェクトのプロセスの両方に存在する依存関係はもはや存在しません。Leiningenは`:plugins`だけを参照し、プロジェクトは`:dependencies`だけを参照します。ただしこれらのマップは現在有効化されているプロファイルによって影響を受ける場合があります。

もしあなたのプロジェクトが`eval-in-project`を使用する必要が全くないのであれば、移植は比較的容易です。移動したLeiningen関数への参照を更新するだけで十分です。`leiningen.util.*`名前空間内の関数は全てなくなり、`leiningen.core`は`leiningen.core.main`に移動しました。

`eval-in-project`を使用しているプラグインについては、プラグインの依存関係とソースコードがプロジェクト内では利用可能ではなくなるということに気をつけてください。もしあなたのプラグインが、プラグインとプロジェクトの両方のコンテクストで実行する必要のあるコードを含んでいる場合は、複数のプロジェクトに分割し、それぞれを`:plugins`と`:dependencies`に登録する必要があります。`eval-in-project`の呼び出しで`:dependencies`を挿入する方法については上記の`lein-swank`の例を参照してください。


## プロジェクト vs スタンドアロンでの実行

Leiningenタスクの中にはどのディレクトリからでも実行可能なものがあります。（例：`lein repl`)。プロジェクトコンテクスト内でのみ有効なタスクもあります。

Leiningenがプロジェクトのコンテクスト内で実行されているかどうか（つまり、現在のディレクトリに`project.clj`があるかどうか）を調べるには、プロジェクトマップの`:root`キーを調べます。

``` clojure
(if (:root project)
  (comment "Running in a project directory")
  (comment "Running standalone"))
```

もしあなたのプラグインがプロジェクトコンテクストの外側で動作するとしても、プロジェクトマップ用の引数リストの余地を残しておくべきです。プロジェクトが存在しない場合はnilが渡ってくるものと考えてください。`^:no-project-needed`メタデータを使って、プロジェクトを必要としてないことを示してください。

Leiningen 1.xでは、数値を返すタスク関数はプロセスのexit値を出力する方法として使われていましたが、2.xでは致命的なエラーが起こった際には`leiningen.core.main/abort`を呼び出すべきです。`leiningen.core.main/*exit-process?*`変数にtrueが設定されている場合、この関数の呼び出しはexitを引き起こしますが、`with-profiles`など、コンテクストによっては、単に例外をスローして次のタスクに進む場合もあります。

## 標準タスクの上書き

通常は、例えば`leiningen.compile`という名前空間を持つプラグインを作ったとしても、それは`lein compile`を実行した時に呼び出されることはありません。標準タスクがあなたのプラグインを上書きします。もし標準タスクを隠したい場合、エイリアスを作るか、プラグインを`leiningen.plugin.compile`名前空間内に作ることで実現することができます。

## 1.x互換性

ひとたび2.xとの互換性を確保するのに必要な変更点を特定すれば、同一のコードベース内でバージョン1.xと2.xの両方をサポートするかどうかを決めることができます。別のブランチで管理したほうが楽な場合もありますし、両方のバージョンをサポートしたほうが簡単な場合もあります。幸運なことに、`:plugins`を用いて、`eval-in-project`のためだけに`:dependencies`を追加する戦略はLeiningen 1.7上ではうまく動作します。

もし2.xで移動してしまった関数を使いたい場合は、コンパイル時に解決する代わりに実行時に解決し、もし見つからなければ1.xバージョンの関数に縮退する方法を試してみてください。`lein-swank`プラグインはこの互換性シムを使った例を提供しています。

```clj
(defn eval-in-project
  "Support eval-in-project in both Leiningen 1.x and 2.x."
  [project form init]
  (let [[eip two?] (or (try (require 'leiningen.core.eval)
                            [(resolve 'leiningen.core.eval/eval-in-project)
                             true]
                            (catch java.io.FileNotFoundException _))
                       (try (require 'leiningen.compile)
                            [(resolve 'leiningen.compile/eval-in-project)]
                            (catch java.io.FileNotFoundException _)))]
    (if two?
      (eip project form init)
      (eip project form nil nil init))))
```

もちろん、関数がとる引数の数が変わったり、関数が完全になくなった場合はこの手法は適用できませんが、ほとんどの場合はこれで充分なはずです。

2.xで変更された関数のうち、広く用いられていたものは、1.xと2.xの両方をサポートする互換性シムを提供する、[leinjacker](https://github.com/sattvik/leinjacker)プロジェクト経由で利用可能です。

他の重要な変更点として、`:source-path`、`:resources-path`、`:java-source-path`、`:test-path`は、`:source-paths`、`:resource-paths`、`:java-source-paths`、`:test-paths`に変更され、Stringの代わりにベクターをとるようになりました。以前の`:dev-resources`キーは、`:dev`プロファイルが有効な場合のみ`:resource-paths`のエントリの一つとして含まれるようになりました。

後方互換性を保つ方法でタスクをプロジェクトディレクトリの外側で実行させるのは、2.xが単にプロジェクトを引数として渡し、なければnilにしておくのに対し、1.xは必要以上に賢く、引数リストが実際にプロジェクトの引数として渡せるかチェックをするため、簡単ではありません。最初の引数がプロジェクトマップかどうかを判定することはできますが、引数が二つ以上の場合、このチェックは非常に難しくなります。このような状況では、二つのブランチで管理するほうがよいかもしれません。

### 特定のプロジェクト向けタスク

プロジェクトのコードベースにタスクを含めなければいけない場合もときとしてありますが、これは思ったほど頻繁におこることではありません。コマンドラインから実行する必要があるだけなら、プロジェクト内の`-main`関数として実行させ、`lein garble`のようなエイリアスとして起動するほうがはるかに簡単です。

```clj
:aliases {"garble" ["run" "-m" "myproject.garble" "supergarble"]}
```

エイリアスのベクターは部分適用されたタスク関数になりますから、上記の設定の場合、`lein garble seventeen`は、`lein run -m myproject.garble supergarble seventeen`（あるいはreplから`(myproject.garble/-main "supergarble" "seventeen")`を実行した場合）と同等であることに注意してください。エイリアスの引数は実行時に、あらかじめ渡されている引数の後ろに追加されます。

Leiningenタスクを記述する必要があるのは、例えば`eval-in-project`やLeiningenの内部へ直接アクセスするタスクを呼び出す前にプロジェクトマップを調整する必要がある場合など、プロジェクトコンテクストの外側で操作する必要がある場合のみです。エイリアスを使って、プロジェクトマップから値を読み取ることさえできます。

```clj
:aliases {"garble" ["run" "-m" "myproject.garble" :project/version]}
```

この例ではプロジェクトマップの`:version`フィールドを引数リストに渡すことで、プロジェクト内で実行される`-main`関数が値にアクセスできるようにしています。

こういった例の多くは[既存のプラグイン](https://github.com/technomnacy/leiningen/wiki/plugins)でカバーされているはずですが、もし類似の例が見つからず、何らかの理由で別のブラグインに分離できない場合、`tasks/leiningen/`の下に新しいタスクを定義した`foo.clj`ファイルを作成し、`tasks`を`.lein-classpath`に追加することでこのふるまいを実現することができます。

```
$ ls
README.md project.clj src tasks test
$ ls -R tasks
leiningen

tasks/leiningen:
foo.clj
$ echo -ne ":tasks" | cat >> .lein-classpath
$ lein foo
Hello, Foo!
```

ただし、ほとんどの場合、別のプラグインプロジェクトにタスクを分離するのが望ましいです。`.lein-classpath`は主に実験用か、適切なプラグインを作る時間がない緊急の場合のためにあるものだからです。

## 楽しんでください

あなたのプラグインができあがったら、[Wiki上のリスト](https://github.com/technomnacy/leiningen/wiki/plugins)に追加してください。

このプラグインシステムが、あなたの思いのままにLeiningenをカスタマイズするための簡単で、柔軟なシステムを提供していることを願っています。


