package com.example.logisticks.dao;
import com.example.logisticks.models.ToBeReceivedBy;
public interface ToBeReceivedByDAO {
    int save(ToBeReceivedBy receive);
    int update(ToBeReceivedBy receive, int id);
}
