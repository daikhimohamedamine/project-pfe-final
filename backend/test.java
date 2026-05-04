import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;  
public class test { public static void main(String[] args) { System.out.println(new BCryptPasswordEncoder().matches("admin123", "$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05")); } }  
