package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Entity
@Table(name = "cash_flow")
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CashFlowReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_flow_id")
    private Long cashFlowId;

    @Column(name = "fiscalYear")
    private String fiscalYear;

    @Column(name = "acDocument")
    public String ACDocument;

    @Column(name = "operationDate")
    public String OperationDate;

    @Column(name = "time")
    public String time;

    @Column(name = "station")
    public String station;

    @Column(name = "receiptNo")
    public String receiptNo;

    @Column(name = "payerPin")
    public String payerPin;

    @Column(name = "payerName")
    public String payerName;

    @Column(name = "payerAddress")
    public String payerAddress;

    @Column(name = "details")
    public String details;

    @Column(name = "payment_mode")
    public String paymentMode;

    @Column(name = "reference")
    public String reference;

    @Column(name = "description")
    public String description;

    @Column(name = "accPin")
    public String accPin;

    @Column(name = "accSeq")
    public String accSeq;

    @Column(name = "checkNo")
    public String checkNo;

    @Column(name = "checkDate")
    public String checkDate;

    @Column(name = "checkStatus")
    public String checkStatus;

    @Column(name = "paymentType")
    public String paymentType;

    @Column(name = "agency")
    public String agency;

    @Column(name = "drawnBank")
    public String drawnBank;

    @Column(name = "clearingBank")
    public String clearingBank;

    @Column(name = "amountLC")
    public String amountLC;

    @Column(name = "postedBy")
    public String postedBy;

    @Column(name = "paidAmount")
    public String paidAmount;

    @Column(name = "totalAmount")
    public String totalAmount;

    @Column(name = "receiptCancellation")
    public String receiptCancellation;

    @Column(name = "reason")
    public String reason;

    @Column(name = "authorizer")
    public String authorizer;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "sys_date")
    private Date sysDate;

    public <E> E mapData(Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
