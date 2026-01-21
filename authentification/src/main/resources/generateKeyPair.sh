openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -out rsaKey.private
openssl rsa -in rsaKey.private -out rsaKey.public -pubout -outform PEM
