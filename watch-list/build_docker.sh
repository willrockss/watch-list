version=$(grep 'appVersion' gradle.properties | cut -d'=' -f2)
docker build -t io.kluev/watch-list:latest -t io.kluev/watch-list:"$version" .
