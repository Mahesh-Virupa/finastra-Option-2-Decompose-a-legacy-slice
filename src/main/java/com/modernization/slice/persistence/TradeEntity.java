package com.modernization.slice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
public class TradeEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String tenantId;
    private String instrument;
    private String trader;
    @Column(precision = 24, scale = 2)
    private BigDecimal notional;
    private String currency;
    @Column(precision = 24, scale = 2)
    private BigDecimal fee;
    private String route;
    @Enumerated(EnumType.STRING)
    private TradeStatus status;
    private int approvalsRemaining;
    private String deskCode;
    @Column(precision = 24, scale = 2)
    private BigDecimal tax;
    private String servedBy;
    @Column(length = 1000)
    private String adaptersCsv;
    private Instant createdAt;
    private Instant postedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }
    public String getTrader() { return trader; }
    public void setTrader(String trader) { this.trader = trader; }
    public BigDecimal getNotional() { return notional; }
    public void setNotional(BigDecimal notional) { this.notional = notional; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public TradeStatus getStatus() { return status; }
    public void setStatus(TradeStatus status) { this.status = status; }
    public int getApprovalsRemaining() { return approvalsRemaining; }
    public void setApprovalsRemaining(int approvalsRemaining) { this.approvalsRemaining = approvalsRemaining; }
    public String getDeskCode() { return deskCode; }
    public void setDeskCode(String deskCode) { this.deskCode = deskCode; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public String getServedBy() { return servedBy; }
    public void setServedBy(String servedBy) { this.servedBy = servedBy; }
    public String getAdaptersCsv() { return adaptersCsv; }
    public void setAdaptersCsv(String adaptersCsv) { this.adaptersCsv = adaptersCsv; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
