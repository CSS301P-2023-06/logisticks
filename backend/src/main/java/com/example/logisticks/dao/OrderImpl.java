package com.example.logisticks.dao;

import com.example.logisticks.models.*;
import com.example.logisticks.requests.OrderRequest;
import com.example.logisticks.responses.OrderResponse;
import com.example.logisticks.responses.TrackingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import com.example.logisticks.dao.OrderDAO;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Autowired
    private orderStatusDAO osDAO;

    private boolean exists(String phoneNumber) {
        System.out.println(phoneNumber);
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

        float rate = 0;
        try {
            rate = rDAO.calculateRate(orderRequest);
        } catch (Exception e) {
            System.out.println(e);
        }

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

            String sql_rec = "insert into toBeReceivedBy(orderId, receiverPhoneNumber,receptionOTP) values (?, ?, ?)";
            int otp = 0;
            try {
                otp = (int)Math.floor((Math.random()*(9999-1000+1) + 1000));
                int finalOtp = otp;
                jdbcTemplate.update(con -> {
                    PreparedStatement stmt = con.prepareStatement(sql_rec);
                    stmt.setInt(1,order.getId());
                    stmt.setString(2, receiverPhoneNumber);

                    stmt.setInt(3, finalOtp);
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
            respone.setPrice(rate);
            respone.setStatus(true);

            try {
                List<Agent> agent_list = jdbcTemplate.query("select * from agent",  new BeanPropertyRowMapper<Agent>(Agent.class));
                int ind = (int)Math.random();
                ind = ind%(agent_list.size());

                String sql_agent = "insert into tobedeliveredby(orderId, agentPhoneNumber) values(?,?)";

                int finalOtp1 = otp;
                int finalInd = ind;
                jdbcTemplate.update(con -> {
                    PreparedStatement stmt = con.prepareStatement(sql_agent);
                    stmt.setInt(1,order.getId());
                    stmt.setString(2, (agent_list.get(finalInd)).getPhoneNumber());
                    return stmt;
                });


            } catch (Exception e) {
                System.out.println(e);
            }

            try {
                OrderStatus os = new OrderStatus(order.getId(), -1, OrderStatus.Status.PLACED);
                String sql_os = "insert into orderstatus values(?,?,?)";

                jdbcTemplate.update(con -> {
                   PreparedStatement stmt = con.prepareStatement(sql_os);
                   stmt.setInt(1,order.getId());
                   stmt.setInt(2,orderRequest.getSenderLocationId());
                   stmt.setInt(3, 0);
                   return stmt;
                });
            } catch (Exception e) {
                System.out.println(e);
                respone.setMessage("Could not add order status");
                respone.setPrice(rate);
                respone.setStatus(false);
                jdbcTemplate.update("delete from orders where id = ?", order.getId());
                jdbcTemplate.update("delete from sentBy where orderId = ?", order.getId());
                jdbcTemplate.update("delete from receivedby where orderId = ?", order.getId());
            }

            return respone;
        } catch (Exception e) {
            System.out.print(e);
            respone.setMessage("Some error occurred in placing order!");
            respone.setPrice(-1);
            respone.setStatus(false);
            return respone;
        }
    }

    @Override
    public List<OrderListTile> getSentOrders(String phoneNumber) {
        List<OrderListTile> orders = new ArrayList<OrderListTile>();
        try{
            System.out.println(phoneNumber);
            String sql = "select id, deliveryRate, weight, isFragile, isExpressDelivery, status  from sentby s inner join orders o on s.orderId = o.id inner join orderstatus t on o.id = t.orderId where s.senderPhoneNumber = ?";
            orders = jdbcTemplate.query(sql, new BeanPropertyRowMapper<OrderListTile>(OrderListTile.class), phoneNumber);
            return orders;
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return orders;
    }

    @Override
    public List<OrderListTile> getReceivedOrders(String phoneNumber) {
        List<OrderListTile> orders = new ArrayList<OrderListTile>();
        try{
            String sql = "select id, deliveryRate, weight, isFragile, isExpressDelivery, status  from tobereceivedby s inner join orders o on s.orderId = o.id inner join orderstatus t on o.id = t.orderId where s.receiverPhoneNumber = ?";
            orders = jdbcTemplate.query(sql, new BeanPropertyRowMapper<OrderListTile>(OrderListTile.class), phoneNumber);
            return orders;
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return orders;
    }

    @Override
    public TrackingResponse getTrackingDetails(int orderId) {
        TrackingResponse ret2 = new TrackingResponse();
        try{
            OrderStatus status = jdbcTemplate.queryForObject("select * from orderstatus where orderId = ?", new Object[]{orderId}, new BeanPropertyRowMapper<OrderStatus>(OrderStatus.class));
            ret2.setStatus(status.getStatus());

            String sql = "select l.id as id, city, district, state from sentby s inner join user u on s.senderPhoneNumber = u.phoneNumber inner join address a on u.addressId = a.id inner join location l on a.locationId = l.id where s.orderId = ?";
            Location senderLocation = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, new BeanPropertyRowMapper<Location>(Location.class));
            ret2.setsCity(senderLocation.getCity());
            ret2.setsDistrict(senderLocation.getDistrict());
            ret2.setcState(senderLocation.getState());

            sql = "select l.id as id, city, district, state from tobereceivedby s inner join user u on s.receiverPhoneNumber = u.phoneNumber inner join address a on u.addressId = a.id inner join location l on a.locationId = l.id where s.orderId = ?";
            Location receiverLocation = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, new BeanPropertyRowMapper<Location>(Location.class));
            ret2.setdCity(receiverLocation.getCity());
            ret2.setdDistrict(receiverLocation.getDistrict());
            ret2.setdState(receiverLocation.getState());

            sql = "select l.id as id, city, district, state from orderstatus s inner join location l on s.currentLocationId = l.id where s.orderId = ?";
            Location currentLocation = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, new BeanPropertyRowMapper<Location>(Location.class));
            ret2.setcCity(currentLocation.getCity());
            ret2.setcDistrict(currentLocation.getDistrict());
            ret2.setcState(currentLocation.getState());

            sql = "select * from tobeReceivedBy where orderId = ?";
            ToBeReceivedBy receipt = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, new BeanPropertyRowMapper<ToBeReceivedBy>(ToBeReceivedBy.class));
            ret2.setReceptionOTP(receipt.getReceptionOTP());

            sql = "select phoneNumber, name, addressId, isAdmin, passwordHash, locationId, vehicleNumber, salary from tobedeliveredby d inner join agent a on d.agentPhoneNumber  = a.phoneNumber where d.orderId = ?";
            Agent deliveryAgent = jdbcTemplate.queryForObject(sql, new Object[]{orderId}, new BeanPropertyRowMapper<Agent>(Agent.class));
            ret2.setAgentName(deliveryAgent.getName());
            ret2.setAgentPhoneNumber(deliveryAgent.getPhoneNumber());
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return ret2;
    }

}
