package com.example.examguard.utility;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static boolean check(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}