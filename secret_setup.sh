mkdir -p secrets

if [[ ! -e secrets/postgres_password.txt ]]; then
  openssl rand -base64 32 > secrets/postgres_password.txt
else
  echo "postgres_password exists already, you must make sure you do not need it, and delete it if you want this script to generate a new one"
fi

if [[ ! -e secrets/app_secret_key.txt ]]; then
  openssl rand -base64 48 > secrets/app_secret_key.txt
else
  echo "app_secret_key exists already, you must make sure you do not need it, and delete it if you want this script to generate a new one"
fi

if [[ ! -e secrets/adminpassword.txt ]]; then
  openssl rand -base64 32 > secrets/adminpassword.txt
else
  echo "adminpassword exists already, you must make sure you do not need it, and delete it if you want this script to generate a new one"
fi


if [[ -e .gitignore && -z $(grep 'secrets' .gitignore) ]]; then
  printf "secrets/\n.env\n*.env\n" >> .gitignore
fi
