RELEASE=`date -u +%Y%m%d`
NOW=`date -u +'%d %B %Y'`

.phony: clean all debfiles deb

all: debfiles deb

debfiles:
	# Control files
	mkdir -p deb/DEBIAN
	cp -p control postinst postrm deb/DEBIAN
	# Script to run RepRap Host program
	mkdir -p deb/usr/bin
	cp -p ../reprap-host deb/usr/bin/
	# Program directory
	mkdir -p deb/usr/lib/reprap/org/reprap
	cp -p ../bin/org/reprap/Main.class deb/usr/lib/reprap/
	cp -p ../jar/Reprap.jar deb/usr/lib/reprap/reprap.jar
	cp -p ../lib/reprap-wv.stl deb/usr/lib/reprap/
	# man page
	mkdir -p deb/usr/share/man/man1
	gzip -9c ../reprap-host.1 >deb/usr/share/man/man1/reprap-host.1.gz
	# Menu generation files
	mkdir -p deb/usr/share/applications
	mkdir -p deb/usr/share/menu
	mkdir -p deb/usr/share/pixmaps
	cp -p reprap-host.desktop deb/usr/share/applications/
	cp -p reprap-host.menu deb/usr/share/menu/
	cp -p reprap.xpm deb/usr/share/pixmaps/
	# Documentation
	mkdir -p deb/usr/share/doc/reprap-host
	gzip -9c changelog >deb/usr/share/doc/reprap-host/changelog.gz
	sed -e "s/RELEASE/$(RELEASE)/g" -e "s/DATE/$(NOW)/g" ../README >deb/usr/share/doc/reprap-host/README
	cp -p copyright deb/usr/share/doc/reprap-host/

deb: debfiles
	# Now we need to be root
	sudo chown -R 0:0 deb
	sudo dpkg -b deb .

clean:
	rm -rf deb

