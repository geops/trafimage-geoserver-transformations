MVN=mvn
SHELL=bash
GITVERSION_FILE=src/main/resources/trafimage-geoserver-transformations.gitversion

build: git-version readme
	rm -f target/*.jar target/*.zip
	$(MVN) -Dmaven.test.skip=true package
	@# build a zip-file containing al jars
	cd target && zip all-jars.zip *.jar

package: build

readme:
	cp README.md src/main/resources/README.trafimage-geoserver-transformations.md

git-version:
	./print-git-commit-hash.sh >$(GITVERSION_FILE)
	@# update the copy of the file cached in mavens build directory
	([ -d target/classes/ ] && cp $(GITVERSION_FILE) target/classes/) || true

clean:
	rm -f $(GITVERSION_FILE) src/main/resources/README.trafimage-geoserver-transformations.md
	$(MVN) clean

eclipse:
	$(MVN) eclipse:eclipse
