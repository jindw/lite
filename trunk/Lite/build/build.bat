rem build for windows
rem compile java

set OUTPUT=..\web\WEB-INF\classes\
mkdir %OUTPUT%
javac -encoding utf8 -extdirs  ../web/WEB-INF/lib -d %OUTPUT% -sourcepath ../src/main/

xcopy /S/Y ..\src\php %OUTPUT%
copy ..\src\main\* %OUTPUT%


pause();