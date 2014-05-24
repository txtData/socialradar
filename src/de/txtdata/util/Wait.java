package de.txtdata.util;

import java.io.IOException;

/**
 * Helper class that does nothing except letting some time pass.
 */
public class Wait {

    public static void forKey(){
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void forSeconds(int s){
        try{
            Thread.sleep(s*1000);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
