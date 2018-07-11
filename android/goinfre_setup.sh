#!/bin/sh

if [ ! -e ./gradlew ]; then
	exit 1
fi

echo "CHECK THE FOLLOWING:"
echo " - This is being run from the PROJECT/android directory."
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

