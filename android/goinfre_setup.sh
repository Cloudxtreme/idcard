#!/bin/sh -e

if [ ! -e ./gradlew ]; then
	echo "not in project root directory"
	exit 1
fi

echo "CHECK THE FOLLOWING:"
echo " - This is being run from the PROJECT/android directory."
echo " - This is being run from a 42 Lab computer."
echo ""
read -p "Enter to continue / ^C" -r

function mkrmln() {
	mkdir -p $1
	rm -f $2
	ln -s $1 $2
}
function mkrmrln() {
	mkdir -p $1
	rm -f $2
	ln -s $1 $2
}

mkrmln /goinfre/$(whoami) ~/goinfre

mkrmrln ~/goinfre/.gradle ~/.gradle
mkrmrln ~/goinfre/.android ~/.android
mkrmrln ~/goinfre/idcard/.gradle-project ./.gradle
mkrmrln ~/goinfre/idcard/root-build ./build
mkrmrln ~/goinfre/idcard/app-build ./app/build

