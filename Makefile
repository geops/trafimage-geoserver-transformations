MVN=mvn
SHELL=bash
M2_REPO=~/.m2
LZ4_VERSION=$(shell $(MVN) -o dependency:list | grep lz4 | cut -d ":" -f 4)
GITVERSION_FILE=src/main/resources/trafimage-geoserver-transformations.gitversion

build: git-version
	$(MVN) -Dmaven.test.skip=true package
	@# dependencies which are not already bundled in geoserver
	cp $(M2_REPO)/repository/net/jpountz/lz4/lz4/$(LZ4_VERSION)/lz4-$(LZ4_VERSION).jar target/

package: build

git-version:
	./print-git-commit-hash.sh >$(GITVERSION_FILE)
	@# update the copy of the file cached in mavens build directory
	([ -d target/classes/ ] && cp $(GITVERSION_FILE) target/classes/) || true

clean:
	rm -f $(GITVERSION_FILE)
	$(MVN) clean