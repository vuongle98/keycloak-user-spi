package org.vuong.keycloak.spi.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static boolean verifyPassword(String rawPassword, String hashed) {
        return BCrypt.checkpw(rawPassword, hashed);
    }

    public static String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }
}