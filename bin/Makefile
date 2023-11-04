SHELL := bash

BPAN := .bpan
BPAN_REPO := https://github.com/bpan-org/bpan
TEST_TAP_BASH := .test-tap-bash
TEST_TAP_BASH_REPO := https://github.com/bpan-org/test-tap-bash

TEST_DEPS := \
    $(BPAN) \
    $(TEST_TAP_BASH) \

export v

.PHONY: test
test: $(TEST_DEPS)
	prove $${v:+-v }test

clean:
	$(RM) -r $(BPAN) $(TEST_TAP_BASH)

$(BPAN):
	git clone $(BPAN_REPO) $@

$(TEST_TAP_BASH):
	git clone $(TEST_TAP_BASH_REPO) $@
