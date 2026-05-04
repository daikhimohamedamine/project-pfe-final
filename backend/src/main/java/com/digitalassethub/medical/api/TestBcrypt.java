package com.digitalassethub.medical.api;  
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;  
public class TestBcrypt { public static void main(String[] args) { BCryptPasswordEncoder e = new BCryptPasswordEncoder(); System.out.println("Matches plaintext: " + e.matches("admin123", "admin123")); } }  
