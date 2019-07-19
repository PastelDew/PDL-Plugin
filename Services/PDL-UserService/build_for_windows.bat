SET ProjectName=UserService
SET BFL=build_file_list.txt

dir /s /b *.java > %BFL%

javac -encoding UTF-8 @%BFL% -cp lib/* -d bin/

rm %BFL%

jar cvf exports/%ProjectName%.jar -C bin pdlab lib/ *.yml