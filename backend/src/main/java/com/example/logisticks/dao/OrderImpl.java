package com.example.logisticks.dao;

import com.example.logisticks.models.*;
import com.example.logisticks.requests.OrderRequest;
import com.example.logisticks.responses.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import com.example.logisticks.dao.OrderDAO;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class OrderImpl implements OrderDAO{

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private OrderDAO oDAO;

    @Autowired
    private UserDAO uDAO;

    @Autowired
    private RateDAO rDAO;

    private boolean exists(String phoneNumber) {
        int found = 0;
        try{
            User user = jdbcTemplate.queryForObject("select * from user where phoneNumber=?",new Object[]{phoneNumber}, new BeanPropertyRowMapper<User>(User.class));
            if(user.getPhoneNumber().equals(phoneNumber)) found++;
        }catch(Exception e){
            System.out.println("Some error occurred while performing the checks.");
            System.out.println(e);
        }
        return (found > 0 ? true : false);
    }

    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) {

        OrderResponse respone = new OrderResponse();

        try {

//            System.out.println(isFragile);

            float deliveryRate = orderRequest.getDeliveryRate();
            float weight = orderRequest.getWeight();
            int isFragile = orderRequest.getIsFragile();
            int isExpressDelivery = orderRequest.getIsExpressDelivery();
            String senderPhoneNumber = orderRequest.getSenderPhoneNumber();
            String receiverPhoneNumber = orderRequest.getReceiverPhoneNumber();
//            LocalDateTime timeOfReceipt = orderRequest.getTimeOfReceipt();
            LocalDateTime orderTime;

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
//            formatter.format(date);


            Order order = new Order(deliveryRate, weight, isFragile, isExpressDelivery);
            String sql = "insert into orders(deliveryRate, weight, isFragile, isExpressDelivery) values(?, ?, ?, ?)";
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();

            // signup the users

//            uDAO.signUp(senderPhoneNumber, "", "", "", "", -1);
//            uDAO.signUp(receiverPhoneNumber, "", "", "", "", -1);

            if (!exists(senderPhoneNumber)) {
                respone.setMessage("User is not logged in!");
                respone.setPrice(-1);
                respone.setStatus(false);
                return respone;
            }

            if (!exists(receiverPhoneNumber)) {
                uDAO.signUp(receiverPhoneNumber, "", "", "", "", orderRequest.getReceiverLocationId());
            }

            try {
                jdbcTemplate.update(conn -> {
                    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    stmt.setFloat(1, deliveryRate);
                    stmt.setFloat(2, weight);
                    stmt.setInt(3, isFragile);
                    stmt.setInt(4, isExpressDelivery);
                    return stmt;
                }, generatedKeyHolder);
                order.setId(generatedKeyHolder.getKey().intValue());
            } catch (Exception e) {
                System.out.println("Error in place order");
                System.out.println(e);
                respone.setMessage("Error in placing order!");
                respone.setPrice(-1);
                respone.setStatus(false);
                return respone;
            }


            SentBy sentby = new SentBy(senderPhoneNumber, order.getId(), formatter.format(date));
            String sql_sentBy = "insert into sentBy(senderPhoneNumber, orderId, orderTime) values (?, ?, ?)";

            try {
                jdbcTemplate.update(con -> {
                    PreparedStatement stmt = con.prepareStatement(sql_sentBy);
                    stmt.setString(1,senderPhoneNumber);
                    stmt.setInt(2, order.getId());
                    stmt.setString(3, formatter.format(date));
                    return stmt;
                });
            } catch (Exception e) {
                jdbcTemplate.update("delete from orders where id = ?", order.getId());
                System.out.println("Error in sent by");
                System.out.println(e);
                respone.setMessage("Error in setting sent by!");
                respone.setPrice(-1);
                respone.setStatus(false);
                return respone;
            }



            ToBeReceivedBy toBeReceivedBy = new ToBeReceivedBy(order.getId(), "", receiverPhoneNumber, -1);

            String sql_rec = "insert into toBeReceivedBy(orderId, receiverPhoneNumber) values (?, ?)";

            try {
                jdbcTemplate.update(con -> {
                    PreparedStatement stmt = con.prepareStatement(sql_rec);
                    stmt.setInt(1,order.getId());
                    stmt.setString(2, receiverPhoneNumber);
                    return stmt;
                });
            } catch (Exception e) {
                System.out.println("Error in received by");
                System.out.println(e);
                jdbcTemplate.update("delete from orders where id = ?", order.getId());
                jdbcTemplate.update("delete from sentBy where orderId = ?", order.getId());
                respone.setMessage("Error in received by function!");
                respone.setPrice(-1);
                respone.setStatus(false);
                return respone;
            }
            respone.setMessage("Successfully placed the order!");
            respone.setPrice(rDAO.calculateRate(orderRequest));
            respone.setStatus(true);
            return respone;
        } catch (Exception e) {
            System.out.print(e);
            respone.setMessage("Some error occurred in placing order!");
            respone.setPrice(-1);
            respone.setStatus(false);
            return respone;
        }
    }
}