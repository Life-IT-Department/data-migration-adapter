package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "cash_flow")
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CashFlowReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_flow_id")
    private Long cashFlowId;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "ac_document")
    private String ACDocument;

    @Column(name = "operation_date")
    private LocalDate OperationDate;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "station")
    private Integer station;

    @Column(name = "receipt_no")
    private Integer receiptNo;

    @Column(name = "payer_pin")
    private String payerPin;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "payer_address")
    private String payerAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "reference",columnDefinition = "TEXT")
    private String reference;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "acc_pin")
    private Integer accPin;

    @Column(name = "acc_seq")
    private Integer accSeq;

    @Column(name = "check_no")
    private Integer checkNo;

    @Column(name = "check_date")
    private LocalDate checkDate;

    @Column(name = "check_status")
    private String checkStatus;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "agency")
    private String agency;

    @Column(name = "drawn_bank")
    private String drawnBank;

    @Column(name = "clearing_bank")
    private String clearingBank;

    @Column(name = "amount_lc")
    private BigDecimal amountLC;

    @Column(name = "posted_by")
    private String postedBy;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "receipt_cancellation")
    private String receiptCancellation;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "authorizer")
    private String authorizer;

    @Column(name = "year")
    private Integer year;

    @CreatedDate
    @Column(name = "sys_date")
    private LocalDateTime sysDate;

    public <E> E mapData(Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
