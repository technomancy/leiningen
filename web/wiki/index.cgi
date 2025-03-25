#!/home/leiningen/fennel
;; we don't have a great solution for the wiki right now; currently it's still
;; left on github because codeberg doesn't let you grant write access to their
;; wikis. for now we can just proxy the content from github and allow edits
;; to be done over git+SSH.

(local head
       "<!DOCTYPE html>
<head>
  <meta charset=\"utf-8\" />
  <title>Leiningen wiki: %s</title>

    <meta name=\"viewport\"
          content=\"width=device-width, initial-scale=1, maximum-scale=1\">
    <link rel=\"stylesheet\" href=\"https://leiningen.org/stylesheets/base.css\">
    <link rel=\"stylesheet\" href=\"https://leiningen.org/stylesheets/skeleton.css\">
    <link rel=\"stylesheet\" href=\"https://leiningen.org/stylesheets/layout.css\">
    <link rel=\"stylesheet\" href=\"https://leiningen.org/stylesheets/lein.css\">
    <link rel=\"shortcut icon\" href=\"https://leiningen.org/images/favicon.ico\">

</head>
<body>
<br>
  <div class=\"container\">
    <div class=\"three columns offset-by-one\">
      <img src=\"https://leiningen.org/img/leiningen.jpg\" title=\"logo\" id=\"logo\">
    </div>
    <div class=\"eight columns offset-by-two\" id=\"title\">
      <h1 style=\"margin-top: 40px\">Leiningen wiki: %s</h1>
")

(local foot
       "<hr />
<div class=\"container\">
  <p>To edit this wiki, clone the repo from
  <tt>git@github.com:technomancy/leiningen.wiki.git</tt> and push your
  changes there.</p>
</div>
</body></html>")

(fn url-for [page]
  (let [url (.. "https://github.com/technomancy/leiningen/wiki/" page)]
    ;; we REALLY don't want to let arbitrary input thru here when we shell out
    (assert (not (page:match "[ $%(%)%.;&|]"))
            "Illegal page; can't shell out to curl!")
    url))

(fn get-body [page]
  (let [curl (assert (io.popen (.. "curl --silent " (url-for page))))
        body (curl:read :*all)]
    (assert (curl:close))
    body))

(fn query-string []
  (or (: (or (os.getenv "QUERY_STRING") "") :match "page=([-a-zA-Z/]+)") ""))

(let [page (match (query-string) "" "Home" p p)
      body (get-body (query-string))
      out (if (not= 0 (length body))
              [(head:format page page)
               ;; this leaves a ton of unmatched closing divs but who cares
               (body:match "<div id=\"wiki%-body\".*</turbo%-frame>")
               foot])]
  (if out
      (do
        (print "content-encoding: utf8")
        (print "content-type: text/html")
        (print "")
        (print (table.concat out "\n")))
      (do
        (print "status: 404 not found")
        (print "")
        (print (with-open [f (io.open "404.html")] (f:read :*all))))))
