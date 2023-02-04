index.cgi: index.fnl
	echo "#!/usr/bin/env lua" > $@
	fennel --compile $< >> $@
	chmod 755 index.cgi

upload: index.cgi .htaccess 404.html
	rsync -rAv $^ leiningen@wiki.leiningen.org:wiki.leiningen.org/
