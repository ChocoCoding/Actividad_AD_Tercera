package org.example.utilities;

import java.util.Scanner;

public class Utilities {
    static Scanner sc = new Scanner(System.in);

    public static int pedirInt(String texto) {
        while(true){
            sc = new Scanner(System.in);
            try {
                System.out.println(texto);
                return sc.nextInt();
            }catch (Exception e){
                System.out.println("ERROR, el dato introducido no es correcto");
            }
        }
    }
    public static String pedirString(String texto) {
        while(true){
            sc = new Scanner(System.in);
            try {
                System.out.println(texto);
                return sc.next();
            }catch (Exception e){
                System.out.println("ERROR, el dato introducido no es correcto");
            }
        }
    }
    public static Double pedirDouble(String texto) {
        while(true){
            sc = new Scanner(System.in);
            try {
                System.out.println(texto);
                return sc.nextDouble();
            }catch (Exception e){
                System.out.println("ERROR, el dato introducido no es correcto");
            }
        }
    }
}

