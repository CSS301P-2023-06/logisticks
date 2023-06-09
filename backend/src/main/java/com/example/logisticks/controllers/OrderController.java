package com.example.logisticks.controllers;

import com.example.logisticks.dao.OrderDAO;
import com.example.logisticks.models.OrderListTile;
import com.example.logisticks.requests.OrderRequest;
import com.example.logisticks.responses.OrderResponse;
import com.example.logisticks.responses.TrackingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin
@RestController
public class OrderController {
    @Autowired
    private OrderDAO oDAO;

    @PostMapping("/order/placeorder")
    public OrderResponse saveOrder(@RequestBody OrderRequest order) {
        return oDAO.placeOrder(order);
    }

    @GetMapping("/order/sent/{phoneNumber}")
    public List<OrderListTile> getSentOrders(@PathVariable String phoneNumber){
        return oDAO.getSentOrders(phoneNumber);
    }
    @GetMapping("/order/received/{phoneNumber}")
    public List<OrderListTile> getReceivedOrders(@PathVariable String phoneNumber){
        return oDAO.getReceivedOrders(phoneNumber);
    }

    @GetMapping("/order/track/{orderId}")
    public TrackingResponse getTrackingDetails(@PathVariable String orderId){
        TrackingResponse ret = oDAO.getTrackingDetails(Integer.parseInt(orderId));
        System.out.println(ret);
        return ret;
    }
}
