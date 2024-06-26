# Stegobmp
TP implementacion de Cripto

Lautaro Nicolas Gazzaneo - 61484
Tristan Flechard - 66692

Comandos a correr

1) Para compilar los archivos correr el siguiente comando:
    javac .\Encryptor.java .\BMPReader.java .\StegoImage.java .\StegoBmp.java

2) Para usar las funciones solo corran el comando 
    java StegoBmp.java -embed -in <file> -b <bitmapfile> -out <bitmapfile> -steg <LSB1 | LSB4 | LSBI> [-pass <password>] [-a <aes128 | aes192 | aes256 | des>] [-m <ecb | cfb | ofb | cbc>]

    java StegoBmp.java -extract -p <bitmapfile> -out <file> -steg <LSB1 | LSB4 | LSBI> [-pass <password>] [-a <aes128 | aes192 | aes256 | des>] [-m <ecb | cfb | ofb | cbc>]