package com.example.urlshortener.util;

import java.security.MessageDigest;

public class HashUtil {
    public static String generateShortCode(String longUrl){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(longUrl.getBytes());
            StringBuilder sb =new StringBuilder();

            for(byte b:hash){
                sb.append(String.format("%02x",b));
            }

            return sb.substring(0,6);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }

    }
}
