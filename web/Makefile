DOCS=tutorial.html faq.html plugins.html profiles.html deploy.html

upload: html
	rsync -r ./ leiningen.org:leiningen.org/

html: $(DOCS)

# we can't set the prerequisite properly because we used uppercase
# filenames for some reason in the markdown
%.html:
	pandoc --template template.html --to html -o $@ \
		--metadata title="Leiningen $*" \
		../doc/$(shell echo $* | tr '[:lower:]' '[:upper:]').md

# TODO: css for code styles

.PHONY: upload html
