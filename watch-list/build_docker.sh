version=$(grep "version =" build.gradle | awk -F"'" '{print $2}')
docker build -t io.kluev/watch-list:latest -t io.kluev/watch-list:"$version" .
