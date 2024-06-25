public class StegoImage {

    public static byte[] stegoLSB1(byte[] bmpData, byte[] byteMsg){
        
        BMPReader bmpReading = new BMPReader();
        int messageLength = byteMsg.length;
        int messageLengthByBits = messageLength * 8;
        int bmpOffset = bmpReading.getOffset(bmpData);
        
        if (bmpData.length - bmpOffset < messageLengthByBits) {
            System.err.println("Image to Small to Hold Message");
            return bmpData;
        }

        for (int i = 0; i < messageLength; i++) {
            byte cur = byteMsg[i];
            for (int j = 0; j < 8; j++) {
                int bmpIndex = bmpOffset + (i * 8) + j;
                bmpData[bmpIndex] = (byte)((bmpData[bmpIndex] & 0xFE) | ((cur >>> 7-j) & 1));
            }
        }

        return bmpData;
    }

    public static byte[] stegoLSB4(byte[] bmpData, byte[] byteMsg){
        
        BMPReader bmpReading = new BMPReader();
        int messageLength = byteMsg.length;
        int messageLengthByBits = messageLength * 8;
        int bmpOffset = bmpReading.getOffset(bmpData);
        
        if ((bmpData.length - bmpOffset) * 4 < messageLengthByBits) {
            System.err.println("Image to Small to Hold Message");
            return bmpData;
        }

        for (int i = 0; i < messageLength; i++) {
            byte cur = byteMsg[i];
            for (int j = 0; j < 2; j++) {
                int bmpIndex = bmpOffset + (i * 2) + j;
                bmpData[bmpIndex] = (byte)((bmpData[bmpIndex] & 0xF0) | ((cur >>> (4-(4*j))) & 0x0F));
            }
        }

        return bmpData;
    }

    public static byte[] stegoLSBI(byte[] bmpData, byte[] byteMsg){

        Integer[] LBC = new Integer[4]; // ultimo bit cambiado
        Integer[] LBNC = new Integer[4]; // ultimo bit no cambiado
        Integer[] IB = new Integer[4]; // bit inversor

        BMPReader bmpReading = new BMPReader();
        int messageLength = byteMsg.length;
        int messageLengthByBits = messageLength * 8;
        int bmpOffset = bmpReading.getOffset(bmpData) + 4; //los 4 bits de inversion
        
        if ((((bmpData.length - bmpOffset - 2)*(2/3))+ 1) < messageLengthByBits) {
            System.err.println("Image to Small to Hold Message");
            return bmpData;
        }

        int redByteCount = 2;
        int indexCount = 0;
        int bmpIndex = 0;

        int bitAntes = 0;   // Valor 0 o 1
        int bitDespues = 0; // Valor 0 o 1
        int bitsPatron = 0; // Valor 0, 1, 2 o 3

        for (int i = 0; i < messageLength; i++) {
            byte cur = byteMsg[i];
            for (int j = 0; j < 8; j++) {
                bmpIndex = bmpOffset + indexCount;
                if(redByteCount % 3 != 0){ //No es el Red
                    
                    bitAntes = bmpData[bmpIndex] & 1;
                    bitsPatron = (bmpData[bmpIndex] >>> 1) & 3;
                    bmpData[bmpIndex] = (byte)((bmpData[bmpIndex] & 0xFE) | ((cur >>> 7-j) & 1));
                    bitDespues = bmpData[bmpIndex] & 1;

                    switch (bitsPatron) {
                        case 0:
                            if (bitAntes != bitDespues) {
                                LBC[0] += 1;
                            } else{
                                LBNC[0] += 1;
                            }
                            break;
                        case 1:
                            if (bitAntes != bitDespues) {
                                LBC[1] += 1;
                            } else{
                                LBNC[1] += 1;
                            }
                            break;
                        case 2:
                            if (bitAntes != bitDespues) {
                                LBC[2] += 1;
                            } else{
                                LBNC[2] += 1;
                            }
                            break;
                        case 3:
                            if (bitAntes != bitDespues) {
                                LBC[3] += 1;
                            } else{
                                LBNC[3] += 1;
                            }
                            break;
                        default:
                            break;
                    }
                }
                else{ // Es el byte red
                    j--;
                }
                redByteCount++;
                indexCount++;
            }
        }

        for (int k = 0; k < 4; k++) {
            if ((LBC[k] - LBNC[k]) > 0 && LBC[k] > 1){
                IB[k] = 1;
            }
            else{
                IB[k] = 0;
            }
        }

        int newOffset = bmpOffset - 4;

        for (int i = 0; i < 4; i++) {
            bmpData[i + newOffset] = (byte)((bmpData[i + newOffset] & 0xFE) | (IB[i] & 1));
        }

        redByteCount = 2;

        for (int i = 0; i < messageLength; i++) {
            bmpIndex = bmpOffset + i;
            
            if(redByteCount % 3 != 0){ //No es el Red

                bitAntes = bmpData[bmpIndex] & 1;
                bitsPatron = (bmpData[bmpIndex] >>> 1) & 3;

                if (IB[bitsPatron] == 1) {
                    if(bitAntes == 1){
                        bmpData[bmpIndex] = (byte)(bmpData[bmpIndex] & 0xFE);
                    }
                    else{
                        bmpData[bmpIndex] = (byte)((bmpData[bmpIndex] & 0xFE) | 1);
                    }
                }
            }

            redByteCount++;
        }

        return bmpData;
    }

    public static byte[] steganalisisLSB1(byte[] bmpData){
        BMPReader reading = new BMPReader();
        byte[] content;
        int contentLen = 0; // si se hace negativo se puede cambiar por long.
        int imgOffset = reading.getOffset(bmpData);

        //Saco el tamaño del contenido
        for (int i = 0; i < 32; i++) {
            if (i != 0) {
                contentLen = contentLen << 1;  
            }
            contentLen = (contentLen | (bmpData[imgOffset + i] & 1)); 
        }

        imgOffset += 32;
        
        int reserva = contentLen + 20; // por extension

        content = new byte[reserva]; 
        byte aux = 0;
        int cont = 0;

        for (int i = 0; i < contentLen; i++) {
            for (int j = 0; j < 8; j++) {
                if (j != 0){
                    aux = (byte)(aux << 1);
                }
                aux = (byte)(aux | bmpData[imgOffset + cont] & 1);
                cont++;
            }
            content[i] = aux;
            aux = 0;
        }

        byte isZero = 1;
        int bytecont = 0;
        while (isZero != 0) {
            isZero = 0;
            for (int i = 0; i < 8; i++) {
                if (i != 0){
                    isZero = (byte)(isZero << 1);
                }
                isZero = (byte)(isZero | bmpData[imgOffset + cont] & 1);
                cont++;
            }
            content[contentLen + bytecont] = isZero;
        }

        int finalLen = contentLen + bytecont;
        byte[] toReturn = new byte[finalLen];
        System.arraycopy(content, 0, toReturn, 0 , finalLen);

        return toReturn;
    }


    public static byte[] steganalisisLSB4(byte[] bmpData){
        BMPReader reading = new BMPReader();
        byte[] content;
        int contentLen = 0; // si se hace negativo se puede cambiar por long.
        int imgOffset = reading.getOffset(bmpData);

        //Saco el tamaño del contenido
        for (int i = 0; i < 8; i++) {
            if (i != 0) {
                contentLen = contentLen << 4;  
            }
            contentLen = (contentLen | (bmpData[imgOffset + i] & 0x0F)); 
        }

        imgOffset += 8;
        
        int reserva = contentLen + 20; // por extension

        content = new byte[reserva]; 
        byte aux = 0;
        int cont = 0;

        for (int i = 0; i < contentLen; i++) {
            for (int j = 0; j < 2; j++) {
                if (j != 0){
                    aux = (byte)(aux << 4);
                }
                aux = (byte)(aux | bmpData[imgOffset + cont] & 0x0F);
                cont++;
            }
            content[i] = aux;
            aux = 0;
        }

        byte isZero = 1;
        int bytecont = 0;
        while (isZero != 0) {
            isZero = 0;
            for (int i = 0; i < 2; i++) {
                if (i != 0){
                    isZero = (byte)(isZero << 4);
                }
                isZero = (byte)(isZero | bmpData[imgOffset + cont] & 0x0F);
                cont++;
            }
            content[contentLen + bytecont] = isZero;
            bytecont++;
        }

        int finalLen = contentLen + bytecont;
        byte[] toReturn = new byte[finalLen];
        System.arraycopy(content, 0, toReturn, 0 , finalLen);


        return toReturn;
    }


    public static byte[] steganalisisLSBI(byte[] bmpData){
        BMPReader reading = new BMPReader();
        byte[] content;
        int contentLen = 0; // si se hace negativo se puede cambiar por long.
        int imgOffset = reading.getOffset(bmpData);
        int redByteCounter = 2;
        int[] BInv = new int[4];
        
       //Saco los 4 bits Inversores
       for (int i = 0; i < 4; i++) {
            BInv[i] = (bmpData[imgOffset + i] & 1);
            System.out.println(BInv[i]);
       }

       imgOffset += 4; //Pongo el offSet al comienzo de los datos de longitud
       
       int lastBit = 0;
       int bitsPatron = 0;
       int bitCambio = 0;

        //Saco el tamaño del contenido
        for (int i = 0; i < 48; i++) {
            if (redByteCounter % 3 != 0) {
                if (i != 0) {
                    contentLen = contentLen << 1;  
                }
                lastBit = bmpData[imgOffset + i] & 1;
                bitCambio = lastBit;
                bitsPatron = (bmpData[imgOffset + i] >>> 1) & 3;
                if (BInv[bitsPatron] == 1) {
                    if (lastBit == 0) {
                        bitCambio = 1;
                    } else{
                        bitCambio = 0;
                    }
                }
                contentLen = (contentLen | bitCambio); 
            }
            redByteCounter++;
        }

        System.out.println(contentLen); //Hasta aca bien

        imgOffset += 48; // Pongo el offset al inicio del contenido
        
        int reserva = contentLen + 20; // por extension

        content = new byte[reserva]; 
        byte aux = 0;
        int cont = 0;
        int bmpByte = 0;

        System.out.println(redByteCounter);

        for (int i = 0; i < contentLen; i++) {
            for (int j = 0; j < 12; j++) {
                if (redByteCounter % 3 != 0) {
                    if (j != 0){
                        aux = (byte)(aux << 1);
                    }
                    bmpByte = bmpData[imgOffset + cont];
                    lastBit = bmpData[imgOffset + cont] & 1;
                    bitCambio = lastBit;
                    bitsPatron = (bmpData[imgOffset + cont] >>> 1) & 3;
                    if (BInv[bitsPatron] == 1) {
                        if (lastBit == 0) {
                            bitCambio = 1;
                        } else{
                            bitCambio = 0;
                        }
                    }
                    aux = (byte)(aux | bitCambio);
                }
                cont++;
                redByteCounter++;
            }
            content[i] = aux;
            aux = (byte)(0);
        }

        System.out.println((char)content[0]);
        System.out.println(content[0]);
        System.out.println((char)content[1]);
        System.out.println((char)content[2]);
        System.out.println((char)content[3]);
        System.out.println((char)content[4]);

        byte isZero = 1;
        int bytecont = 0;
        while (isZero != 0) {
            isZero = (byte)(0);
            for (int i = 0; i < 12; i++) {
                if (redByteCounter % 3 != 0) {
                    if (i != 0){
                        isZero = (byte)(isZero << 1);
                    }
                    lastBit = bmpData[imgOffset + cont] & 1;
                    bitCambio = lastBit;
                    bitsPatron = (bmpData[imgOffset + cont] >>> 1) & 3;
                    if (BInv[bitsPatron] == 1) {
                        if (lastBit == 0) {
                            bitCambio = 1;
                        } else{
                            bitCambio = 0;
                        }
                    }
                    isZero = (byte)(isZero | bitCambio);
                }
                cont++;
                redByteCounter++;
            }
            content[contentLen + bytecont] = isZero;
            System.out.println((char)isZero);
            bytecont++;
        }

        int finalLen = contentLen + bytecont;
        byte[] toReturn = new byte[finalLen];
        System.arraycopy(content, 0, toReturn, 0 , finalLen);

        for (int i = 0; i < 5; i++) {
            System.out.println((char)toReturn[contentLen + i]);
        }



        return toReturn;
    }
}
