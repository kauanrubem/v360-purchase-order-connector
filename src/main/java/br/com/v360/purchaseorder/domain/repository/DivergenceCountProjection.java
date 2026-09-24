package br.com.v360.purchaseorder.domain.repository;

import br.com.v360.purchaseorder.domain.model.DivergenceType;

public interface DivergenceCountProjection {

    DivergenceType getType();

    long getTotal();
}

