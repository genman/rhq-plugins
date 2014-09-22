#!/bin/sh

# Use to package all plugins into a tar
# Run 'mvn install' first

version=`mvn -o org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.version | grep -v '\['`
target=target
package=package
dest=$target/$package

rm -rf $dest
mkdir -p $dest

for i in `find . -name rhq-*-plugin-${version}.jar`
do
    cp $i $dest
done

