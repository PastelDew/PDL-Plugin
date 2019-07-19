ProjectName=UserService
BFL=build_file_list.txt

find ./ -name *.java -print > $BFL

javac @$BFL -cp "lib/*" -d bin/

rm $BFL

jar cvf exports/$ProjectName.jar -C bin pdlab lib/ *.yml
