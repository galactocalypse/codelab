package com.codelab.orders.repository;

import com.codelab.orders.entity.LineItemEntity;
import com.codelab.orders.entity.LineItemId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineItemRepository extends JpaRepository<LineItemEntity, LineItemId> {}
