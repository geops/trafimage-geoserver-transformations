MVN=mvn
M2_REPO=~/.m2
LZ4_VERSION=$(shell $(MVN) -o dependency:list | grep lz4 | cut -d ":" -f 4)

build::
	$(MVN) -Dmaven.test.skip=true package
	@# dependencies which are not already bundled in geoserver
	cp $(M2_REPO)/repository/net/jpountz/lz4/lz4/$(LZ4_VERSION)/lz4-$(LZ4_VERSION).jar target/

package: build


clean:
	$(MVN) clean
