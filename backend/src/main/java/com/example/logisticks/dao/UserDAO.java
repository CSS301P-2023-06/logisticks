package com.example.logisticks.dao;
import com.example.logisticks.models.User;
import com.example.logisticks.responses.UserDeetResponse;
import org.springframework.core.annotation.Order;
import java.util.List;

public interface UserDAO {
    int signIn(String phoneNumber, String password);
    int signUp(String phoneNumber, String password, String name, String houseNumber, String locality, int locationId);
    UserDeetResponse getUserLocation(String phoneNumber);


}
